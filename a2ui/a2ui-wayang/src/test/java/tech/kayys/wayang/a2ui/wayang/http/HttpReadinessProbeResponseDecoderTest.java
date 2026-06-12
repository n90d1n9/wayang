package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiActions;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpReadinessProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpReadinessProbeResultDecoder;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportContent;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportResponse;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.a2ui.wayang.http.HttpResponseDecoderTestFixtures.jsonBody;
import static tech.kayys.wayang.a2ui.wayang.http.HttpResponseDecoderTestFixtures.jsonResponse;

class HttpReadinessProbeResponseDecoderTest {

    @Test
    void decodesTransportEnvelopeBody() {
        WayangA2uiHttpReadinessProbeResult expected =
                WayangA2uiHttpReadinessProbeResultDecoder.fromMap(readinessProbeValues());
        WayangA2uiHttpResponse response = jsonResponse(WayangA2uiTransportResponse.from(expected).toJson());

        WayangA2uiHttpReadinessProbeResult decoded =
                HttpReadinessProbeResponseDecoder.from(response);

        assertThat(decoded).isEqualTo(expected);
        assertThat(WayangA2uiHttpReadinessProbeResult.from(response)).isEqualTo(expected);
    }

    @Test
    void decodesRawReadinessJsonBody() {
        WayangA2uiHttpResponse response = jsonResponse(jsonBody(readinessProbeValues()));

        WayangA2uiHttpReadinessProbeResult decoded =
                HttpReadinessProbeResponseDecoder.from(response);

        assertThat(decoded.passed()).isTrue();
        assertThat(decoded.smokeRequired()).isTrue();
        assertThat(decoded.actionBindingPassed()).isTrue();
        assertThat(decoded.bindingReportProbe().routeOperations())
                .containsExactly(WayangA2uiHttpRoute.OPERATION_EXCHANGE, WayangA2uiHttpRoute.OPERATION_SMOKE);
    }

    @Test
    void blankOrMalformedBodiesDecodeToEmptyProbeResult() {
        assertThat(HttpReadinessProbeResponseDecoder.from(jsonResponse(" ")).passed()).isFalse();
        assertThat(HttpReadinessProbeResponseDecoder.from(jsonResponse("not-json")).passed()).isFalse();
    }

    private static Map<String, Object> readinessProbeValues() {
        return Map.of(
                "smokeRequired",
                true,
                "bindingReportProbe",
                bindingReportProbeValues(),
                "actionBindingProbe",
                actionBindingProbeValues(),
                "smokeProbe",
                smokeProbeValues());
    }

    private static Map<String, Object> bindingReportProbeValues() {
        return Map.ofEntries(
                Map.entry("statusCode", 200),
                Map.entry("httpSuccessful", true),
                Map.entry("routeOperation", WayangA2uiHttpRoute.OPERATION_BINDING_REPORT),
                Map.entry("contentType", WayangA2uiTransportContent.MIME_JSON),
                Map.entry(WayangA2uiTransportFields.MIME_TYPE, WayangA2uiTransportContent.MIME_JSON),
                Map.entry(WayangA2uiTransportFields.BODY_ENCODING, WayangA2uiTransportContent.ENCODING_JSON),
                Map.entry(WayangA2uiTransportFields.COMPLETE, true),
                Map.entry(
                        "routeOperations",
                        List.of(WayangA2uiHttpRoute.OPERATION_EXCHANGE, WayangA2uiHttpRoute.OPERATION_SMOKE)),
                Map.entry(
                        WayangA2uiTransportFields.METADATA,
                        Map.of(
                                WayangA2uiTransportFields.RESPONSE_KIND,
                                WayangA2uiTransportFields.RESPONSE_KIND_HTTP_BINDING_REPORT)));
    }

    private static Map<String, Object> smokeProbeValues() {
        return Map.of(
                "statusCode",
                200,
                "httpSuccessful",
                true,
                "routeOperation",
                WayangA2uiHttpRoute.OPERATION_SMOKE,
                "summary",
                Map.of(
                        WayangA2uiTransportFields.PASSED,
                        true,
                        WayangA2uiTransportFields.EXIT_CODE,
                        0));
    }

    private static Map<String, Object> actionBindingProbeValues() {
        return Map.ofEntries(
                Map.entry("statusCode", 200),
                Map.entry("httpSuccessful", true),
                Map.entry("routeOperation", WayangA2uiHttpRoute.OPERATION_EXCHANGE),
                Map.entry("contentType", WayangA2uiTransportContent.MIME_JSON),
                Map.entry(WayangA2uiTransportFields.MIME_TYPE, WayangA2uiTransportContent.MIME_JSON),
                Map.entry(WayangA2uiTransportFields.BODY_ENCODING, WayangA2uiTransportContent.ENCODING_JSON),
                Map.entry(WayangA2uiTransportFields.COMPLETE, true),
                Map.entry("policyActions", List.of(WayangA2uiActions.RUN_INSPECT)),
                Map.entry("handlerActions", List.of(WayangA2uiActions.RUN_INSPECT)),
                Map.entry(
                        WayangA2uiTransportFields.METADATA,
                        Map.of(
                                WayangA2uiTransportFields.RESPONSE_KIND,
                                WayangA2uiTransportFields.RESPONSE_KIND_ACTION_BINDING_REPORT)));
    }
}
