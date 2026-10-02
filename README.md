# Order + Inventory Shop

A simple shop app made with Spring Boot (backend) and React (frontend). It uses a Supabase database.

## Parts of the app

* **Order** – places and cancels orders
* **Inventory** – keeps track of stock
* **Notification** – saves what happened
* **Supplier** – orders more stock from LegacySupply
* **Channel** – sells our products on Tiangge

## What it can do

* Place orders with one or more items
* Cancel an order and get the stock back
* Show orders, stock and notifications
* Warn when stock is low and reorder it

## How to run it

**1. Database:** In Supabase SQL Editor, run `db/schema.sql`, then `db/lab4.sql`.

**2. Backend:**

```powershell
$env:SUPABASE_DB_URL='jdbc:postgresql://POOLER-HOST:5432/postgres?sslmode=require'
$env:SUPABASE_DB_USER='postgres.YOUR-PROJECT-REF'
$env:SUPABASE_DB_PASSWORD='your-database-password'
$env:LS_API_KEY='your-api-key'
cd backend
mvn spring-boot:run
```

**3. Frontend:**

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Then open `http://localhost:5173`.

**4. Tests:**

```bash
cd backend
mvn test
```

## How orders work

* If every item has enough stock, the order is **CONFIRMED**.
* If one item is short, the whole order is **REJECTED** and no stock is taken.
* Only confirmed orders can be cancelled. Cancelling gives the stock back.
* If stock goes below 5, the app reorders from the supplier.

## API

| Method | Endpoint                       | What it does       |
| ------ | ------------------------------ | ------------------ |
| POST   | `/api/orders`                  | Place an order     |
| POST   | `/api/orders/{orderId}/cancel` | Cancel an order    |
| GET    | `/api/orders`                  | List orders        |
| GET    | `/api/inventory`               | List stock         |
| GET    | `/api/notifications`           | List notifications |
| GET    | `/api/channel/status`          | Tiangge status     |

## Supplier (Lab 3)

When stock is low, the app orders more from LegacySupply. When the order arrives, the stock goes up. More details are in [INTEGRATION.md](INTEGRATION.md).

## Tiangge marketplace (Lab 4)

The app also sells on Tiangge. It works on its own:

* Every 3 seconds, it checks Tiangge for new orders.
* For each order, it replies **accepted**, **rejected** or **backordered** (waiting for a supplier delivery).
* When a buyer cancels, it gives the stock back and tells Tiangge.
* When stock changes, it sends the new stock to Tiangge.
* It saves every order ID, so the same order is never handled twice, even after a restart.

## Lab 2 reflection

**1. How do orders stay all-or-nothing?**

The whole order runs in one database transaction. The app checks every item first, and if one is short, it takes nothing. If something fails halfway, everything is undone. If Order and Inventory were separate servers, I would need to give back the stock myself when something fails.

**2. Why use events instead of calling Notification directly?**

With events, the Order code does not need to know Notification exists. I can change or remove Notification without touching Order. If Notification became its own server, I would need a message system like Kafka to send the events, and a way to avoid losing or repeating them.

**3. Which part would I move out first?**

Notification, because nothing calls it directly and it has its own table. If it stops, orders still work. Inventory would be harder because Order needs it in the middle of every order.

The Lab 4 answers are in [REFLECTION.md](REFLECTION.md).
