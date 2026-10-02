package edu.cit.menardo.app;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AppInstance {

    public static final String HEADER = "X-Client-Instance";

    private static final Logger log = LoggerFactory.getLogger(AppInstance.class);

    private final UUID id = UUID.randomUUID();
    private final Instant startedAt = Instant.now();

    AppInstance() {
        log.info("Instance ID for this run: {}", id);
    }

    public String id() {
        return id.toString();
    }

    public Instant startedAt() {
        return startedAt;
    }
}
