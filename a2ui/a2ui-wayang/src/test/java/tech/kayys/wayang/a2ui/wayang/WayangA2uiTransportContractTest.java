package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class WayangA2uiTransportContractTest {

    private final A2uiContractAssert contracts = new A2uiContractAssert();

    @Test
    void transportRequestEnvelopeMatchesContractFixture() throws IOException {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "original");
        Map<String, Object> dataPart = new LinkedHashMap<>();
        dataPart.put("kind", "userAction");
        dataPart.put("nested", nested);
        dataPart.put("ignored", null);

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-transport-request-envelope.json",
                WayangA2uiTransportRequest.dataPart(dataPart).toJson());
    }

    @Test
    void transportResponseEnvelopeMatchesContractFixture() throws IOException {
        WayangA2uiTransportResponse response = new WayangA2uiTransportResponse(
                WayangA2uiTransportContent.MIME_A2UI,
                WayangA2uiTransportContent.ENCODING_JSONL,
                "{\"hello\":\"world\"}\n",
                List.of(Map.of(
                        "kind", "data",
                        "nested", Map.of("value", "original"))),
                1,
                0,
                Map.of(
                        WayangA2uiTransportFields.REQUEST_KIND,
                        WayangA2uiTransportPayloadKind.JSON_LINE.name(),
                        WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_SESSION_RESULT));

        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-transport-response-envelope.json",
                response.toJson());
    }

    @Test
    void transportProblemBodyMatchesContractFixture() throws IOException {
        contracts.matchesJsonFixture(
                "contracts/a2ui/wayang-transport-problem-body.json",
                WayangA2uiTransportResponse.error("bad_request", "Bad request.").body());
    }
}
