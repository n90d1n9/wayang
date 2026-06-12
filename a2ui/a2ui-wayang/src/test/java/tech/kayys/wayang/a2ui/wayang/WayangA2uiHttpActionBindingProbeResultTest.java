package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiHttpActionBindingProbeResultTest {

    @Test
    void probesActionBindingReportThroughHttpExchange() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(new WayangA2uiTransportAdapter(sdk));

        WayangA2uiHttpActionBindingProbeResult probe = adapter.actionBindingProbe();

        assertThat(probe.statusCode()).isEqualTo(200);
        assertThat(probe.httpSuccessful()).isTrue();
        assertThat(probe.exchangeRoute()).isTrue();
        assertThat(probe.actionBindingResult()).isTrue();
        assertThat(probe.jsonContent()).isTrue();
        assertThat(probe.complete()).isFalse();
        assertThat(probe.passed()).isTrue();
        assertThat(probe.policyActionCount()).isEqualTo(1);
        assertThat(probe.handlerActionCount()).isEqualTo(5);
        assertThat(probe.missingHandlerCount()).isZero();
        assertThat(probe.orphanHandlerCount()).isGreaterThan(0);
        assertThat(probe.issueCount()).isZero();
        assertThat(probe.issues()).isEmpty();
        assertThat(probe.policyActions()).containsExactly(WayangA2uiActions.RUN_INSPECT);
        assertThat(probe.handlerActions()).contains(WayangA2uiActions.RUN_CANCEL);
        assertThat(probe.headers())
                .containsEntry(WayangA2uiHttpResponse.HEADER_A2UI_ROUTE_OPERATION,
                        WayangA2uiHttpRoute.OPERATION_EXCHANGE)
                .containsEntry(WayangA2uiHttpResponse.HEADER_ALLOW, "POST, OPTIONS");
        assertThat(sdk.inspected).isZero();
    }

    @Test
    void failsWhenAllowedActionHasNoHandler() {
        WayangA2uiActionPolicy policy = new WayangA2uiActionPolicy(Set.of("custom.allowed"), Set.of(), Map.of());
        WayangA2uiActionRouter router = new WayangA2uiActionRouter(
                policy,
                WayangA2uiSurfaceRegistry.readOnly(),
                WayangA2uiActionHandlers.builder().build());
        WayangA2uiHttpBridgeAdapter adapter = WayangA2uiHttpBridgeAdapter.from(
                new WayangA2uiTransportAdapter(new WayangA2uiSession(router)));

        WayangA2uiHttpActionBindingProbeResult probe = adapter.actionBindingProbe();

        assertThat(probe.passed()).isFalse();
        assertThat(probe.complete()).isFalse();
        assertThat(probe.policyActions()).containsExactly("custom.allowed");
        assertThat(probe.handlerActions()).isEmpty();
        assertThat(probe.missingHandlerActions()).containsExactly("custom.allowed");
        assertThat(probe.orphanHandlerActions()).isEmpty();
        assertThat(probe.issueCount()).isEqualTo(1);
        assertThat(probe.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("source", "actionBinding")
                        .containsEntry("field", "missingHandlerActions")
                        .containsEntry("action", "custom.allowed"));
    }

    @Test
    void decodesStoredProbeMapsAndJson() {
        WayangA2uiHttpActionBindingProbeResult decoded =
                WayangA2uiHttpActionBindingProbeResultDecoder.fromMap(Map.ofEntries(
                        Map.entry("statusCode", "200"),
                        Map.entry("httpSuccessful", "true"),
                        Map.entry("routeOperation", WayangA2uiHttpRoute.OPERATION_EXCHANGE),
                        Map.entry("contentType", WayangA2uiTransportContent.MIME_JSON),
                        Map.entry(WayangA2uiTransportFields.MIME_TYPE, WayangA2uiTransportContent.MIME_JSON),
                        Map.entry(WayangA2uiTransportFields.BODY_ENCODING,
                                WayangA2uiTransportContent.ENCODING_JSON),
                        Map.entry("policyActions", "wayang.run.inspect custom.allowed"),
                        Map.entry("handlerActions", List.of(WayangA2uiActions.RUN_INSPECT)),
                        Map.entry("missingHandlerActions", "custom.allowed"),
                        Map.entry(WayangA2uiTransportFields.METADATA, Map.of(
                                WayangA2uiTransportFields.RESPONSE_KIND,
                                WayangA2uiTransportFields.RESPONSE_KIND_ACTION_BINDING_REPORT))));

        assertThat(decoded.policyActions()).containsExactly(WayangA2uiActions.RUN_INSPECT, "custom.allowed");
        assertThat(decoded.missingHandlerActions()).containsExactly("custom.allowed");
        assertThat(decoded.issueCount()).isEqualTo(1);
        assertThat(decoded.passed()).isFalse();
        assertThat(WayangA2uiHttpActionBindingProbeResult.fromJson(decoded.toJson())).isEqualTo(decoded);
        assertThatThrownBy(() -> WayangA2uiHttpActionBindingProbeResultDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP action binding probe result JSON must not be blank");
    }

    @Test
    void emptyProbeFactoryNamesFailedUnpopulatedState() {
        WayangA2uiHttpActionBindingProbeResult probe = WayangA2uiHttpActionBindingProbeResult.empty();

        assertThat(WayangA2uiHttpActionBindingProbeResult.fromMap(Map.of())).isEqualTo(probe);
        assertThat(WayangA2uiHttpActionBindingProbeResultDecoder.fromMap(Map.of())).isEqualTo(probe);
        assertThat(probe.statusCode()).isZero();
        assertThat(probe.httpSuccessful()).isFalse();
        assertThat(probe.routeOperation()).isEmpty();
        assertThat(probe.exchangeRoute()).isFalse();
        assertThat(probe.actionBindingResult()).isFalse();
        assertThat(probe.jsonContent()).isFalse();
        assertThat(probe.passed()).isFalse();
        assertThat(probe.policyActions()).isEmpty();
        assertThat(probe.handlerActions()).isEmpty();
        assertThat(probe.missingHandlerActions()).isEmpty();
        assertThat(probe.orphanHandlerActions()).isEmpty();
    }

    @Test
    void compatibilityFallbackNamesUnprobedReadinessState() {
        WayangA2uiHttpActionBindingProbeResult fallback =
                WayangA2uiHttpActionBindingProbeResult.compatibilityFallback();

        assertThat(fallback).isEqualTo(WayangA2uiHttpActionBindingProbeResult.passedWithoutProbe());
        assertThat(fallback.passed()).isTrue();
        assertThat(fallback.statusCode()).isEqualTo(200);
        assertThat(fallback.exchangeRoute()).isTrue();
        assertThat(fallback.actionBindingResult()).isTrue();
        assertThat(fallback.jsonContent()).isTrue();
        assertThat(fallback.complete()).isTrue();
        assertThat(fallback.policyActions()).isEmpty();
        assertThat(fallback.handlerActions()).isEmpty();
        assertThat(fallback.missingHandlerActions()).isEmpty();
        assertThat(fallback.orphanHandlerActions()).isEmpty();
        assertThat(fallback.metadata())
                .containsEntry(
                        WayangA2uiTransportFields.RESPONSE_KIND,
                        WayangA2uiTransportFields.RESPONSE_KIND_ACTION_BINDING_REPORT);
    }
}
