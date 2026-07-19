package tech.kayys.wayang.client;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.readiness.WayangReadinessReport;
import tech.kayys.wayang.readiness.WayangReadinessReports;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangReadinessReportTest {

    @Test
    void normalizesSharedReadinessEnvelope() {
        WayangReadinessReport report = WayangReadinessReport.from(
                " adapter.readiness ",
                false,
                1,
                0,
                List.of(Map.of("probe", "binding", "passed", false)),
                List.of(Map.of("code", "binding_failed")),
                Map.of("protocol", "A2A"));

        assertThat(report.readinessId()).isEqualTo("adapter.readiness");
        assertThat(report.ready()).isFalse();
        assertThat(report.exitCode()).isEqualTo(1);
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.toMap())
                .containsEntry("readinessId", "adapter.readiness")
                .containsEntry("ready", false)
                .containsEntry("exitCode", 1)
                .containsEntry("issueCount", 1)
                .containsEntry("attributes", Map.of("protocol", "A2A"));
    }

    @Test
    void preservesWireFriendlyFieldOrder() {
        WayangReadinessReport report = WayangReadinessReport.from(
                "adapter.readiness",
                true,
                0,
                0,
                List.of(),
                List.of(),
                Map.of());

        assertThat(report.toMap().keySet())
                .containsExactly(
                        "readinessId",
                        "ready",
                        "exitCode",
                        "issueCount",
                        "probes",
                        "issues",
                        "attributes");
    }

    @Test
    void preservesBlankProbeFields() {
        WayangReadinessReport report = WayangReadinessReport.from(
                "adapter.readiness",
                true,
                0,
                0,
                List.of(Map.of("probe", "smoke", "routeOperation", "")),
                List.of(),
                Map.of("binding", "JSONRPC"));

        assertThat(report.probes())
                .singleElement()
                .satisfies(probe -> assertThat(probe).containsEntry("routeOperation", ""));
        assertThat(report.toMap())
                .containsEntry("probes", List.of(Map.of(
                        "probe", "smoke",
                        "routeOperation", "")));
    }

    @Test
    void redactsSecretLikeStringsAcrossReadinessReportMaps() {
        WayangReadinessReport report = WayangReadinessReport.from(
                "adapter.readiness",
                false,
                1,
                1,
                List.of(Map.of(
                        "probe", "database",
                        "attributes", Map.of(
                                "url", "jdbc:postgresql://ops:inline-password@localhost/wayang"))),
                List.of(Map.of(
                        "code", "db_failed",
                        "message", "password=inline-secret token=inline-token")),
                Map.of("nested", List.of(Map.of(
                        "credentials", "accessKeyId=inline-access secretAccessKey=inline-secret"))));

        String output = report.toMap().toString();

        assertThat(output)
                .contains("<redacted>")
                .doesNotContain("inline-password")
                .doesNotContain("inline-secret")
                .doesNotContain("inline-token")
                .doesNotContain("inline-access");
    }

    @Test
    void buildsSharedProbeFragments() {
        Map<String, Object> probe = WayangReadinessReports.probe(
                " smoke ",
                true,
                false,
                -5,
                Map.of("routeOperation", ""));

        assertThat(probe.keySet())
                .containsExactly("probe", "required", "passed", "issueCount", "attributes");
        assertThat(probe)
                .containsEntry("probe", "smoke")
                .containsEntry("required", true)
                .containsEntry("passed", false)
                .containsEntry("issueCount", 0);
        assertThat(probe.get("attributes"))
                .isEqualTo(Map.of("routeOperation", ""));
    }

    @Test
    void buildsSharedIssueFragmentsWithoutOverwritingReservedFields() {
        Map<String, Object> issue = WayangReadinessReports.issue(
                " connector_failed ",
                " connector ",
                "",
                Map.of(
                        "code",
                        "override",
                        "source",
                        "override",
                        "scenarioId",
                        "smoke"));

        assertThat(issue.keySet())
                .containsExactly("code", "source", "message", "scenarioId");
        assertThat(issue)
                .containsEntry("code", "connector_failed")
                .containsEntry("source", "connector")
                .containsEntry("message", "connector failed")
                .containsEntry("scenarioId", "smoke");
    }

    @Test
    void mapsReadyStateToStandardExitCodes() {
        assertThat(WayangReadinessReports.exitCode(true)).isEqualTo(0);
        assertThat(WayangReadinessReports.exitCode(false)).isEqualTo(1);
    }

    @Test
    void aggregatesComponentReadinessReports() {
        WayangReadinessReport runtime = WayangReadinessReport.from(
                "runtime.readiness",
                true,
                0,
                0,
                List.of(),
                List.of(),
                Map.of("component", "runtime"));
        WayangReadinessReport a2a = WayangReadinessReport.from(
                "a2a.readiness",
                false,
                1,
                2,
                List.of(),
                List.of(WayangReadinessReports.issue(
                        "binding_failed",
                        "http",
                        "Binding failed.")),
                Map.of("component", "a2a"));

        WayangReadinessReport aggregate =
                WayangReadinessReports.aggregate("platform.readiness", runtime, a2a);

        assertThat(aggregate.toMap())
                .containsEntry("readinessId", "platform.readiness")
                .containsEntry("ready", false)
                .containsEntry("exitCode", 1)
                .containsEntry("issueCount", 2);
        assertThat(aggregate.probes())
                .hasSize(2)
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "runtime.readiness")
                        .containsEntry("required", true)
                        .containsEntry("passed", true)
                        .containsEntry("issueCount", 0))
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", "a2a.readiness")
                        .containsEntry("required", true)
                        .containsEntry("passed", false)
                        .containsEntry("issueCount", 2));
        assertThat(aggregate.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "binding_failed")
                        .containsEntry("source", "http")
                        .containsEntry("componentReadinessId", "a2a.readiness"));
        assertThat(aggregate.attributes())
                .containsEntry("componentCount", 2)
                .containsEntry("readyComponentCount", 1)
                .containsEntry("failedComponentCount", 1)
                .containsEntry("componentReadinessIds", List.of("runtime.readiness", "a2a.readiness"))
                .containsEntry("failedReadinessIds", List.of("a2a.readiness"));
        assertThat(objectList(aggregate.attributes().get("componentSummaries")))
                .anySatisfy(summary -> assertThat(summary)
                        .containsEntry("readinessId", "runtime.readiness")
                        .containsEntry("ready", true)
                        .containsEntry("issueCount", 0))
                .anySatisfy(summary -> assertThat(summary)
                        .containsEntry("readinessId", "a2a.readiness")
                        .containsEntry("ready", false)
                        .containsEntry("issueCount", 2));
    }

    @Test
    void aggregatesEmptyReadinessListAsEmptyReadyPortfolio() {
        WayangReadinessReport aggregate = WayangReadinessReports.aggregate(List.of());

        assertThat(aggregate.toMap())
                .containsEntry("readinessId", WayangReadinessReports.AGGREGATE_READINESS_ID)
                .containsEntry("ready", true)
                .containsEntry("exitCode", 0)
                .containsEntry("issueCount", 0)
                .containsEntry("probes", List.of())
                .containsEntry("issues", List.of());
        assertThat(aggregate.attributes())
                .containsEntry("componentCount", 0)
                .containsEntry("readyComponentCount", 0)
                .containsEntry("failedComponentCount", 0)
                .containsEntry("componentReadinessIds", List.of())
                .containsEntry("failedReadinessIds", List.of())
                .containsEntry("componentSummaries", List.of());
    }

    @Test
    void aggregateAttributesCanCarryContextWithoutOverridingComponentStats() {
        WayangReadinessReport aggregate = WayangReadinessReports.aggregate(
                "platform.readiness",
                List.of(WayangReadinessReport.from(
                        "runtime.readiness",
                        true,
                        0,
                        0,
                        List.of(),
                        List.of(),
                        Map.of())),
                Map.of(
                        "readinessProfileId", "minimal",
                        "componentCount", 99));

        assertThat(aggregate.attributes())
                .containsEntry("readinessProfileId", "minimal")
                .containsEntry("componentCount", 1)
                .containsEntry("readyComponentCount", 1)
                .containsEntry("componentReadinessIds", List.of("runtime.readiness"));
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> objectList(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<Map<String, Object>>) value;
    }

    private static WayangReadinessReport readyReport(String readinessId) {
        return WayangReadinessReport.from(
                readinessId,
                true,
                0,
                0,
                List.of(),
                List.of(),
                Map.of());
    }
}
