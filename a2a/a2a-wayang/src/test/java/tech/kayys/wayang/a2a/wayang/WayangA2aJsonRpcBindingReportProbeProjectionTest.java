package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcBindingReportProbeProjectionTest {

    @Test
    void keepsOrderedProbeEnvelopeWithoutMethodDispatch() {
        WayangA2aJsonRpcBindingReportProbeResult probe = legacyProbe();

        Map<String, Object> values = WayangA2aJsonRpcBindingReportProbeProjection.probe(probe);

        assertThat(values.keySet()).containsExactly(
                "statusCode",
                "httpSuccessful",
                "routeOperation",
                "protocolVersion",
                "contentType",
                "allow",
                "bindingReportRoute",
                "jsonContent",
                "complete",
                "passed",
                "issueCount",
                "issues",
                "methodCount",
                "streamingMethodCount",
                "endpointPath",
                "smokePath",
                "smokeEnabled",
                "routeCatalogPath",
                "routeCatalogEnabled",
                "diagnosticsReportPath",
                "diagnosticsReportEnabled",
                "specComplianceReportPath",
                "specComplianceReportEnabled",
                "bindingReportPath",
                "bindingReportEnabled",
                "readinessPath",
                "readinessEnabled",
                "readinessIssueSummaryPath",
                "readinessIssueSummaryEnabled",
                "diagnosticHandlersComplete",
                "diagnosticRouteKeyCount",
                "diagnosticHandlerKeyCount",
                "diagnosticRouteKeys",
                "diagnosticHandlerKeys",
                "missingDiagnosticHandlerKeys",
                "orphanDiagnosticHandlerKeys",
                "streamingMethods",
                "body",
                "headers");
        assertThat(values)
                .containsEntry("statusCode", 200)
                .containsEntry("httpSuccessful", true)
                .containsEntry("bindingReportRoute", true)
                .containsEntry("jsonContent", true)
                .containsEntry("complete", true)
                .containsEntry("passed", true)
                .containsEntry("methodCount", WayangA2aJsonRpcMethods.methods().size())
                .containsEntry("streamingMethodCount", 2)
                .doesNotContainKey("methodDispatchReported");
    }

    @Test
    void insertsReportedMethodDispatchBeforeStreamingMethods() {
        WayangA2aJsonRpcBindingReportProbeResult probe = probeWithCompleteMethodDispatch();

        Map<String, Object> values = WayangA2aJsonRpcBindingReportProbeProjection.probe(probe);

        assertThat(values.keySet()).containsExactly(
                "statusCode",
                "httpSuccessful",
                "routeOperation",
                "protocolVersion",
                "contentType",
                "allow",
                "bindingReportRoute",
                "jsonContent",
                "complete",
                "passed",
                "issueCount",
                "issues",
                "methodCount",
                "streamingMethodCount",
                "endpointPath",
                "smokePath",
                "smokeEnabled",
                "routeCatalogPath",
                "routeCatalogEnabled",
                "diagnosticsReportPath",
                "diagnosticsReportEnabled",
                "specComplianceReportPath",
                "specComplianceReportEnabled",
                "bindingReportPath",
                "bindingReportEnabled",
                "readinessPath",
                "readinessEnabled",
                "readinessIssueSummaryPath",
                "readinessIssueSummaryEnabled",
                "diagnosticHandlersComplete",
                "diagnosticRouteKeyCount",
                "diagnosticHandlerKeyCount",
                "diagnosticRouteKeys",
                "diagnosticHandlerKeys",
                "missingDiagnosticHandlerKeys",
                "orphanDiagnosticHandlerKeys",
                "methodDispatchReported",
                "methodDispatchComplete",
                "registeredMethodCount",
                "dispatchMethodCount",
                "registeredMethods",
                "dispatchMethods",
                "missingDispatchMethods",
                "orphanDispatchMethods",
                "methodDispatchGroups",
                "streamingMethods",
                "body",
                "headers");
        assertThat(values)
                .containsEntry("methodDispatchReported", true)
                .containsEntry("methodDispatchComplete", true)
                .containsEntry("registeredMethodCount", WayangA2aJsonRpcMethods.methods().size())
                .containsEntry("dispatchMethodCount", WayangA2aJsonRpcMethods.methods().size());
        assertThat(WayangA2aMaps.stringList(values.get("registeredMethods")))
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
    }

    @Test
    void parsesProbeMapsAndDerivesMethodDispatchGroups() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("statusCode", "200");
        values.put("httpSuccessful", "true");
        values.put("routeOperation", " " + WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT + " ");
        values.put("protocolVersion", " 0.3.0 ");
        values.put("contentType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        values.put("allow", " GET ");
        values.put("methodCount", "2");
        values.put("streamingMethods", List.of(WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE));
        values.put("endpointPath", " /a2a ");
        values.put("smokePath", " /smoke ");
        values.put("smokeEnabled", "true");
        values.put("routeCatalogPath", " /routes ");
        values.put("routeCatalogEnabled", true);
        values.put("diagnosticsReportPath", " /diagnostics ");
        values.put("diagnosticsReportEnabled", true);
        values.put("specComplianceReportPath", " /spec ");
        values.put("specComplianceReportEnabled", true);
        values.put("bindingReportPath", " /binding ");
        values.put("bindingReportEnabled", true);
        values.put("readinessPath", " /ready ");
        values.put("readinessEnabled", true);
        values.put("readinessIssueSummaryPath", " /ready/issues ");
        values.put("readinessIssueSummaryEnabled", true);
        values.put("diagnosticHandlersComplete", true);
        values.put("diagnosticRouteKeyCount", "1");
        values.put("diagnosticHandlerKeyCount", "1");
        values.put("diagnosticRouteKeys", List.of("bindingReport"));
        values.put("diagnosticHandlerKeys", List.of("bindingReport"));
        values.put("methodDispatchReported", true);
        values.put("methodDispatchComplete", false);
        values.put("registeredMethods", List.of(
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                WayangA2aJsonRpcMethods.GET_TASK));
        values.put("dispatchMethods", List.of(
                WayangA2aJsonRpcMethods.SEND_MESSAGE,
                "CustomMethod"));
        values.put("missingDispatchMethods", List.of(WayangA2aJsonRpcMethods.GET_TASK));
        values.put("orphanDispatchMethods", List.of("CustomMethod"));
        values.put("issueCount", "0");
        values.put("issues", List.of(Map.of("code", "method_dispatch_coverage_incomplete")));
        values.put("body", Map.of("methodCount", 2));
        values.put("headers", Map.of(WayangA2aHttpResponse.HEADER_ALLOW, "GET"));

        WayangA2aJsonRpcBindingReportProbeResult probe =
                WayangA2aJsonRpcBindingReportProbeProjection.fromMap(values);

        assertThat(probe.statusCode()).isEqualTo(200);
        assertThat(probe.routeOperation()).isEqualTo(WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT);
        assertThat(probe.protocolVersion()).isEqualTo("0.3.0");
        assertThat(probe.endpointPath()).isEqualTo("/a2a");
        assertThat(probe.methodDispatchReported()).isTrue();
        assertThat(probe.methodDispatchComplete()).isFalse();
        assertThat(probe.issueCount()).isEqualTo(1);
        assertThat(probe.methodDispatchGroups())
                .anySatisfy(group -> assertThat(group)
                        .containsEntry("group", WayangA2aJsonRpcMethods.METHOD_GROUP_TASK_QUERY)
                        .containsEntry("complete", false))
                .anySatisfy(group -> assertThat(group)
                        .containsEntry("group", "unassigned")
                        .containsEntry("complete", false));
        assertThat(probe.headers()).containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, "GET");
    }

    @Test
    void projectsReportedMethodRegistryBeforeStreamingMethods() {
        WayangA2aJsonRpcBindingReport report = new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                WayangA2aJsonRpcMethods.methods(),
                null,
                WayangA2aJsonRpcMethodRegistryTestFixtures.taskRegistrySnapshot());
        WayangA2aJsonRpcBindingReportProbeResult probe =
                WayangA2aJsonRpcBindingReportProbeResult.from(report.response()
                        .withHeaders(Map.of(WayangA2aHttpResponse.HEADER_ALLOW,
                                WayangA2aJsonRpcHttpAdapter.ALLOW_BINDING_REPORT)));

        Map<String, Object> values = WayangA2aJsonRpcBindingReportProbeProjection.probe(probe);

        assertThat(values.keySet()).containsSubsequence(
                "methodRegistryReported",
                "methodRegistryGroupCount",
                "methodRegistryGroups",
                "methodRegistryProviderCount",
                "methodRegistryProviderIds",
                "methodRegistryModuleIds",
                "methodRegistryCapabilityTags",
                "methodRegistryOverridePolicy",
                "methodRegistryOverrideCount",
                "methodRegistryOverrides",
                "streamingMethods");
        assertThat(values)
                .containsEntry("methodRegistryReported", true)
                .containsEntry("methodRegistryGroupCount", 1)
                .containsEntry("methodRegistryProviderCount", 1)
                .containsEntry("methodRegistryOverridePolicy",
                        WayangA2aJsonRpcMethodRegistryTestFixtures.OVERRIDE_POLICY_ALLOW_REPLACE)
                .containsEntry("methodRegistryOverrideCount", 0);
        assertThat(WayangA2aMaps.stringList(values.get("methodRegistryProviderIds")))
                .containsExactly(WayangA2aJsonRpcMethodRegistryTestFixtures.PROVIDER_TASK);
        assertThat(WayangA2aMaps.stringList(values.get("methodRegistryModuleIds")))
                .containsExactly(WayangA2aJsonRpcMethodRegistryTestFixtures.MODULE_TEST);
        assertThat(WayangA2aMaps.stringList(values.get("methodRegistryCapabilityTags")))
                .containsExactly("test", "task");
    }

    @Test
    void parsesFlattenedMethodRegistryProbeMaps() {
        Map<String, Object> values =
                WayangA2aJsonRpcMethodRegistryTestFixtures.flattenedTaskRegistryProbeMapWithPaddedPolicy();

        WayangA2aJsonRpcBindingReportProbeResult probe =
                WayangA2aJsonRpcBindingReportProbeProjection.fromMap(values);

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
        assertThat(probe.methodRegistryGroups())
                .singleElement()
                .satisfies(group -> assertThat(group)
                        .containsEntry("name", WayangA2aJsonRpcMethodRegistryTestFixtures.GROUP_TASK)
                        .containsEntry("methodCount", 1));
    }

    @Test
    void recordDelegatesToProjectionForJsonRoundTrip() {
        WayangA2aJsonRpcBindingReportProbeResult probe = probeWithCompleteMethodDispatch();

        assertThat(probe.toMap()).isEqualTo(WayangA2aJsonRpcBindingReportProbeProjection.probe(probe));
        assertThat(WayangA2aJsonRpcBindingReportProbeResult.fromJson(probe.toJson()).toMap())
                .isEqualTo(probe.toMap());
    }

    private static WayangA2aJsonRpcBindingReportProbeResult legacyProbe() {
        return WayangA2aJsonRpcBindingReportProbeResult.from(legacyBindingReportResponse());
    }

    private static WayangA2aJsonRpcBindingReportProbeResult probeWithCompleteMethodDispatch() {
        WayangA2aJsonRpcBindingReport report = new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                WayangA2aJsonRpcMethods.methods(),
                WayangA2aJsonRpcMethodDispatchCoverage.from(
                        WayangA2aJsonRpcMethods.methods(),
                        WayangA2aJsonRpcMethods.methods()));
        return WayangA2aJsonRpcBindingReportProbeResult.from(report.response()
                .withHeaders(Map.of(WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_BINDING_REPORT)));
    }

    private static WayangA2aHttpResponse legacyBindingReportResponse() {
        return WayangA2aJsonRpcBindingReport.defaults().response()
                .withHeaders(Map.of(WayangA2aHttpResponse.HEADER_ALLOW,
                        WayangA2aJsonRpcHttpAdapter.ALLOW_BINDING_REPORT));
    }
}
