package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiHttpEndpointDiagnosticReportTest {

    @Test
    void decodesReportMapsWithStringyCountsAndNestedLists() {
        WayangA2uiHttpEndpointDiagnosticReport report =
                WayangA2uiHttpEndpointDiagnosticReport.fromMap(Map.ofEntries(
                        Map.entry("diagnosticsId", "external-report"),
                        Map.entry("exchangeCount", "2"),
                        Map.entry("knownPathCount", "1"),
                        Map.entry("unknownPathCount", "1"),
                        Map.entry("matchedCount", "1"),
                        Map.entry("unmatchedCount", "1"),
                        Map.entry("successfulCount", "1"),
                        Map.entry("clientErrorCount", "1"),
                        Map.entry("serverErrorCount", "0"),
                        Map.entry("handledCount", "1"),
                        Map.entry("rejectedCount", "1"),
                        Map.entry("transportErrors", "true"),
                        Map.entry("statusCodes", List.of("200", "404")),
                        Map.entry("outcomes", List.of("SUCCESS", " TRANSPORT_ERROR ", "")),
                        Map.entry("exchanges", List.of(Map.of("index", 1))),
                        Map.entry("issues", List.of(Map.of("errorCode", "not_found"))),
                        Map.entry("attributes", Map.of("source", "gateway"))));

        assertThat(report.diagnosticsId()).isEqualTo("external-report");
        assertThat(report.exchangeCount()).isEqualTo(2);
        assertThat(report.statusCodes()).containsExactly(200, 404);
        assertThat(report.outcomes()).containsExactly("SUCCESS", "TRANSPORT_ERROR");
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.passed()).isFalse();
        assertThat(report.attributes()).containsEntry("source", "gateway");
        assertThat(report.toMap())
                .containsEntry("issueCount", 1)
                .containsEntry("passed", false);
    }

    @Test
    void roundTripsReportJson() {
        WayangA2uiHttpEndpointDiagnosticReport report = new WayangA2uiHttpEndpointDiagnosticReport(
                "json-report",
                1,
                1,
                0,
                1,
                0,
                1,
                0,
                0,
                1,
                0,
                false,
                List.of(200),
                List.of(WayangA2uiTransportOutcome.SUCCESS.name()),
                List.of(Map.of("index", 1)),
                List.of(),
                Map.of("source", "json"));

        WayangA2uiHttpEndpointDiagnosticReport decoded =
                WayangA2uiHttpEndpointDiagnosticReport.fromJson(report.toJson());

        assertThat(decoded).isEqualTo(report);
        assertThat(decoded.toJson())
                .contains("\"diagnosticsId\":\"json-report\"")
                .contains("\"passed\":true");
    }

    @Test
    void emptyReportFactoryNamesFallbackState() {
        WayangA2uiHttpEndpointDiagnosticReport report = WayangA2uiHttpEndpointDiagnosticReport.empty();

        assertThat(WayangA2uiHttpEndpointDiagnosticReport.fromMap(Map.of())).isEqualTo(report);
        assertThat(report.diagnosticsId()).isEqualTo(WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID);
        assertThat(report.exchangeCount()).isZero();
        assertThat(report.issueCount()).isZero();
        assertThat(report.passed()).isTrue();
        assertThat(report.summary()).isEqualTo(WayangA2uiHttpEndpointDiagnosticSummary.empty());
    }

    @Test
    void rejectsBlankInvalidOrNonNumericReportJson() {
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticReport.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP endpoint diagnostic report JSON must not be blank");
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticReport.fromJson("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to decode A2UI HTTP endpoint diagnostic report JSON");
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticReport.fromMap(Map.of(
                "exchangeCount",
                "nope")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("A2UI HTTP endpoint diagnostic report count must be numeric: exchangeCount");
    }
}
