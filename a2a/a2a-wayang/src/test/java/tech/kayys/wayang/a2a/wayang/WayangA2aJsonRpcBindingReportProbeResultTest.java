package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcBindingReportProbeResultTest {

    @Test
    void missingMethodDispatchCoverageDoesNotFailLegacyBindingReportProbe() {
        WayangA2aJsonRpcBindingReportProbeResult probe =
                WayangA2aJsonRpcBindingReportProbeResult.from(legacyBindingReportResponse());

        assertThat(probe.methodDispatchReported()).isFalse();
        assertThat(probe.methodDispatchComplete()).isFalse();
        assertThat(probe.methodRegistryReported()).isFalse();
        assertThat(probe.complete()).isTrue();
        assertThat(probe.passed()).isTrue();
        assertThat(probe.issueCount()).isZero();
    }

    @Test
    void reportedIncompleteMethodDispatchCoverageCreatesProbeIssue() {
        Map<String, Object> body = new LinkedHashMap<>(
                WayangA2aJsonRpcBindingReport.defaults().toMap());
        body.put("methodDispatch", WayangA2aJsonRpcMethodDispatchCoverage.from(
                List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, WayangA2aJsonRpcMethods.GET_TASK),
                List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, "CustomMethod")).toMap());

        WayangA2aJsonRpcBindingReportProbeResult probe =
                WayangA2aJsonRpcBindingReportProbeResult.from(bindingReportResponse(body));

        assertThat(probe.methodDispatchReported()).isTrue();
        assertThat(probe.methodDispatchComplete()).isFalse();
        assertThat(probe.complete()).isFalse();
        assertThat(probe.passed()).isFalse();
        assertThat(probe.missingDispatchMethods()).containsExactly(WayangA2aJsonRpcMethods.GET_TASK);
        assertThat(probe.orphanDispatchMethods()).containsExactly("CustomMethod");
        assertThat(probe.methodDispatchGroups())
                .anySatisfy(group -> assertThat(group)
                        .containsEntry("group", WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY)
                        .containsEntry("complete", false))
                .anySatisfy(group -> assertThat(group)
                        .containsEntry("group", "unassigned")
                        .containsEntry("complete", false));
        assertThat(probe.issues())
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("code", "method_dispatch_coverage_incomplete")
                        .containsEntry("field", "methodDispatch.complete"))
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("code", "method_dispatch_group_coverage_incomplete")
                        .containsEntry("field", "methodDispatch.methodGroups.taskQuery.complete"))
                .anySatisfy(issue -> assertThat(issue)
                        .containsEntry("code", "method_dispatch_group_coverage_incomplete")
                        .containsEntry("field", "methodDispatch.methodGroups.unassigned.complete"));
    }

    @Test
    void decodesMethodRegistrySnapshotFromBindingReportBody() {
        Map<String, Object> body = new LinkedHashMap<>(
                WayangA2aJsonRpcBindingReport.defaults().toMap());
        body.put("methodRegistry", WayangA2aJsonRpcMethodRegistryTestFixtures.taskRegistryMapWithOverride());

        WayangA2aJsonRpcBindingReportProbeResult probe =
                WayangA2aJsonRpcBindingReportProbeResult.from(bindingReportResponse(body));

        assertThat(probe.methodRegistryReported()).isTrue();
        assertThat(probe.methodRegistryGroupCount()).isEqualTo(1);
        assertThat(probe.methodRegistryProviderCount()).isEqualTo(1);
        assertThat(probe.methodRegistryProviderIds())
                .containsExactly(WayangA2aJsonRpcMethodRegistryTestFixtures.PROVIDER_TASK);
        assertThat(probe.methodRegistryModuleIds())
                .containsExactly(WayangA2aJsonRpcMethodRegistryTestFixtures.MODULE_TEST);
        assertThat(probe.methodRegistryCapabilityTags()).containsExactly("test", "task");
        assertThat(probe.methodRegistryOverridePolicy())
                .isEqualTo(WayangA2aJsonRpcMethodRegistryTestFixtures.OVERRIDE_POLICY_ALLOW_REPLACE);
        assertThat(probe.methodRegistryOverrideCount()).isEqualTo(1);
        assertThat(probe.methodRegistryGroups())
                .singleElement()
                .satisfies(group -> assertThat(group)
                        .containsEntry("name", WayangA2aJsonRpcMethodRegistryTestFixtures.GROUP_TASK)
                        .containsEntry("methodCount", 1));
        assertThat(probe.methodRegistryOverrides())
                .singleElement()
                .satisfies(override -> assertThat(override)
                        .containsEntry("method", WayangA2aJsonRpcMethods.GET_TASK)
                        .containsEntry("originalGroup", WayangA2aJsonRpcMethodRegistryTestFixtures.GROUP_TASK)
                        .containsEntry("replacementGroup",
                                WayangA2aJsonRpcMethodRegistryTestFixtures.GROUP_EXTENSION));
    }

    @Test
    void derivesMethodDispatchGroupsFromFlattenedProbeMaps() {
        WayangA2aJsonRpcBindingReportProbeResult probe =
                WayangA2aJsonRpcBindingReportProbeResult.fromMap(Map.of(
                        "methodDispatchReported", true,
                        "methodDispatchComplete", false,
                        "registeredMethods", List.of(
                                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                WayangA2aJsonRpcMethods.GET_TASK),
                        "dispatchMethods", List.of(
                                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                                "CustomMethod"),
                        "missingDispatchMethods", List.of(WayangA2aJsonRpcMethods.GET_TASK),
                        "orphanDispatchMethods", List.of("CustomMethod")));

        assertThat(probe.methodDispatchGroups())
                .anySatisfy(group -> assertThat(group)
                        .containsEntry("group", WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY)
                        .containsEntry("complete", false))
                .anySatisfy(group -> assertThat(group)
                        .containsEntry("group", "unassigned")
                        .containsEntry("complete", false));
    }

    private static WayangA2aHttpResponse legacyBindingReportResponse() {
        return WayangA2aJsonRpcBindingReport.defaults().response()
                .withHeaders(Map.of(WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_BINDING_REPORT));
    }

    private static WayangA2aHttpResponse bindingReportResponse(Map<String, Object> body) {
        return new WayangA2aHttpResponse(
                200,
                WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                WayangA2aHttpJson.write(body),
                Map.of(
                        WayangA2aHttpResponse.HEADER_CONTENT_TYPE,
                        WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                        WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT,
                        WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION,
                        "1.0",
                        WayangA2aHttpResponse.HEADER_A2A_VERSION,
                        "1.0",
                        WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_BINDING_REPORT));
    }
}
