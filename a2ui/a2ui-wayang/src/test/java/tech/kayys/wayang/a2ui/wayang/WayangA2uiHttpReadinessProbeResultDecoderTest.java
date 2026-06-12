package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiHttpReadinessProbeResultDecoderTest {

    @Test
    void decodesNestedProbeMapsAndSmokeRequiredFlag() {
        WayangA2uiHttpReadinessProbeResult result =
                WayangA2uiHttpReadinessProbeResultDecoder.fromMap(readinessProbeValues());

        assertThat(result.smokeRequired()).isTrue();
        assertThat(result.bindingReportProbe().statusCode()).isEqualTo(200);
        assertThat(result.bindingReportProbe().routeOperations())
                .containsExactly(WayangA2uiHttpRoute.OPERATION_EXCHANGE, WayangA2uiHttpRoute.OPERATION_SMOKE);
        assertThat(result.bindingReportPassed()).isTrue();
        assertThat(result.actionBindingPassed()).isTrue();
        assertThat(result.actionBindingProbe().policyActions()).containsExactly(WayangA2uiActions.RUN_INSPECT);
        assertThat(result.smokeProbe().statusCode()).isEqualTo(200);
        assertThat(result.smokePassed()).isTrue();
        assertThat(result.passed()).isTrue();
    }

    @Test
    void recordFactoriesDelegateToDecoder() {
        Map<String, Object> values = readinessProbeValues();

        assertThat(WayangA2uiHttpReadinessProbeResult.fromMap(values))
                .isEqualTo(WayangA2uiHttpReadinessProbeResultDecoder.fromMap(values));
    }

    @Test
    void emptyReadinessFactoryNamesFallbackState() {
        WayangA2uiHttpReadinessProbeResult readiness = WayangA2uiHttpReadinessProbeResult.empty();

        assertThat(WayangA2uiHttpReadinessProbeResult.fromMap(Map.of())).isEqualTo(readiness);
        assertThat(readiness.bindingReportPassed()).isFalse();
        assertThat(readiness.actionBindingPassed()).isTrue();
        assertThat(readiness.smokeRequired()).isFalse();
        assertThat(readiness.smokePassed()).isTrue();
        assertThat(readiness.passed()).isFalse();
        assertThat(readiness.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_FAILURE);
        assertThat(readiness.bindingReportProbe()).isEqualTo(WayangA2uiHttpBindingReportProbeResult.empty());
        assertThat(readiness.smokeProbe()).isEqualTo(WayangA2uiHttpSmokeProbeResult.empty());
    }

    @Test
    void decodesJsonAndKeepsValidationMessagesStable() {
        WayangA2uiHttpReadinessProbeResult decoded =
                WayangA2uiHttpReadinessProbeResultDecoder.fromJson("""
                        {
                          "smokeRequired": "true",
                          "bindingReportProbe": {
                            "statusCode": "200",
                            "httpSuccessful": "true",
                            "routeOperation": "a2ui.bindingReport",
                            "contentType": "application/json",
                            "mimeType": "application/json",
                            "bodyEncoding": "json",
                            "complete": "true",
                            "metadata": {"responseKind": "http-binding-report"}
                          },
                          "actionBindingProbe": {
                            "statusCode": "200",
                            "httpSuccessful": "true",
                            "routeOperation": "a2ui.exchange",
                            "contentType": "application/json",
                            "mimeType": "application/json",
                            "bodyEncoding": "json",
                            "complete": "true",
                            "policyActions": ["wayang.run.inspect"],
                            "handlerActions": ["wayang.run.inspect"],
                            "metadata": {"responseKind": "action-binding-report"}
                          },
                          "smokeProbe": {
                            "statusCode": "200",
                            "httpSuccessful": "true",
                            "routeOperation": "a2ui.smoke",
                            "summary": {"passed": "true", "exitCode": "0"}
                          }
                        }
                        """);

        assertThat(decoded.passed()).isTrue();
        assertThat(decoded.smokeRequired()).isTrue();
        assertThatThrownBy(() -> WayangA2uiHttpReadinessProbeResultDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP readiness probe result JSON must not be blank");
    }

    private static Map<String, Object> readinessProbeValues() {
        return Map.of(
                "smokeRequired",
                "true",
                "bindingReportProbe",
                bindingReportProbeValues(),
                "actionBindingProbe",
                actionBindingProbeValues(),
                "smokeProbe",
                smokeProbeValues());
    }

    private static Map<String, Object> bindingReportProbeValues() {
        return Map.ofEntries(
                Map.entry("statusCode", "200"),
                Map.entry("httpSuccessful", "true"),
                Map.entry("routeOperation", WayangA2uiHttpRoute.OPERATION_BINDING_REPORT),
                Map.entry("contentType", WayangA2uiTransportContent.MIME_JSON),
                Map.entry(WayangA2uiTransportFields.MIME_TYPE, WayangA2uiTransportContent.MIME_JSON),
                Map.entry(WayangA2uiTransportFields.BODY_ENCODING, WayangA2uiTransportContent.ENCODING_JSON),
                Map.entry(WayangA2uiTransportFields.COMPLETE, "true"),
                Map.entry(
                        "routeOperations",
                        List.of(WayangA2uiHttpRoute.OPERATION_EXCHANGE, WayangA2uiHttpRoute.OPERATION_SMOKE)),
                Map.entry(
                        WayangA2uiTransportFields.METADATA,
                        Map.of(
                                WayangA2uiTransportFields.RESPONSE_KIND,
                                WayangA2uiTransportFields.RESPONSE_KIND_HTTP_BINDING_REPORT)),
                Map.entry("headers", Map.of("Allow", "GET")));
    }

    private static Map<String, Object> smokeProbeValues() {
        return Map.of(
                "statusCode",
                "200",
                "httpSuccessful",
                "true",
                "routeOperation",
                WayangA2uiHttpRoute.OPERATION_SMOKE,
                "summary",
                Map.of(
                        WayangA2uiTransportFields.PASSED,
                        "true",
                        WayangA2uiTransportFields.EXIT_CODE,
                        "0"));
    }

    private static Map<String, Object> actionBindingProbeValues() {
        return Map.ofEntries(
                Map.entry("statusCode", "200"),
                Map.entry("httpSuccessful", "true"),
                Map.entry("routeOperation", WayangA2uiHttpRoute.OPERATION_EXCHANGE),
                Map.entry("contentType", WayangA2uiTransportContent.MIME_JSON),
                Map.entry(WayangA2uiTransportFields.MIME_TYPE, WayangA2uiTransportContent.MIME_JSON),
                Map.entry(WayangA2uiTransportFields.BODY_ENCODING, WayangA2uiTransportContent.ENCODING_JSON),
                Map.entry(WayangA2uiTransportFields.COMPLETE, "true"),
                Map.entry("policyActions", List.of(WayangA2uiActions.RUN_INSPECT)),
                Map.entry("handlerActions", List.of(WayangA2uiActions.RUN_INSPECT)),
                Map.entry(
                        WayangA2uiTransportFields.METADATA,
                        Map.of(
                                WayangA2uiTransportFields.RESPONSE_KIND,
                                WayangA2uiTransportFields.RESPONSE_KIND_ACTION_BINDING_REPORT)));
    }
}
