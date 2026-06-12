package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcBindingReportSectionsTest {

    @Test
    void decodesNamedBindingReportSections() {
        WayangA2aJsonRpcBindingReportSections sections =
                WayangA2aJsonRpcBindingReportSections.from(Map.of(
                        "endpoint", Map.of("path", "/a2a/rpc"),
                        "smoke", Map.of("path", "/smoke", "enabled", false),
                        "routeCatalog", Map.of("path", "/routes", "enabled", true),
                        "diagnosticsReport", Map.of("path", "/diagnostics", "enabled", true),
                        "specComplianceReport", Map.of("path", "/spec", "enabled", true),
                        "bindingReport", Map.of("path", "/binding", "enabled", true),
                        "readiness", Map.of("path", "/ready", "enabled", true),
                        "readinessIssueSummary", Map.of("path", "/ready/issues", "enabled", true)));

        assertThat(sections.endpoint().path()).isEqualTo("/a2a/rpc");
        assertThat(sections.smoke().path()).isEqualTo("/smoke");
        assertThat(sections.smoke().enabled()).isFalse();
        assertThat(sections.routeCatalog().path()).isEqualTo("/routes");
        assertThat(sections.readinessIssueSummary().enabled()).isTrue();
    }

    @Test
    void exposesRequiredSectionIssueOrder() {
        WayangA2aJsonRpcBindingReportSections sections =
                WayangA2aJsonRpcBindingReportSections.from(Map.of());

        assertThat(sections.required())
                .extracting(WayangA2aJsonRpcBindingReportSection::key)
                .containsExactly(
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_BINDING_REPORT,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_DIAGNOSTICS_REPORT,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_SPEC_COMPLIANCE_REPORT,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS_ISSUE_SUMMARY);
    }

    @Test
    void defaultsMissingSectionsToBlankDisabledValues() {
        WayangA2aJsonRpcBindingReportSections sections =
                WayangA2aJsonRpcBindingReportSections.from(Map.of());

        assertThat(sections.required()).allSatisfy(section -> {
            assertThat(section.path()).isEmpty();
            assertThat(section.enabled()).isFalse();
            assertThat(section.pathMissing()).isTrue();
        });
    }
}
