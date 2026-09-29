package edu.cit.menardo.supplier;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class LegacySupplyClient {

    private static final Logger log = LoggerFactory.getLogger(LegacySupplyClient.class);

    private static final Duration TIMEOUT = Duration.ofSeconds(3);
    private static final int MAX_ATTEMPTS = 3;
    private static final long FIRST_BACKOFF_MS = 500;

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final String baseUrl;
    private final String clientId;
    private final String apiKey;
    private final Duration sessionLifetime;

    private String sessionToken;
    private Instant sessionStartedAt;

    LegacySupplyClient(@Value("${legacysupply.base-url}") String baseUrl,
                       @Value("${legacysupply.client-id}") String clientId,
                       @Value("${legacysupply.api-key}") String apiKey,
                       @Value("${legacysupply.session-renew-after-seconds}") long sessionRenewAfterSeconds) {
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.sessionLifetime = Duration.ofSeconds(sessionRenewAfterSeconds);
    }

    LegacyOrderAck placeOrder(String supplierSku, int qty, String buyerRef, String requestId) {
        String body = LegacySupplyXml.purchaseOrder(supplierSku, qty, buyerRef);
        return LegacySupplyXml.orderAck(call("POST", "/purchase-orders", body, requestId));
    }

    LegacyOrderAck getOrder(String poNumber) {
        return LegacySupplyXml.orderAck(call("GET", "/purchase-orders/" + poNumber, null, null));
    }

    Optional<LegacyOrderAck> findOrderByBuyerRef(String buyerRef) {
        String query = "?buyerRef=" + URLEncoder.encode(buyerRef, StandardCharsets.UTF_8);
        return LegacySupplyXml.firstOrderInList(call("GET", "/purchase-orders" + query, null, null));
    }

    private synchronized String call(String method, String path, String body, String requestId) {
        LegacySupplyException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                return sendWithSession(method, path, body, requestId);
            } catch (LegacySupplyException e) {
                lastError = e;
                log.warn("LegacySupply {} {} failed on attempt {}/{}: {}", method, path, attempt, MAX_ATTEMPTS, e.getMessage());
                if (!e.isRetryable()) {
                    throw e;
                }
                if (e.isSessionProblem()) {
                    sessionToken = null;
                }
                if (attempt < MAX_ATTEMPTS) {
                    pause(FIRST_BACKOFF_MS * (1L << (attempt - 1)));
                }
            }
        }
        throw lastError;
    }

    private String sendWithSession(String method, String path, String body, String requestId) {
        if (sessionToken == null || sessionIsOld()) {
            signIn();
        }

        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("X-LS-Session", sessionToken);
        if (requestId != null) {
            request.header("X-Request-Id", requestId);
        }
        if (body != null) {
            request.header("Content-Type", "application/xml")
                    .method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            request.GET();
        }
        return execute(request.build());
    }

    private void signIn() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/auth/token"))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/xml")
                .POST(HttpRequest.BodyPublishers.ofString(LegacySupplyXml.authRequest(clientId, apiKey)))
                .build();
        sessionToken = LegacySupplyXml.sessionToken(execute(request));
        sessionStartedAt = Instant.now();
        log.info("Signed in to LegacySupply");
    }

    private boolean sessionIsOld() {
        return sessionStartedAt.plus(sessionLifetime).isBefore(Instant.now());
    }

    private String execute(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw LegacySupplyException.noResponse("timed out after " + TIMEOUT.toSeconds() + "s");
        } catch (IOException e) {
            throw LegacySupplyException.noResponse(e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw LegacySupplyException.noResponse("interrupted");
        }

        if (response.statusCode() < 300) {
            return response.body();
        }
        throw LegacySupplyException.fromResponse(response.statusCode(), LegacySupplyXml.errorCode(response.body()));
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
