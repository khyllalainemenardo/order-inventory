package edu.cit.menardo.channel;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;

import edu.cit.menardo.app.AppInstance;
import edu.cit.menardo.channel.TianggeJson.CancellationConfirmation;
import edu.cit.menardo.channel.TianggeJson.Decision;
import edu.cit.menardo.channel.TianggeJson.FeedPage;
import edu.cit.menardo.channel.TianggeJson.Heartbeat;
import edu.cit.menardo.channel.TianggeJson.HeartbeatReply;
import edu.cit.menardo.channel.TianggeJson.Listing;
import edu.cit.menardo.channel.TianggeJson.Resolution;
import edu.cit.menardo.channel.TianggeJson.StockEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
class TianggeClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private final JsonMapper json = JsonMapper.builder().build();
    private final String baseUrl;
    private final String clientId;
    private final String apiKey;
    private final AppInstance instance;

    TianggeClient(@Value("${tiangge.base-url}") String baseUrl,
                  @Value("${legacysupply.client-id}") String clientId,
                  @Value("${legacysupply.api-key}") String apiKey,
                  AppInstance instance) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Set the LS_API_KEY environment variable to your LegacySupply/Tiangge API key.");
        }
        this.baseUrl = baseUrl;
        this.clientId = clientId;
        this.apiKey = apiKey;
        this.instance = instance;
    }

    HeartbeatReply heartbeat(Heartbeat heartbeat) {
        return read(send("POST", "/instances/heartbeat", heartbeat), HeartbeatReply.class);
    }

    void publishListings(List<Listing> listings) {
        send("PUT", "/listings", listings);
    }

    void publishStock(List<StockEntry> stock) {
        send("PUT", "/stock", stock);
    }

    FeedPage feed(long after, int limit) {
        return read(send("GET", "/feed?after=" + after + "&limit=" + limit, null), FeedPage.class);
    }

    void decide(String orderId, Decision decision) {
        send("POST", "/orders/" + orderId + "/decision", decision);
    }

    void resolve(String orderId, Resolution resolution) {
        send("POST", "/orders/" + orderId + "/resolution", resolution);
    }

    void confirmCancellation(String orderId) {
        send("POST", "/orders/" + orderId + "/cancellation", new CancellationConfirmation(true));
    }

    private String send(String method, String path, Object body) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("X-Client-Id", clientId)
                .header("Authorization", "Bearer " + apiKey)
                .header(AppInstance.HEADER, instance.id())
                .header("Accept", "application/json");
        if (body != null) {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)));
        } else {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        }

        HttpResponse<String> response;
        try {
            response = http.send(request.build(), HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw TianggeException.noResponse("timed out after " + TIMEOUT.toSeconds() + "s");
        } catch (IOException e) {
            throw TianggeException.noResponse(e.getClass().getSimpleName());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw TianggeException.noResponse("interrupted");
        }

        if (response.statusCode() < 300) {
            return response.body();
        }
        TianggeJson.Error error = readError(response.body());
        throw TianggeException.fromResponse(response.statusCode(), error.error(), error.message());
    }

    private <T> T read(String body, Class<T> type) {
        try {
            return json.readValue(body, type);
        } catch (JacksonException e) {
            throw TianggeException.noResponse("unreadable reply: " + e.getOriginalMessage());
        }
    }

    private TianggeJson.Error readError(String body) {
        try {
            TianggeJson.Error error = json.readValue(body, TianggeJson.Error.class);
            return error.error() == null ? new TianggeJson.Error("unknown", body) : error;
        } catch (JacksonException e) {
            return new TianggeJson.Error("unknown", body);
        }
    }
}
