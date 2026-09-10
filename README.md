# Order + Inventory (Spring Boot, React, Supabase)

One Spring Boot application containing two modules that talk to each other
in-process, sharing a Supabase Postgres database, with a Vite React client
calling it over HTTP.

```
edu.cit.menardo            ShopApplication  (scans both modules)
edu.cit.menardo.shop       Order module     -> OrderService, OrderController
edu.cit.menardo.inventory  Inventory module -> InventoryService (interface only, publicly)
```

## The three integrations

| Boundary | How it is crossed | Where to look |
|---|---|---|
| Order to Inventory | Constructor-injected `InventoryService`, plain method call, one transaction | `shop/OrderService.java` |
| Service to database | Spring Data JPA over the Supabase pooler | `inventory/InventoryRepository.java`, `db/schema.sql` |
| Browser to service | `POST /api/orders` with CORS for `localhost:5173` | `shop/OrderController.java`, `config/CorsConfig.java` |

## 1. Supabase

Create a free project, then open **SQL Editor** and run `db/schema.sql`. It
creates both tables and seeds P100 (25), P200 (10) and P300 (0).

Then click **Connect** at the top of the dashboard, choose **Session pooler**,
and open the **JDBC** tab. Use the session pooler on port **5432**, not the
transaction pooler on 6543 — Hibernate relies on server-side prepared
statements, which transaction mode does not support. The direct
`db.<ref>.supabase.co` host is IPv6-only on most networks, which is the usual
cause of a connection timeout.

The username for pooler connections is `postgres.<your-project-ref>`, not plain
`postgres`.

## 2. Backend

Credentials come from three environment variables and are never written to a
file in the repo. `backend/.env.example` shows the shape.

macOS / Linux:

```bash
export SUPABASE_DB_URL='jdbc:postgresql://POOLER-HOST:5432/postgres?sslmode=require'
export SUPABASE_DB_USER='postgres.YOUR-PROJECT-REF'
export SUPABASE_DB_PASSWORD='your-database-password'
```

Windows PowerShell:

```powershell
$env:SUPABASE_DB_URL='jdbc:postgresql://POOLER-HOST:5432/postgres?sslmode=require'
$env:SUPABASE_DB_USER='postgres.YOUR-PROJECT-REF'
$env:SUPABASE_DB_PASSWORD='your-database-password'
```

In IntelliJ, set the same three in Run configuration → Environment variables
instead, so you do not have to launch from a prepared shell.

There is no Maven wrapper in the zip. If you would rather have `./mvnw`, copy
the `.mvn/` folder and `mvnw` / `mvnw.cmd` from any Spring Initializr download,
or run `mvn wrapper:wrapper` once.

```bash
cd backend
mvn spring-boot:run
```

`ddl-auto=validate` means startup fails immediately if the tables do not match
the entities, which is the behaviour you want when the schema is owned by SQL.

## 3. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Opens on http://localhost:5173, which is the origin allowed in
`application.properties` under `app.cors.allowed-origins`.

## 4. Capturing the evidence

Open DevTools → Network, tick **Preserve log**, and filter on `orders`.

**Confirmed path.** Pick Mechanical Keyboard, quantity 2, place the order. The
request payload is `{"productId":"P200","quantity":2}` and the response is
`{"status":"CONFIRMED","reason":null,"inventory":{"productId":"P200","name":"Mechanical Keyboard","stock":8}}`.
Screenshot the Headers, Payload and Response tabs. Then run
`select * from orders order by created_at desc limit 5;` in Supabase to show the
row landed.

**Rejected path.** Pick USB-C Hub (stock 0), quantity 1. Same 200 status, but
the body is `{"status":"REJECTED","reason":"Only 0 of USB-C Hub left, 1 requested.", ...}`.
Rejections are recorded in `orders` too, so both screenshots have a matching row.

A rejection returns HTTP 200 on purpose — the request was well formed and the
server answered it. If your rubric wants a non-2xx for rejections, change
`OrderController.placeOrder` to return
`ResponseEntity.status(HttpStatus.CONFLICT).body(response)` when the status is
`REJECTED`.

**CORS.** To show it is doing something, temporarily change
`app.cors.allowed-origins` to `http://localhost:5174`, restart, and screenshot
the blocked request in the Console.

## 5. Tests

```bash
cd backend
mvn test
```

- `ModuleBoundaryTest` uses reflection to assert `InventoryServiceImpl`, the
  entity and the repository are all package-private — direct proof of the
  boundary requirement.
- `OrderServiceTest` covers the confirmed and rejected paths with a stub
  `InventoryService` and no database, which is only possible because Order
  depends on the interface.

## Notes on the design

- `reserve` decrements with `update ... where stock >= :quantity`, so the check
  and the write are one statement. Two simultaneous orders cannot both pass a
  read-then-write check and drive stock negative.
- `OrderService.place` is `@Transactional`. The stock deduction and the order
  row commit together, so a failed insert rolls the reservation back.
- Inventory exposes `InventoryView` and `ReservationResult` records, never the
  `InventoryItem` entity. Order cannot hold a managed entity and cannot write to
  the inventory table.
- "Not enough stock" is a return value, not an exception, because it is an
  expected business outcome rather than a fault.

## Before pushing

`.gitignore` covers `.env` files and `target/`. Confirm with
`git status --ignored` and `git log -p | grep -i supabase` that no connection
string was ever committed. If one was, rotate the database password in Supabase
— removing the line in a later commit does not remove it from history.
"# order-inventory" 
