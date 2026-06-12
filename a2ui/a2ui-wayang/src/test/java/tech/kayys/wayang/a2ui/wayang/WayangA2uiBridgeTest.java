package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.core.A2uiJsonlCodec;
import tech.kayys.wayang.a2ui.core.A2uiUserAction;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiBridgeTest {

    private final A2uiJsonlCodec codec = new A2uiJsonlCodec();

    @Test
    void exchangesTypedBridgeRequestsThroughTransportAdapter() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiBridge bridge = WayangA2uiBridge.from(new WayangA2uiTransportAdapter(sdk));
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("value", "original");
        WayangA2uiBridgeRequest request = WayangA2uiBridgeRequest.of(
                WayangA2uiTransportRequest.jsonLine(codec.line(action(WayangA2uiActions.RUN_INSPECT))),
                Map.of("tenant", "demo", "nested", nested));
        nested.put("value", "changed");

        WayangA2uiBridgeResponse response = bridge.exchange(request).withAttributes(Map.of("transport", "unit-test"));

        assertThat(request.attributes())
                .containsEntry("tenant", "demo");
        assertThat((Map<String, Object>) request.attributes().get("nested"))
                .containsEntry("value", "original");
        assertThatThrownBy(() -> request.attributes().put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(response.outcome()).isEqualTo(WayangA2uiTransportOutcome.SUCCESS);
        assertThat(response.transportResponse().metadata())
                .containsEntry(WayangA2uiTransportFields.REQUEST_KIND, WayangA2uiTransportPayloadKind.JSON_LINE.name())
                .containsEntry(
                        WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_SESSION_RESULT);
        assertThat(response.transportEnvelope())
                .containsEntry(WayangA2uiTransportFields.HANDLED_COUNT, 1L)
                .containsEntry(WayangA2uiTransportFields.REJECTED_COUNT, 0L);
        assertThat(response.transportEnvelopeJson()).contains("wayang.run.inspect");
        assertThat(response.attributes()).containsEntry("transport", "unit-test");
        assertThat(sdk.inspected).isEqualTo(1);
    }

    @Test
    void exchangesBridgeEnvelopesAndConvertsInvalidInputsToErrors() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiBridge bridge = new WayangA2uiTransportBridge(new WayangA2uiTransportAdapter(
                sdk,
                WayangA2uiSessionConfig.readOnly()));

        WayangA2uiBridgeResponse catalogResponse =
                bridge.exchangeEnvelope(WayangA2uiTransportRequest.surfaceCatalog().toMap());
        WayangA2uiBridgeResponse jsonResponse =
                bridge.exchangeEnvelopeJson(WayangA2uiTransportRequest.surfaceCatalog().toJson());
        WayangA2uiBridgeResponse invalidMapResponse = bridge.exchangeEnvelopeOrError(Map.of("kind", "nope"));
        WayangA2uiBridgeResponse invalidJsonResponse = bridge.exchangeEnvelopeJsonOrError("{not-json");

        assertThat(catalogResponse.transportResponse().bodyEncoding())
                .isEqualTo(WayangA2uiTransportContent.ENCODING_JSON);
        assertThat(catalogResponse.transportResponse().metadata())
                .containsEntry(WayangA2uiTransportFields.REQUEST_KIND, WayangA2uiTransportPayloadKind.SURFACE_CATALOG.name())
                .containsEntry(
                        WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_SURFACE_CATALOG);
        assertThat(jsonResponse.transportResponse().body()).contains(WayangA2uiSurfaceRegistry.ACTION_RESULT);
        assertThat(invalidMapResponse.outcome()).isEqualTo(WayangA2uiTransportOutcome.TRANSPORT_ERROR);
        assertThat(invalidMapResponse.transportError())
                .contains(WayangA2uiTransportError.of(
                        "invalid_request_envelope",
                        "Unsupported A2UI transport request kind: nope"));
        assertThat(invalidJsonResponse.outcome()).isEqualTo(WayangA2uiTransportOutcome.TRANSPORT_ERROR);
        assertThat(invalidJsonResponse.transportError())
                .contains(WayangA2uiTransportError.of(
                        "invalid_request_json",
                        "Unable to decode A2UI transport request JSON"));
        assertThat(sdk.inspected).isZero();
    }

    private static A2uiUserAction action(String name) {
        return new A2uiUserAction(
                name,
                "main",
                "button",
                Instant.parse("2026-05-31T00:00:00Z"),
                Map.of("runId", "run-1"));
    }
}
