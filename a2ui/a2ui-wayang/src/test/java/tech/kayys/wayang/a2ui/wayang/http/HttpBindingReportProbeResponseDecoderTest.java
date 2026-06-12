package tech.kayys.wayang.a2ui.wayang.http;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpBindingReportProbeResult;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpResponse;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiHttpRoute;
import tech.kayys.wayang.a2ui.wayang.WayangA2uiTransportFields;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.a2ui.wayang.http.HttpResponseDecoderTestFixtures.jsonBody;
import static tech.kayys.wayang.a2ui.wayang.http.HttpResponseDecoderTestFixtures.jsonResponse;
import static tech.kayys.wayang.a2ui.wayang.http.HttpResponseDecoderTestFixtures.transportEnvelopeJson;

class HttpBindingReportProbeResponseDecoderTest {

    @Test
    void decodesTransportEnvelopeBodyAndMetadata() {
        WayangA2uiHttpResponse response = jsonResponse(
                transportEnvelopeJson(bindingReportBodyValues(), bindingReportMetadataValues()),
                WayangA2uiHttpRoute.bindingReport());

        WayangA2uiHttpBindingReportProbeResult decoded =
                HttpBindingReportProbeResponseDecoder.from(response);

        assertThat(decoded.passed()).isTrue();
        assertThat(decoded.bindingReportRoute()).isTrue();
        assertThat(decoded.bindingReportResult()).isTrue();
        assertThat(decoded.jsonContent()).isTrue();
        assertThat(decoded.routeOperations()).containsExactly(
                WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                WayangA2uiHttpRoute.OPERATION_SMOKE);
        assertThat(decoded.missingHandlerCount()).isEqualTo(1);
        assertThat(decoded.orphanHandlerCount()).isEqualTo(1);
        assertThat(decoded.issueCount()).isEqualTo(2);
        assertThat(WayangA2uiHttpBindingReportProbeResult.from(response)).isEqualTo(decoded);
    }

    @Test
    void decodesRawBindingReportJsonBodyAsProbeBodyFallback() {
        WayangA2uiHttpResponse response = jsonResponse(
                jsonBody(bindingReportBodyValues()),
                WayangA2uiHttpRoute.bindingReport());

        WayangA2uiHttpBindingReportProbeResult decoded =
                HttpBindingReportProbeResponseDecoder.from(response);

        assertThat(decoded.bindingReportRoute()).isTrue();
        assertThat(decoded.bindingReportResult()).isFalse();
        assertThat(decoded.jsonContent()).isFalse();
        assertThat(decoded.complete()).isTrue();
        assertThat(decoded.routeOperations()).containsExactly(
                WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                WayangA2uiHttpRoute.OPERATION_SMOKE);
        assertThat(decoded.issueCount()).isEqualTo(2);
    }

    @Test
    void blankOrMalformedBodiesDecodeToEmptyProbeResult() {
        assertThat(HttpBindingReportProbeResponseDecoder
                .from(jsonResponse(" ", WayangA2uiHttpRoute.bindingReport())).passed()).isFalse();
        assertThat(HttpBindingReportProbeResponseDecoder
                .from(jsonResponse("not-json", WayangA2uiHttpRoute.bindingReport())).passed()).isFalse();
    }

    private static Map<String, Object> bindingReportMetadataValues() {
        return Map.ofEntries(
                Map.entry(
                        WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_HTTP_BINDING_REPORT),
                Map.entry(WayangA2uiTransportFields.COMPLETE, true),
                Map.entry(WayangA2uiTransportFields.ROUTE_OPERATION_COUNT, 2),
                Map.entry(WayangA2uiTransportFields.HANDLER_OPERATION_COUNT, 1),
                Map.entry(WayangA2uiTransportFields.MISSING_HANDLER_COUNT, 1),
                Map.entry(WayangA2uiTransportFields.ORPHAN_HANDLER_COUNT, 1));
    }

    private static Map<String, Object> bindingReportBodyValues() {
        return Map.ofEntries(
                Map.entry(WayangA2uiTransportFields.COMPLETE, true),
                Map.entry("routeOperations", List.of(
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE,
                        WayangA2uiHttpRoute.OPERATION_SMOKE)),
                Map.entry("handlerOperations", List.of(WayangA2uiHttpRoute.OPERATION_EXCHANGE)),
                Map.entry("missingHandlerOperations", List.of(WayangA2uiHttpRoute.OPERATION_SMOKE)),
                Map.entry("orphanHandlerOperations", List.of("a2ui.custom")),
                Map.entry("routeOperationCount", 2),
                Map.entry("handlerOperationCount", 1));
    }
}
