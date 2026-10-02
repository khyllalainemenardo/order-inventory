package edu.cit.menardo.channel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import edu.cit.menardo.channel.TianggeJson.FeedEvent;
import edu.cit.menardo.channel.TianggeJson.FeedPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
class FeedPoller {

    private static final Logger log = LoggerFactory.getLogger(FeedPoller.class);

    private static final int PAGE_SIZE = 50;
    private static final int MAX_PAGES_PER_RUN = 20;
    private static final int MAX_FAILURES_PER_EVENT = 8;

    private final TianggeClient client;
    private final FeedEventHandler handler;
    private final TianggeOutbox outbox;

    private final Map<String, Integer> failures = new HashMap<>();
    private final AtomicLong lastCursor = new AtomicLong(-1);
    private volatile boolean live;

    FeedPoller(TianggeClient client, FeedEventHandler handler, TianggeOutbox outbox) {
        this.client = client;
        this.handler = handler;
        this.outbox = outbox;
    }

    void start() {
        long cursor = handler.cursor();
        lastCursor.set(cursor);
        live = true;
        log.info("Reading the Tiangge feed from cursor {}", cursor);
    }

    boolean isLive() {
        return live;
    }

    long lastCursor() {
        return lastCursor.get();
    }

    @Scheduled(initialDelay = 2_000, fixedDelayString = "${tiangge.poll-every-ms}")
    public void poll() {
        if (!live) {
            return;
        }
        try {
            for (int page = 0; page < MAX_PAGES_PER_RUN; page++) {
                if (!readOnePage()) {
                    return;
                }
            }
        } catch (RuntimeException e) {
            log.warn("Reading the feed failed: {}", e.getMessage());
        }
    }

    private boolean readOnePage() {
        long cursor = handler.cursor();
        FeedPage page = client.feed(cursor, PAGE_SIZE);
        lastCursor.set(cursor);
        if (page.events() == null || page.events().isEmpty()) {
            return false;
        }

        for (FeedEvent event : page.events()) {
            if (event.seq() <= cursor) {
                continue;
            }
            if (!handleOne(withEventId(event))) {
                return false;
            }
            cursor = event.seq();
            lastCursor.set(cursor);
        }
        return page.events().size() >= PAGE_SIZE;
    }

    private boolean handleOne(FeedEvent event) {
        try {
            FeedEventHandler.Reply reply = handler.handle(event, outbox.listedSkus());
            failures.remove(event.eventId());
            switch (reply) {
                case DECISION -> outbox.decisionReady(event.orderId());
                case CANCELLATION -> outbox.cancellationReady(event.orderId());
                case NONE -> {
                }
            }
            return true;
        } catch (RuntimeException e) {
            int count = failures.merge(event.eventId(), 1, Integer::sum);
            log.warn("Event {} (seq {}, {} {}) failed, attempt {}: {}", event.eventId(), event.seq(), event.type(),
                    event.orderId(), count, e.toString());
            if (count >= MAX_FAILURES_PER_EVENT) {
                log.error("Giving up on event {} after {} attempts", event.eventId(), count);
                handler.skip(event);
                failures.remove(event.eventId());
                return true;
            }
            return false;
        }
    }

    private static FeedEvent withEventId(FeedEvent event) {
        if (event.eventId() != null) {
            return event;
        }
        return new FeedEvent(event.seq(), "seq-" + event.seq(), event.type(), event.orderId(), event.placedAt(),
                event.decisionDeadline(), event.cancelledAt(), event.confirmDeadline(), event.lines());
    }
}
