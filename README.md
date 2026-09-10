# Order + Inventory

Khylla Laine C. Menardo — CIT-U

An order desk for a small shop: pick a product, enter a quantity, and the order
comes back CONFIRMED or REJECTED against live stock.

One Spring Boot app with two modules that call each other in-process, a shared
Supabase Postgres database, and a React (Vite) frontend.

```
edu.cit.menardo            ShopApplication
edu.cit.menardo.shop       Order module
edu.cit.menardo.inventory  Inventory module
```

## Supabase setup

**1. Create the project.** Sign in at supabase.com and click New project. Pick
the Southeast Asia (Singapore) region and save the database password — Supabase
only shows it once.

**2. Create the tables.** Open SQL Editor → New query, paste all of
`db/schema.sql`, and run it. It creates `inventory` and `orders` and seeds
P100 Wireless Mouse (25), P200 Mechanical Keyboard (10), P300 USB-C Hub (0).

Check Table Editor afterwards: three rows in `inventory`, `orders` empty.

**3. Get the connection details.** Click **Connect** at the top of the
dashboard, choose **Direct** → **Session pooler**, then the **JDBC** tab.

Use the session pooler on port 5432, not the transaction pooler on 6543 —
Hibernate relies on server-side prepared statements, which transaction mode
does not support. The direct `db.<ref>.supabase.co` host is IPv6-only on most
networks and will time out.

**4. Set the environment variables.** In IntelliJ: Run → Edit Configurations →
Environment variables.

```
SUPABASE_DB_URL       jdbc:postgresql://POOLER-HOST:5432/postgres?sslmode=require
SUPABASE_DB_USER      postgres.YOUR-PROJECT-REF
SUPABASE_DB_PASSWORD  your-database-password
```

The username must carry the project ref. Plain `postgres` gives
`FATAL: Tenant or user not found`.

Nothing secret is written to a file in the repo — `application.properties` only
references the three variable names.

## Running it

Run `ShopApplication` (port 8080), then:

```
cd frontend
copy .env.example .env
npm install
npm run dev
```

Opens on http://localhost:5173, the origin allowed in
`app.cors.allowed-origins`.

## Network tab evidence

DevTools → Network, Preserve log ticked, filtered on `orders`.

**CORS preflight.** Every order sends an OPTIONS request first. Its response
carries `Access-Control-Allow-Origin: http://localhost:5173`,
`Access-Control-Allow-Methods: GET,POST,OPTIONS` and
`Access-Control-Allow-Headers: content-type` — the rules registered in
`CorsConfig`.

**Confirmed order.** Mechanical Keyboard, quantity 2.

```
POST http://localhost:8080/api/orders          200 OK

Payload   {"productId":"P200","quantity":2}

Response  {"status":"CONFIRMED",
           "reason":null,
           "inventory":{"productId":"P200","name":"Mechanical Keyboard","stock":8}}


```

P200 drops from 10 to 8 in the stock list, and a CONFIRMED row appears in the
`orders` table with a generated UUID and timestamp.

**Rejected order.** USB-C Hub, quantity 1.

```
POST http://localhost:8080/api/orders          200 OK

Payload   {"productId":"P300","quantity":1}

Response  {"status":"REJECTED",
           "reason":"Only 0 of USB-C Hub left, 1 requested.",
           "inventory":{"productId":"P300","name":"USB-C Hub","stock":0}}


```

Stock is unchanged and a REJECTED row is written with the reason filled in, so
the `orders` table holds both attempts.

Both return HTTP 200 on purpose: the request was well formed and the server
answered it correctly. The answer was simply no. A 4xx would claim the client
made a mistake, which it did not.

`db/reset.sql` clears orders and restores stock to 25/10/0 between runs.

## Tests

Maven panel → Lifecycle → test. Five tests, no database needed.

- `ModuleBoundaryTest` proves by reflection that `InventoryServiceImpl`, the
  entity and the repository are all package-private
- `OrderServiceTest` covers both paths with a stub `InventoryService`

## Reflection

**1. In-process versus separate services.** Right now `OrderService` just calls
`inventoryService.reserve(...)`. It is a normal method call inside the same
program, so a lot of hard problems simply do not exist. The call cannot time
out. It cannot arrive twice. The compiler checks the types, so if I change the
method signature the build breaks immediately instead of failing later at
runtime. Best of all, both modules share the same database transaction, so
`@Transactional` on `place` means the stock deduction and the order row are
saved together. If saving the order fails, the stock goes back automatically. I
did not write any code to make that happen.

If I split the two modules onto separate servers, I would have to add all of
that back myself: an HTTP client and matching request/response classes on both
sides, timeouts and retries, some way to make sure a retry does not deduct
stock twice, and a "give the stock back" call for when the order fails to save
— because there is no shared transaction to undo it. I would also have to tell
apart two things that are currently the same case: inventory saying no, and
inventory not replying at all.

**2. Why package-private matters.** `InventoryServiceImpl` is declared without
`public`, so code in the `shop` package cannot even mention the class name. The
boundary is enforced by the compiler, not by everyone remembering to respect
it. If it were public, `OrderService` could inject the implementation directly
and the interface would stop being the real contract.

The same goes for `InventoryItem` and `InventoryRepository`. If those were
public, the Order module could load an inventory row and change `stock` itself,
skipping the safe `update ... where stock >= :quantity` in `reserve`. That one
statement is what stops two orders arriving at once from pushing stock below
zero, and it only works because it is the single place that writes to the
inventory table.

**3. When to split Inventory out.** I would do it when the two parts really
need different things — for example if inventory gets far more traffic than
orders, if another team owns it, or if some other app needs to check stock too.

The interface makes that change cheap. `InventoryService` stays exactly the
same, and I would swap `InventoryServiceImpl` for a version that sends HTTP
requests instead of using the repository. `OrderService` would not change at
all. What would change is everything the shared transaction was quietly
handling: `place` would need to undo the reservation itself if saving the order
fails, and `ReservationResult` would need a third outcome meaning "inventory
did not respond", so a network problem is not shown to the user as if the item
were out of stock.

## Notes

- Built against Java 19. Raise `<java.version>` in `backend/pom.xml` if your JDK
  is newer.
- `.env`, `node_modules/` and `target/` are gitignored.
