package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcReadinessProbeResultTest {

    @Test
    void reportsBindingProbeFailureWithCompactIssueShape() {
        WayangA2aJsonRpcReadinessProbeResult readiness =
                new WayangA2aJsonRpcReadinessProbeResult(
                        WayangA2aJsonRpcBindingReportProbeResult.fromMap(Map.of(
                                "statusCode", 404,
                                "routeOperation", "JsonRpc")),
                        null,
                        false,
                        null,
                        false);

        assertThat(readiness.passed()).isFalse();
        assertThat(readiness.issueCount()).isEqualTo(1);
        assertThat(readiness.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "binding_report_probe_failed")
                        .containsEntry("message", "A2A JSON-RPC binding report probe did not pass.")
                        .containsEntry("statusCode", 404)
                        .containsEntry("routeOperation", "JsonRpc")
                        .doesNotContainKeys("source", "field", "expected", "actual"));
        assertThat(WayangA2aMaps.objectList(readiness.standardReadiness().toMap().get("issues")))
                .singleElement()
                .satisfies(issue -> assertThat(issue)
                        .containsEntry("code", "binding_report_probe_failed")
                        .containsEntry("source", "http")
                        .containsEntry("statusCode", 404)
                        .containsEntry("routeOperation", "JsonRpc"));
    }

    @Test
    void omitsMethodDispatchSnapshotForLegacyBindingReportProbes() {
        WayangA2aJsonRpcReadinessProbeResult readiness =
                new WayangA2aJsonRpcReadinessProbeResult(
                        WayangA2aJsonRpcBindingReportProbeResult.from(
                                WayangA2aJsonRpcBindingReport.defaults().response()),
                        null,
                        false,
                        null,
                        false);

        assertThat(readiness.methodDispatchReported()).isFalse();
        assertThat(readiness.methodDispatchPassed()).isTrue();
        assertThat(readiness.toMap()).doesNotContainKey("methodDispatch");
    }

    @Test
    void exposesSharedReadinessContractView() {
        WayangA2aJsonRpcReadinessProbeResult readiness =
                new WayangA2aJsonRpcReadinessProbeResult(
                        WayangA2aJsonRpcBindingReportProbeResult.from(
                                WayangA2aJsonRpcBindingReport.defaults().response()),
                        null,
                        false,
                        null,
                        false);
        Map<String, Object> standard = readiness.standardReadiness().toMap();

        assertThat(standard)
                .containsEntry("readinessId", WayangA2aJsonRpcReadinessProbeResult.READINESS_ID)
                .containsEntry("ready", true)
                .containsEntry("exitCode", 0)
                .containsEntry("issueCount", 0)
                .containsEntry("issues", List.of());
        assertThat(WayangA2aMaps.objectList(standard.get("probes")))
                .hasSize(3)
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", WayangA2aJsonRpcReadinessIssueCatalog.PROBE_BINDING_REPORT)
                        .containsEntry("required", true)
                        .containsEntry("passed", true)
                        .containsEntry(
                                "attributes",
                                Map.of(
                                        "statusCode",
                                        200,
                                        "routeOperation",
                                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT)))
                .anySatisfy(probe -> assertThat(probe)
                        .containsEntry("probe", WayangA2aJsonRpcReadinessIssueCatalog.PROBE_ROUTE_CATALOG)
                        .containsEntry("required", false)
                        .containsEntry("attributes", Map.of("statusCode", 0, "routeOperation", "")));
        assertThat(map(standard.get("attributes")))
                .containsEntry("bindingReportPassed", true)
                .containsEntry("routeCatalogRequired", false)
                .containsEntry("routeCatalogPassed", true)
                .containsEntry("smokeRequired", false)
                .containsEntry("smokePassed", true);
    }

    @Test
    void summarizesReportedMethodDispatchCoverage() {
        WayangA2aJsonRpcMethodDispatchCoverage coverage =
                WayangA2aJsonRpcMethodDispatchCoverage.from(
                        List.of(
                                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                WayangA2aJsonRpcMethods.GET_TASK),
                        List.of(
                                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                "agent/unknown"));
        WayangA2aJsonRpcBindingReport report = new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                WayangA2aJsonRpcMethods.methods(),
                coverage);
        WayangA2aJsonRpcReadinessProbeResult readiness =
                new WayangA2aJsonRpcReadinessProbeResult(
                        WayangA2aJsonRpcBindingReportProbeResult.from(report.response()),
                        null,
                        false,
                        null,
                        false);
        String readinessJson = readiness.toJson();

        Map<String, Object> methodDispatch = map(readiness.toMap().get("methodDispatch"));

        assertThat(readiness.methodDispatchReported()).isTrue();
        assertThat(readiness.methodDispatchPassed()).isFalse();
        assertThat(readiness.passed()).isFalse();
        assertThat(methodDispatch)
                .containsEntry("reported", true)
                .containsEntry("complete", false)
                .containsEntry("passed", false)
                .containsEntry("registeredMethodCount", 2)
                .containsEntry("dispatchMethodCount", 2);
        assertThat(WayangA2aMaps.stringList(methodDispatch.get("missingDispatchMethods")))
                .containsExactly(WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(WayangA2aMaps.stringList(methodDispatch.get("orphanDispatchMethods")))
                .containsExactly("agent/unknown");
        assertThat(WayangA2aMaps.objectList(methodDispatch.get("methodGroups")))
                .anySatisfy(group -> assertThat(group)
                        .containsEntry("group", WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY)
                        .containsEntry("complete", false))
                .anySatisfy(group -> assertThat(group)
                        .containsEntry("group", "unassigned")
                        .containsEntry("complete", false));
        assertThat(WayangA2aJsonRpcReadinessIssueSummary.from(readiness).issues())
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("probe", "methodDispatch")
                        .containsEntry("code", "method_dispatch_coverage_incomplete"));
        assertThat(readinessJson).startsWith("{\"passed\":");
        assertThat(readinessJson.indexOf("\"methodDispatch\""))
                .isGreaterThan(readinessJson.indexOf("\"smokePassed\""));
        assertThat(readinessJson.indexOf("\"issueCount\""))
                .isGreaterThan(readinessJson.indexOf("\"methodDispatch\""));
        assertThat(readinessJson.indexOf("\"bindingReportProbe\""))
                .isGreaterThan(readinessJson.indexOf("\"issues\""));
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }
}
