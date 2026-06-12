package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiActionBindingReportDecoderTest {

    @Test
    void decodesStringyReportMapsAndActionLists() {
        WayangA2uiActionBindingReport report = WayangA2uiActionBindingReportDecoder.fromMap(Map.of(
                "policyActions",
                "wayang.run.inspect wayang.run.wait custom.allowed wayang.run.inspect",
                "handlerActions",
                List.of("wayang.run.inspect", "custom.orphan", "custom.orphan"),
                "missingHandlerActions",
                "wayang.run.wait custom.allowed",
                "orphanHandlerActions",
                "custom.orphan"));

        assertThat(report.complete()).isFalse();
        assertThat(report.policyActions())
                .containsExactly(WayangA2uiActions.RUN_INSPECT, WayangA2uiActions.RUN_WAIT, "custom.allowed");
        assertThat(report.handlerActions())
                .containsExactly(WayangA2uiActions.RUN_INSPECT, "custom.orphan");
        assertThat(report.missingHandlerActions())
                .containsExactly(WayangA2uiActions.RUN_WAIT, "custom.allowed");
        assertThat(report.orphanHandlerActions()).containsExactly("custom.orphan");
    }

    @Test
    void recordFactoriesDelegateToDecoder() {
        Map<String, Object> values = Map.of(
                "policyActions",
                List.of(WayangA2uiActions.RUN_INSPECT),
                "handlerActions",
                List.of(WayangA2uiActions.RUN_INSPECT));

        assertThat(WayangA2uiActionBindingReport.fromMap(values))
                .isEqualTo(WayangA2uiActionBindingReportDecoder.fromMap(values));
    }

    @Test
    void decodesJsonAndKeepsValidationMessagesStable() {
        WayangA2uiActionBindingReport decoded = WayangA2uiActionBindingReportDecoder.fromJson("""
                {
                  "policyActions": ["wayang.run.inspect", "wayang.run.wait"],
                  "handlerActions": ["wayang.run.inspect"],
                  "missingHandlerActions": ["wayang.run.wait"]
                }
                """);

        assertThat(decoded.policyActions())
                .containsExactly(WayangA2uiActions.RUN_INSPECT, WayangA2uiActions.RUN_WAIT);
        assertThat(decoded.handlerActions()).containsExactly(WayangA2uiActions.RUN_INSPECT);
        assertThat(decoded.missingHandlerActions()).containsExactly(WayangA2uiActions.RUN_WAIT);
        assertThat(decoded.complete()).isFalse();
        assertThat(WayangA2uiActionBindingReport.fromJson(decoded.toJson())).isEqualTo(decoded);
        assertThatThrownBy(() -> WayangA2uiActionBindingReportDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI action binding report JSON must not be blank");
        assertThatThrownBy(() -> WayangA2uiActionBindingReportDecoder.fromJson("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to decode A2UI action binding report JSON");
    }

    @Test
    void decodesTransportAndHttpExchangeResponses() {
        RecordingWayangGollekSdk sdk = new RecordingWayangGollekSdk();
        WayangA2uiTransportAdapter transportAdapter = new WayangA2uiTransportAdapter(
                sdk,
                WayangA2uiSessionConfig.runLifecycle());
        WayangA2uiHttpBridgeAdapter httpAdapter = WayangA2uiHttpBridgeAdapter.from(transportAdapter);

        WayangA2uiActionBindingReport transportReport =
                WayangA2uiActionBindingReport.from(transportAdapter.exchangeActionBindingReport());
        WayangA2uiActionBindingReport httpReport =
                WayangA2uiActionBindingReport.from(httpAdapter.exchange(
                        WayangA2uiTransportRequest.actionBindingReport().toJson()));

        assertThat(transportReport.complete()).isTrue();
        assertThat(httpReport).isEqualTo(transportReport);
        assertThat(httpReport.policyActions()).containsExactlyElementsOf(WayangA2uiActions.runLifecycleActionOrder());
        assertThat(sdk.inspected).isZero();
    }
}
