# Reflection – Lab 3

The questions are copied from the LegacySupply self-check page for client `23-6669-443`.

## 1. How long does a session last, and how does the adapter decide when to sign in again?

A session lasts **180 seconds (3 minutes)** from the moment it is issued. I measured it twice:

- A token issued at 17:01:52Z and used every 20 seconds was accepted at +170 s and refused at +191 s.
- A token issued at 17:06:01Z was accepted at +178 s and refused at +182 s with `E-AUTH-07 Session not valid`.

Using the token does not make it last longer; the 180 seconds always count from sign-in.

`LegacySupplyClient` decides when to sign in again in two ways:

1. **Before it expires:** it remembers when it signed in. If the session is older than 150 s
   (`legacysupply.session-renew-after-seconds`), it signs in again before the next call. That
   leaves 30 s of margin.
2. **When it is refused anyway:** if LegacySupply answers `E-AUTH-02`, `E-AUTH-03` or `E-AUTH-07`,
   the client throws the token away, signs in again and retries the same call. That retry counts
   as one of the 3 attempts.

Nobody has to paste a token by hand.

## 2. From "units needed" to Qty sent to units received

Order `RO-3`, product P100 Wireless Mouse. In the LegacySupply catalog it is `WLU-6392`, `PackSize` 24.

| Step | Value |
|---|---|
| Stock after the customer order | 4 (below the threshold of 5) |
| Reorder target | 20 |
| Units our Inventory needed | 20 − 4 = **16** |
| Qty sent to LegacySupply | ceil(16 / 24) = **1**, `Uom` `CS` (1 case) |
| Units received on delivery | 1 × 24 = **24** |

We round **up** so we never receive less than we need. `Qty` must be a whole number of cases,
so ordering "16 units" is impossible. The conversion happens only in
`LegacySupplyTranslator.casesFor` and `unitsIn`. Inventory only ever sees units: it asked for 16
and, on delivery, the `SupplierOrderDelivered` event tells it to add 24.

A second example is `RO-2` (P500 Headset, `PackSize` 6). We needed 16 units, sent `Qty` 3 and
received 18 units: stock went from 4 to 22.

## 3. If LegacySupply is replaced by a JSON supplier with different status codes, what changes?

Classes that would change, all in `edu.cit.menardo.supplier`:

| Class | Why it changes |
|---|---|
| `LegacySupplyXml` | Builds and reads XML. It would be replaced by JSON handling. |
| `LegacySupplyClient` | URLs, headers (`X-LS-Session`), sign-in flow and error parsing are LegacySupply's. |
| `LegacySupplyException` | Knows LegacySupply's error codes (`E-AUTH-07`, …). |
| `LegacyOrderAck` | Shape of LegacySupply's order reply (`PoNumber`, `StatusCode`, `Uom`). |
| `LegacySupplyTranslator` | SKUs, pack sizes and the status-code → `SupplierOrderStatus` mapping. |
| `application.properties` | `legacysupply.*` settings. |

Possibly also `ReorderSender` and `DeliveryTracker`, if the new supplier has no idempotency key
or no "look up by reference" call. The retry/PENDING logic itself would stay.

**Order and Inventory are not on the list.** They never see anything that belongs to LegacySupply:

- Inventory calls `SupplierGateway.requestReorder(productId, unitsNeeded)`, which uses our
  product ID and units.
- Inventory restocks when it receives `SupplierOrderDelivered(reference, productId, units)`,
  again in our own terms.
- Order does not talk to the supplier at all.
- `SupplierOrderStatus` is our own enum, so a new status code only changes the translator's mapping.
- `ModuleDependencyTest.orderAndInventoryKnowNothingAboutLegacySupply` fails the build if `shop`
  or `inventory` ever mention SKUs, pack sizes, `Uom`, status codes or LegacySupply classes.
