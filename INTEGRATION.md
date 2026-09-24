# LegacySupply Integration

Client ID: `23-6669-443`. Base URL: `https://legacysupply.onrender.com/api/v1`.
The API key is read from the `LS_API_KEY` environment variable (or the gitignored
`application-local.properties`). It is never committed.

## 1. Product mapping

Values come from `GET /catalog` for our client ID. The mapping lives only in
`supplier/LegacySupplyTranslator.java`.

| Our product ID | Our name | SupplierSku | Supplier description | PackSize |
|---|---|---|---|---|
| P100 | Wireless Mouse | WLU-6392 | WIRELESS MOUSE 2.4GHZ | 24 |
| P200 | Mechanical Keyboard | WLU-9391 | KEYBOARD MECH TKL | 24 |
| P300 | USB-C Hub | WLU-5564 | USB HUB 4-PORT | 10 |
| P400 | USB-C Cable 1m | WLU-7311 | USB-C CABLE 1M BRAIDED | 10 |
| P500 | Headset with Mic | WLU-8644 | HEADSET W/ MIC | 6 |

P400 and P500 were added to our inventory for this lab.

## 2. Sessions

1. `POST /auth/token` with `<AuthRequest><ClientId/><ApiKey/></AuthRequest>` returns
   `<AuthResponse><SessionToken/><IssuedAt/></AuthResponse>`.
2. Every other call sends the token in the `X-LS-Session` header.

**Measured lifetime: 180 seconds (3 minutes) from sign-in.**

| Test | Result |
|---|---|
| Token issued 17:01:52Z, used every 20 s | accepted at +170 s, refused at +191 s |
| Token issued 17:06:01Z, idle, then used every 3–5 s | accepted at +178 s, refused at +182 s (`E-AUTH-07`) |

Using the token does not extend it; the lifetime counts from sign-in.

How our adapter (`LegacySupplyClient`) handles it:

- It signs in when it has no token.
- It signs in again after 150 s, before the 180 s limit.
- If LegacySupply still answers `E-AUTH-02`, `E-AUTH-03` or `E-AUTH-07`, the adapter drops
  the token, signs in again and retries the call.

## 3. Error codes we received

| Code | HTTP | What caused it |
|---|---|---|
| E-AUTH-01 | 401 | Sign-in with a wrong API key. |
| E-AUTH-02 | 401 | Request without an `X-LS-Session` header. |
| E-AUTH-03 | 401 | Made-up session token (`abc123`). |
| E-AUTH-07 | 401 | Real token used after its 180 s lifetime. |
| E-FMT-01 | 415 | Order sent as `application/json`. |
| E-FMT-02 | 400 | Truncated XML body. |
| E-SKU-02 | 422 | SKU from the manual's example (`ABC-1234`) instead of one from our catalog. |
| E-QTY-11 | 422 | `Qty` of 0, and `Qty` of 100 (allowed range is 1–99). |
| E-REF-05 | 400 | Order without a `BuyerRef`. |
| E-PO-04 | 404 | `GET /purchase-orders/PO-000000` (order does not exist). |
| E-QRY-06 | 400 | `GET /purchase-orders` without `?buyerRef=`. |

Not seen yet during probing: `E-IDEM-04`, `E-RATE-03`, `E-SYS-50`, `E-SYS-99`. How the adapter
treats them is described in section 6.

## 4. Qty and Uom

`Qty` is how many **cases** we order, not how many single items. `Uom` (unit of measure)
is `CS`, which means case. One case holds `PackSize` units of that item.
`Qty` must be a whole number from 1 to 99.

Our Inventory only thinks in units. The translator converts units to cases, **rounding
up**, so we never receive less than we need:

```
cases = ceil(unitsNeeded / PackSize)   (at least 1, at most 99)
unitsReceived = cases × PackSize
```

**Worked example (P100 Wireless Mouse, PackSize 24):**
the reorder target is 20 units. An order drops stock to 4, below the threshold of 5.

- Units needed: 20 − 4 = 16
- Qty sent: ceil(16 / 24) = **1** case (`Uom` `CS`)
- Units received on delivery: 1 × 24 = **24**, so stock goes from 4 to 28

Another example: P500 Headset, PackSize 6. Needing 16 units gives ceil(16 / 6) = 3 cases,
which is 18 units.

## 5. Order status mapping

LegacySupply status codes never leave the supplier module. `supplier_orders.status` uses
our own enum, `SupplierOrderStatus`.

| LegacySupply `StatusCode` | Our status | What we do |
|---|---|---|
| (not sent yet) | PENDING | Retry every 30 s until LegacySupply accepts it. |
| 10 Accepted | PLACED | Check every 60 s. |
| 20 Picking | PICKING | Check every 60 s. |
| 30 Shipped | SHIPPED | Check every 60 s. |
| 40 Delivered | DELIVERED | Publish `SupplierOrderDelivered`; Inventory restocks the units. Stop checking. |
| order refused (400/409/415/422) | FAILED | Stop. Notification "reorder stopped". |
| **any other code** | NEEDS_REVIEW | See below. |

**Unexpected status codes** (for example a cancelled order, which the manual does not list):
we do not guess what the code means. The order is set to `NEEDS_REVIEW`:

- no stock is added, because nothing tells us goods are coming;
- we stop checking it, which saves request quota;
- a `REORDER_STOPPED` notification appears in the activity feed, and the raw code is written
  to the application log;
- the product is free to be reordered again the next time it drops below the threshold,
  because `NEEDS_REVIEW` is not an "open" status.

## 6. Resilience

| Requirement | How |
|---|---|
| Slow calls | Every HTTP call has a 3 s timeout (`LegacySupplyClient.TIMEOUT`). |
| Retries | At most 3 attempts per call, backing off 0.5 s and then 1 s. Retried: timeouts, connection errors, 5xx (`E-SYS-50`, `E-SYS-99`) and session errors. |
| Not retried | 4xx order errors (the order is `FAILED`). `E-RATE-03` (429): we stop for this round and wait for the next scheduled run, so we don't use up more quota. |
| No duplicates | Each reorder gets a UUID `request_id` when its row is created. The same value is sent as `X-Request-Id` on every attempt, including after a restart. A reorder that has waited more than 1 minute is first looked up with `GET /purchase-orders?buyerRef=` before it is sent again. |
| Unique BuyerRef | `RO-` + the `supplier_orders` id, e.g. `RO-7`. |
| Never lose a reorder | The row is saved as `PENDING` in the same database transaction as the customer order. It is sent only after that transaction commits. If LegacySupply is down it stays `PENDING`, and `SupplierJobs.sendPendingReorders` (`@Scheduled`, every 30 s) sends it later. |
| One reorder per product | If a product already has an open reorder (PENDING/PLACED/PICKING/SHIPPED), no new one is created. |
| Quota | One session per 150 s. Pending sends every 30 s, status checks every 60 s. Each run stops at the first failure instead of trying every order. |

### Observed on 2026-09-29

Reorder `RO-4` (P200, 24 units) was sent while LegacySupply was slow:

1. Attempt 1: LegacySupply created `PO-100330`, but its reply took longer than 3 s. Our
   adapter timed out (the self-check log shows `PO_CREATED, CLIENT_GONE`).
2. Attempt 2, 0.5 s later, with the **same** `X-Request-Id`: LegacySupply answered 200
   `IDEMPOTENT_REPLAY` with the existing `PO-100330`.

Result on the self-check page: 0 duplicates, 1 chaos event, 1 safe replay.

## 7. Where the LegacySupply details live

Everything about LegacySupply is inside `edu.cit.menardo.supplier`, and only these types are
public: `SupplierGateway`, `ReorderResult`, `SupplierOrderStatus` and the `supplier.events`
records. XML handling, the HTTP client, sessions, SKUs, pack sizes and status codes are
package-private. `ModuleDependencyTest` and `SupplierModuleBoundaryTest` check this.
