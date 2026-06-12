package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiHttpEndpointDiagnosticSummaryDecoderTest {

    @Test
    void decodesStringySummaryMaps() {
        WayangA2uiHttpEndpointDiagnosticSummary summary =
                WayangA2uiHttpEndpointDiagnosticSummaryDecoder.fromMap(Map.ofEntries(
                        Map.entry("diagnosticsId", "decoder-summary"),
                        Map.entry("passed", "false"),
                        Map.entry("exitCode", "1"),
                        Map.entry("exchangeCount", "2"),
                        Map.entry("issueCount", "1"),
                        Map.entry("knownPathCount", "1"),
                        Map.entry("unknownPathCount", "1"),
                        Map.entry("matchedCount", "1"),
                        Map.entry("unmatchedCount", "1"),
                        Map.entry("successfulCount", "1"),
                        Map.entry("clientErrorCount", "1"),
                        Map.entry("serverErrorCount", "0"),
                        Map.entry("transportErrors", "true"),
                        Map.entry("statusCodes", List.of("200", "bad", "404")),
                        Map.entry("outcomes", List.of("SUCCESS", " TRANSPORT_ERROR ", "")),
                        Map.entry("issueCategories", List.of("unknown-path", " ")),
                        Map.entry("errorCodes", List.of("not_found")),
                        Map.entry("attributes", Map.of("source", "decoder"))));

        assertThat(summary.diagnosticsId()).isEqualTo("decoder-summary");
        assertThat(summary.passed()).isFalse();
        assertThat(summary.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_FAILURE);
        assertThat(summary.statusCodes()).containsExactly(200, 404);
        assertThat(summary.outcomes()).containsExactly("SUCCESS", "TRANSPORT_ERROR");
        assertThat(summary.issueCategories()).containsExactly("unknown-path");
        assertThat(summary.errorCodes()).containsExactly("not_found");
        assertThat(summary.attributes()).containsEntry("source", "decoder");
    }

    @Test
    void recordFactoriesDelegateToDecoder() {
        Map<String, Object> values = Map.of(
                "diagnosticsId",
                "delegated-summary",
                "passed",
                true,
                "exitCode",
                0);

        assertThat(WayangA2uiHttpEndpointDiagnosticSummary.fromMap(values))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticSummaryDecoder.fromMap(values));
    }

    @Test
    void emptyMapDecodesThroughExplicitSummaryFallback() {
        assertThat(WayangA2uiHttpEndpointDiagnosticSummaryDecoder.fromMap(Map.of()))
                .isEqualTo(WayangA2uiHttpEndpointDiagnosticSummary.empty());
    }

    @Test
    void decodesJsonAndKeepsValidationMessagesStable() {
        WayangA2uiHttpEndpointDiagnosticSummary decoded =
                WayangA2uiHttpEndpointDiagnosticSummaryDecoder.fromJson("""
                        {
                          "diagnosticsId": "json-decoder-summary",
                          "passed": true,
                          "exitCode": 0,
                          "statusCodes": [200]
                        }
                        """);

        assertThat(decoded.diagnosticsId()).isEqualTo("json-decoder-summary");
        assertThat(decoded.statusCodes()).containsExactly(200);
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticSummaryDecoder.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP endpoint diagnostic summary JSON must not be blank");
    }
}
