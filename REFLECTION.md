
# Reflection – Lab 4 (Marketplace)


## 1. Tiangge order TG-GLNKDK (8 x P300) was accepted at 22:39:17. At that moment your last published stock for P300 was 7, and the stock Tiangge worked out from your own decisions, cancellations and deliveries was 7. Where did your application's stock figure come from, and why did it disagree?

My app reads its stock from the `inventory` table in my database. A delivery at 22:39:12 brought P300 up to 20, and two other orders used 5, so 15 were left. That was enough for 8, so my app filled TG-GLNKDK and the stock dropped to 7. But my app sent "P300 is now 7" to Tiangge at 22:39:17.012, before it sent "TG-GLNKDK is accepted" at 22:39:17.543. So Tiangge saw 7 in stock and then an order of 8, and counted it as an oversell. My count was right; the messages were just sent in the wrong order.

## 2. Event evt_a361f217b67587d3 (order TG-KP9693) reached your application twice, as seq 1 and seq 10, and you processed it once. Show the code and the stored data that made the second delivery harmless, and explain what would happen if your application restarted between the two.

My app saves the ID of every event it handles, and skips any ID it has already seen:

```java
if (processedEvents.existsById(event.eventId())) {
    log.info("Skipping redelivered event {} ...");
} else {
    ...
    processedEvents.save(new ProcessedEvent(event.eventId(), event.seq(), event.type(), event.orderId()));
}
moveCursor(event.seq());
```

The first copy (seq 1) created the order at 21:45:43 and saved this row in `channel_events`: `evt_a361f217b67587d3 | seq 1 | ORDER_PLACED | TG-KP9693`. When the second copy (seq 10) came at 21:50:24, the app found the ID and logged `Skipping redelivered event evt_a361f217b67587d3 (seq 10, ...)`, so only one order was made. A restart in between would not change this, because the ID is saved in the database, not in memory. After restarting, the app still finds the ID and skips seq 10.

## 3. Order TG-JTSKQ4 was backordered at 21:54:11 and accepted at 21:56:58, after PO-101963 was delivered at 21:56:25. Trace how the delivery reached your Inventory and what then resumed the backordered order.

TG-JTSKQ4 wanted 5 P100, but we did not have enough, so it waited as a backorder because supplier order RO-5 (PO-101963) was coming. The delivery came while my app was off for the restart test. After the restart, `DeliveryTracker` checked RO-5 and logged `RO-5 is now DELIVERED` at 21:56:57. Inventory then added the new stock, and `BackorderService` saw there was now enough P100, so it filled the order at 21:56:58. Finally, my app told Tiangge the order was accepted, and Tiangge confirmed it at 21:56:59.
