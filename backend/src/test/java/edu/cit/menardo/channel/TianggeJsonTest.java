package edu.cit.menardo.channel;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class TianggeJsonTest {

    private final JsonMapper json = JsonMapper.builder().build();

    @Test
    void readsTheFeedExampleFromTheManual() {
        String body = """
                {
                  "events": [
                    { "seq": 41, "eventId": "evt_3f9a0c2b7d1e4a55", "type": "ORDER_PLACED", "orderId": "TG-K7Q2MX",
                      "placedAt": "2026-10-01T01:06:00.000Z", "decisionDeadline": "2026-10-01T01:07:00.000Z",
                      "lines": [ { "sellerSku": "P-1001", "qty": 2 } ],
                      "buyer": { "name": "Ana Reyes", "city": "Cebu City" } },
                    { "seq": 42, "eventId": "evt_91c0d7e2a6b84f10", "type": "ORDER_CANCELLED", "orderId": "TG-H3WZ8P",
                      "cancelledAt": "2026-10-01T01:06:20.000Z", "confirmDeadline": "2026-10-01T01:07:20.000Z" }
                  ],
                  "nextCursor": 42
                }""";

        TianggeJson.FeedPage page = json.readValue(body, TianggeJson.FeedPage.class);

        assertThat(page.nextCursor()).isEqualTo(42);
        assertThat(page.events()).hasSize(2);
        assertThat(page.events().get(0).lines().get(0).qty()).isEqualTo(2);
        assertThat(page.events().get(1).type()).isEqualTo("ORDER_CANCELLED");
    }

    @Test
    void writesADecisionWithoutAnEmptyReason() {
        String body = json.writeValueAsString(new TianggeJson.Decision("ACCEPTED", "SO-1", null));

        assertThat(body).isEqualTo("{\"decision\":\"ACCEPTED\",\"shopOrderId\":\"SO-1\"}");
    }
}
