package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcBindingReportSectionTest {

    @Test
    void decodesPathAndEnabledStateFromBindingReportBody() {
        WayangA2aJsonRpcBindingReportSection section =
                WayangA2aJsonRpcBindingReportSection.from(Map.of(
                        "smoke", Map.of(
                                "path", " /internal/a2a/smoke ",
                                "enabled", "true")),
                        "smoke");

        assertThat(section.key()).isEqualTo("smoke");
        assertThat(section.path()).isEqualTo("/internal/a2a/smoke");
        assertThat(section.enabled()).isTrue();
    }

    @Test
    void defaultsMissingSectionToBlankDisabledSurface() {
        WayangA2aJsonRpcBindingReportSection section =
                WayangA2aJsonRpcBindingReportSection.from(Map.of(), "routeCatalog");

        assertThat(section.key()).isEqualTo("routeCatalog");
        assertThat(section.path()).isEmpty();
        assertThat(section.enabled()).isFalse();
    }

    @Test
    void treatsEndpointAsPathOnlySection() {
        WayangA2aJsonRpcBindingReportSection section =
                WayangA2aJsonRpcBindingReportSection.fromMap(
                        "endpoint",
                        Map.of("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_ENDPOINT_PATH));

        assertThat(section.path()).isEqualTo(WayangA2aJsonRpcHttpAdapter.DEFAULT_ENDPOINT_PATH);
        assertThat(section.enabled()).isFalse();
    }

    @Test
    void buildsMissingPathIssueForSectionKey() {
        WayangA2aJsonRpcBindingReportSection section =
                WayangA2aJsonRpcBindingReportSection.fromMap(
                        "readinessIssueSummary",
                        Map.of("path", " "));

        assertThat(section.pathMissing()).isTrue();
        assertThat(section.missingPathIssue())
                .containsEntry("source", "bindingReport")
                .containsEntry("code", "readiness_issue_summary_path_missing")
                .containsEntry("field", "readinessIssueSummaryPath")
                .containsEntry("expected", "non-blank")
                .containsEntry("actual", "")
                .containsEntry(
                        "message",
                        "A2A JSON-RPC binding report did not expose a readiness issue summary path.");
    }

    @Test
    void usesEndpointArticleInMissingPathIssue() {
        WayangA2aJsonRpcBindingReportSection section =
                WayangA2aJsonRpcBindingReportSection.fromMap("endpoint", Map.of());

        assertThat(section.missingPathIssue())
                .containsEntry("message", "A2A JSON-RPC binding report did not expose an endpoint path.");
    }
}
