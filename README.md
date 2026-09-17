# Order + Inventory + Notification

A Spring Boot application with three modules:

* **Order** – handles orders
* **Inventory** – manages product stock
* **Notification** – records order and stock activities

The backend uses **Supabase PostgreSQL**, and the frontend uses **React + Vite**.

## Features

* Create multi-item orders
* All-or-nothing orders
* Cancel confirmed orders and return the stock
* View orders
* View current inventory
* View notifications
* Low-stock alerts

## Modules

```text
Order
  ↓
Inventory

Order ──→ Events ──→ Notification
Inventory ──→ Events ──→ Notification
```

The Order module communicates with Inventory using `InventoryService`. Order and Inventory send events that the Notification module listens to.

## API Endpoints

| Method | Endpoint                       | Description        |
| ------ | ------------------------------ | ------------------ |
| POST   | `/api/orders`                  | Create an order    |
| POST   | `/api/orders/{orderId}/cancel` | Cancel an order    |
| GET    | `/api/orders`                  | View orders        |
| GET    | `/api/inventory`               | View inventory     |
| GET    | `/api/notifications`           | View notifications |

## 1. Set Up Supabase

Open **Supabase → SQL Editor** and run:

```text
db/schema.sql
```

This creates the required tables:

* `inventory`
* `orders`
* `order_items`
* `notifications`

It also adds the starting stock:

* P100 = 25
* P200 = 10
* P300 = 0

To reset the data for testing, run:

```text
db/reset.sql
```

## 2. Run the Backend

Set these environment variables:

```powershell
$env:SUPABASE_DB_URL='jdbc:postgresql://POOLER-HOST:5432/postgres?sslmode=require'
$env:SUPABASE_DB_USER='postgres.YOUR-PROJECT-REF'
$env:SUPABASE_DB_PASSWORD='your-database-password'
```

Then run the backend:

```bash
cd backend
mvn spring-boot:run
```

You can also run `ShopApplication` directly from IntelliJ.

## 3. Run the Frontend

Open another terminal:

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

The frontend has four main sections:

* **On the Shelf** – shows current inventory
* **Cart** – add products and place orders
* **Orders** – view and cancel orders
* **Activity** – view notifications

## 4. How the Order Works

When placing an order, the system first checks all items.

If all items have enough stock, the order is **CONFIRMED** and the stock is reduced.

If even one item does not have enough stock, the whole order is **REJECTED** and no stock is removed.

This keeps multi-item orders **all-or-nothing**.

## 5. Cancellation

Only confirmed orders can be cancelled.

When an order is cancelled:

1. The stock is returned.
2. The order status becomes `CANCELLED`.
3. A notification is created.

Rejected orders cannot be cancelled because they did not reserve any stock.

## 6. Notifications

The Order and Inventory modules publish events.

Examples:

* `ORDER_CONFIRMED`
* `ORDER_REJECTED`
* `ORDER_CANCELLED`
* `LOW_STOCK`

The Notification module listens to these events and saves them in the database.

## 7. Low Stock

The low-stock threshold is **5**.

When a product's stock goes below 5 after a successful reservation, a low-stock notification is created.

## 8. Testing

Run the tests with:

```bash
cd backend
mvn test
```

The tests check orders, inventory, cancellation, notifications, and module dependencies.


**Reflection**

**1. How do multi-item orders stay all-or-nothing?**

Everything happens inside one database transaction. `OrderService.place` is marked with `@Transactional`, so every `reserve()` 
call and the order save are treated as one operation. First, the code checks every item to make sure there is enough stock. 
If any item is short, nothing is reserved. If a reserve operation fails halfway through, an exception is thrown and the whole 
transaction is rolled back, so any stock that was already reserved is automatically returned. If Order and Inventory were running 
on separate servers, there would be no shared transaction. In that case, I would have to undo the work myself using a saga. 
The items would be reserved one by one, and if one reservation failed, the system would call a “release” endpoint to return the stock 
that had already been reserved. This is called a compensating transaction. Retries would also need an ID to make sure the same stock is not deducted twice, 
and the system would need a way to handle situations where the Inventory service does not respond.

**2. What changes when Order publishes an event instead of calling Notification?**

When Order publishes an event instead of directly calling Notification, `OrderService` no longer needs to know that the Notification module exists. 
It simply announces an event such as “order placed” or “order rejected,” and any module that is interested can listen to it. 
This means Notification can be changed or even removed without needing to modify `OrderService`. In the current setup, 
the listeners still run on the same thread and inside the same transaction. This means that if saving a notification fails, 
the order can also fail. However, if Notification becomes its own microservice, a message broker such as RabbitMQ or Kafka would be needed to 
carry the events between services. The system would also need delivery guarantees so that no event is lost. One common solution is an outbox table, 
where the event is saved together with the order and then sent to the broker. Duplicate handling would also be necessary because a message broker may 
deliver the same message more than once.

**3. Which module would I extract first, and what would change?**

I would choose the **Notification** module to extract first because it would be the easiest to separate from the other modules. 
No other module directly calls it, it only listens to events, and it has its own table. This also means that if Notification goes down, 
the order process can still continue. Inventory would be more difficult to extract because Order calls it in the middle of a transaction, 
which would require the saga approach discussed earlier. To extract Notification, I would first move the `notification` package into a new 
Spring Boot application with its own database. Then, I would replace `@EventListener` with a message-broker listener and make the main application 
send its events to the broker instead of using Spring’s in-memory event bus. Finally, I would point the frontend’s `GET /api/notifications` endpoint 
to the new Notification service. The `OrderService` and `InventoryService` would remain mostly the same.



