package tech.kayys.wayang.a2ui.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2uiHttpEndpointDiagnosticSummaryTest {

    @Test
    void summarizesFailedReportsWithDistinctIssueTaxonomy() {
        WayangA2uiHttpEndpointDiagnosticReport report = new WayangA2uiHttpEndpointDiagnosticReport(
                "summary-report",
                3,
                2,
                1,
                1,
                2,
                1,
                2,
                0,
                1,
                2,
                true,
                List.of(200, 405, 404),
                List.of(
                        WayangA2uiTransportOutcome.SUCCESS.name(),
                        WayangA2uiTransportOutcome.TRANSPORT_ERROR.name(),
                        WayangA2uiTransportOutcome.TRANSPORT_ERROR.name()),
                List.of(),
                List.of(
                        Map.of(
                                "category",
                                WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_ROUTE_MISMATCH,
                                "errorCode",
                                "method_not_allowed"),
                        Map.of(
                                "category",
                                WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_UNKNOWN_PATH,
                                "errorCode",
                                "not_found"),
                        Map.of(
                                "category",
                                WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_ROUTE_MISMATCH,
                                "errorCode",
                                "method_not_allowed")),
                Map.of("tenant", "demo"));

        WayangA2uiHttpEndpointDiagnosticSummary summary = report.summary();

        assertThat(summary.diagnosticsId()).isEqualTo("summary-report");
        assertThat(summary.passed()).isFalse();
        assertThat(summary.exitCode()).isEqualTo(WayangA2uiHttpSmokeResult.EXIT_FAILURE);
        assertThat(summary.successfulExit()).isFalse();
        assertThat(summary.exchangeCount()).isEqualTo(3);
        assertThat(summary.issueCount()).isEqualTo(3);
        assertThat(summary.issueCategories()).containsExactly(
                WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_ROUTE_MISMATCH,
                WayangA2uiHttpEndpointDiagnosticIssueCatalog.CATEGORY_UNKNOWN_PATH);
        assertThat(summary.errorCodes()).containsExactly("method_not_allowed", "not_found");
        assertThat(summary.attributes()).containsEntry("tenant", "demo");
    }

    @Test
    void roundTripsSummaryJson() {
        WayangA2uiHttpEndpointDiagnosticSummary summary = new WayangA2uiHttpEndpointDiagnosticSummary(
                "json-summary",
                true,
                WayangA2uiHttpSmokeResult.EXIT_SUCCESS,
                1,
                0,
                1,
                0,
                1,
                0,
                1,
                0,
                0,
                false,
                List.of(200),
                List.of(WayangA2uiTransportOutcome.SUCCESS.name()),
                List.of(),
                List.of(),
                Map.of("source", "json"));

        WayangA2uiHttpEndpointDiagnosticSummary decoded =
                WayangA2uiHttpEndpointDiagnosticSummary.fromJson(summary.toJson());

        assertThat(decoded).isEqualTo(summary);
        assertThat(decoded.toMap())
                .containsEntry("passed", true)
                .containsEntry("successfulExit", true);
    }

    @Test
    void decodesStringySummaryMaps() {
        WayangA2uiHttpEndpointDiagnosticSummary summary =
                WayangA2uiHttpEndpointDiagnosticSummary.fromMap(Map.ofEntries(
                        Map.entry("diagnosticsId", "external-summary"),
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
                        Map.entry("statusCodes", List.of("200", "404")),
                        Map.entry("outcomes", List.of("SUCCESS", " TRANSPORT_ERROR ", "")),
                        Map.entry("issueCategories", List.of("unknown-path", " ")),
                        Map.entry("errorCodes", List.of("not_found")),
                        Map.entry("attributes", Map.of("source", "cli"))));

        assertThat(summary.statusCodes()).containsExactly(200, 404);
        assertThat(summary.outcomes()).containsExactly("SUCCESS", "TRANSPORT_ERROR");
        assertThat(summary.issueCategories()).containsExactly("unknown-path");
        assertThat(summary.errorCodes()).containsExactly("not_found");
        assertThat(summary.attributes()).containsEntry("source", "cli");
    }

    @Test
    void emptySummaryFactoryNamesFallbackState() {
        WayangA2uiHttpEndpointDiagnosticSummary summary = WayangA2uiHttpEndpointDiagnosticSummary.empty();

        assertThat(WayangA2uiHttpEndpointDiagnosticSummary.from((WayangA2uiHttpEndpointDiagnosticReport) null))
                .isEqualTo(summary);
        assertThat(WayangA2uiHttpEndpointDiagnosticSummary.fromMap(Map.of())).isEqualTo(summary);
        assertThat(summary.diagnosticsId()).isEqualTo(WayangA2uiHttpEndpointDiagnostics.DEFAULT_ID);
        assertThat(summary.passed()).isTrue();
        assertThat(summary.successfulExit()).isTrue();
        assertThat(summary.exchangeCount()).isZero();
        assertThat(summary.issueCount()).isZero();
    }

    @Test
    void rejectsBlankOrInvalidSummaryJson() {
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticSummary.fromJson(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A2UI HTTP endpoint diagnostic summary JSON must not be blank");
        assertThatThrownBy(() -> WayangA2uiHttpEndpointDiagnosticSummary.fromJson("{not-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unable to decode A2UI HTTP endpoint diagnostic summary JSON");
    }
}
