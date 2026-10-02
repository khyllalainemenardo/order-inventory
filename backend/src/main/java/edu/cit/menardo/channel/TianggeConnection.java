package edu.cit.menardo.channel;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import edu.cit.menardo.app.AppInstance;
import edu.cit.menardo.channel.TianggeJson.Heartbeat;
import edu.cit.menardo.channel.TianggeJson.HeartbeatReply;
import edu.cit.menardo.channel.TianggeJson.Listing;
import edu.cit.menardo.inventory.InventoryService;
import edu.cit.menardo.inventory.InventoryView;
import edu.cit.menardo.supplier.SupplierGateway;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
class TianggeConnection {

    private static final Logger log = LoggerFactory.getLogger(TianggeConnection.class);

    private static final int MAX_LISTINGS = 10;
    private static final long RETRY_SECONDS = 5;

    private final ScheduledExecutorService heartbeats = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "tiangge-heartbeat");
        thread.setDaemon(true);
        return thread;
    });
    private final ScheduledExecutorService startup = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "tiangge-go-live");
        thread.setDaemon(true);
        return thread;
    });

    private final TianggeClient client;
    private final TianggeTranslator translator;
    private final TianggeOutbox outbox;
    private final FeedPoller poller;
    private final InventoryService inventoryService;
    private final SupplierGateway supplierGateway;
    private final AppInstance instance;
    private final String appName;
    private final long heartbeatSeconds;

    private volatile Instant lastHeartbeat;
    private volatile boolean firstHeartbeatDone;

    TianggeConnection(TianggeClient client,
                      TianggeTranslator translator,
                      TianggeOutbox outbox,
                      FeedPoller poller,
                      InventoryService inventoryService,
                      SupplierGateway supplierGateway,
                      AppInstance instance,
                      @Value("${spring.application.name}") String appName,
                      @Value("${tiangge.heartbeat-every-seconds}") long heartbeatSeconds) {
        this.client = client;
        this.translator = translator;
        this.outbox = outbox;
        this.poller = poller;
        this.inventoryService = inventoryService;
        this.supplierGateway = supplierGateway;
        this.instance = instance;
        this.appName = appName;
        this.heartbeatSeconds = heartbeatSeconds;
    }

    @EventListener(ApplicationReadyEvent.class)
    void goLive() {
        log.info("Going live on Tiangge as instance {}", instance.id());
        heartbeats.execute(this::heartbeat);
        startup.execute(this::publishShop);
    }

    @PreDestroy
    void stop() {
        heartbeats.shutdownNow();
        startup.shutdownNow();
    }

    Instant lastHeartbeat() {
        return lastHeartbeat;
    }

    private void heartbeat() {
        long next = RETRY_SECONDS;
        try {
            long uptime = Duration.between(instance.startedAt(), Instant.now()).toSeconds();
            HeartbeatReply reply = client.heartbeat(new Heartbeat(appName, instance.startedAt().toString(), uptime));
            lastHeartbeat = Instant.now();
            if (!firstHeartbeatDone) {
                firstHeartbeatDone = true;
                log.info("First heartbeat accepted (instance {})", instance.id());
            }
            Integer suggested = reply == null ? null : reply.nextHeartbeatSeconds();
            next = suggested == null || suggested < 1 ? heartbeatSeconds : Math.min(heartbeatSeconds, suggested);
        } catch (RuntimeException e) {
            log.warn("Heartbeat failed: {}. Retrying in {}s", e.getMessage(), next);
        }
        heartbeats.schedule(this::heartbeat, next, TimeUnit.SECONDS);
    }

    private void publishShop() {
        if (!firstHeartbeatDone) {
            startup.schedule(this::publishShop, 1, TimeUnit.SECONDS);
            return;
        }
        try {
            List<Listing> listings = inventoryService.listItems().stream()
                    .filter(item -> supplierGateway.supplierItemFor(item.productId()).isPresent())
                    .limit(MAX_LISTINGS)
                    .map(this::toListing)
                    .toList();
            client.publishListings(listings);
            log.info("Published {} listings: {}", listings.size(), listings);

            Set<String> skus = new LinkedHashSet<>();
            listings.forEach(listing -> skus.add(listing.sellerSku()));
            outbox.listed(skus);
            outbox.publishAllStockNow();
            poller.start();
        } catch (RuntimeException e) {
            log.warn("Publishing the shop failed: {}. Retrying in {}s", e.getMessage(), RETRY_SECONDS);
            startup.schedule(this::publishShop, RETRY_SECONDS, TimeUnit.SECONDS);
        }
    }

    private Listing toListing(InventoryView item) {
        return translator.toListing(item, supplierGateway.supplierItemFor(item.productId()).orElseThrow());
    }
}
