package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonRpcBindingReportTest {

    @Test
    void reportsConfiguredJsonRpcHttpBindingSurface() {
        WayangA2aJsonRpcHttpConfig config = WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/a2a/rpc")
                .smokePath("/internal/a2a/smoke")
                .smokeEnabled(false)
                .build();
        WayangA2aJsonRpcBindingReport report = WayangA2aJsonRpcBindingReport.fromConfig(config);
        String reportJson = report.toJson();

        Map<String, Object> values = report.toMap();
        Map<String, Object> endpoint = map(values.get("endpoint"));
        Map<String, Object> smoke = map(values.get("smoke"));
        Map<String, Object> routeCatalog = map(values.get("routeCatalog"));
        Map<String, Object> diagnosticsReport = map(values.get("diagnosticsReport"));
        Map<String, Object> specComplianceReport = map(values.get("specComplianceReport"));
        Map<String, Object> bindingReport = map(values.get("bindingReport"));
        Map<String, Object> readiness = map(values.get("readiness"));
        Map<String, Object> readinessIssueSummary = map(values.get("readinessIssueSummary"));
        Map<String, Object> diagnosticHandlers = map(values.get("diagnosticHandlers"));
        List<Map<String, Object>> methods = maps(values.get("methods"));

        assertThat(values)
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("protocolVersion", A2aProtocol.VERSION)
                .containsEntry("methodCount", WayangA2aJsonRpcMethods.methods().size());
        assertThat(values).doesNotContainKey("methodDispatch");
        assertThat(values).doesNotContainKey("methodRegistry");
        assertThat(endpoint)
                .containsEntry("path", "/a2a/rpc")
                .containsEntry("httpMethod", "POST")
                .containsEntry("allow", "POST, OPTIONS")
                .containsEntry("requestMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(list(endpoint.get("responseMediaTypes")))
                .containsExactly(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON, A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(smoke)
                .containsEntry("enabled", false)
                .containsEntry("path", "/internal/a2a/smoke")
                .containsEntry("allow", "GET, OPTIONS");
        assertThat(routeCatalog)
                .containsEntry("enabled", true)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH)
                .containsEntry("allow", "GET, OPTIONS")
                .containsEntry("responseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(diagnosticsReport)
                .containsEntry("enabled", true)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_DIAGNOSTICS_REPORT_PATH)
                .containsEntry("allow", "GET, OPTIONS")
                .containsEntry("responseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(specComplianceReport)
                .containsEntry("enabled", true)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_SPEC_COMPLIANCE_REPORT_PATH)
                .containsEntry("allow", "GET, OPTIONS")
                .containsEntry("responseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(bindingReport)
                .containsEntry("enabled", true)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_BINDING_REPORT_PATH)
                .containsEntry("allow", "GET, OPTIONS")
                .containsEntry("responseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(readiness)
                .containsEntry("enabled", true)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_PATH)
                .containsEntry("allow", "GET, OPTIONS")
                .containsEntry("responseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(readinessIssueSummary)
                .containsEntry("enabled", true)
                .containsEntry("path", WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_ISSUE_SUMMARY_PATH)
                .containsEntry("allow", "GET, OPTIONS")
                .containsEntry("responseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(diagnosticHandlers)
                .containsEntry("complete", true)
                .containsEntry("routeKeyCount", 7)
                .containsEntry("handlerKeyCount", 7);
        assertThat(list(diagnosticHandlers.get("missingHandlerKeys"))).isEmpty();
        assertThat(list(diagnosticHandlers.get("orphanHandlerKeys"))).isEmpty();
        assertThat(list(values.get("streamingMethods")))
                .containsExactly(
                        WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE,
                        WayangA2aJsonRpcMethods.SUBSCRIBE_TO_TASK);
        assertThat(methods)
                .anySatisfy(method -> assertThat(method)
                        .containsEntry("method", WayangA2aJsonRpcMethods.SEND_MESSAGE)
                        .containsEntry("responseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                        .containsEntry("streaming", false))
                .anySatisfy(method -> assertThat(method)
                        .containsEntry("method", WayangA2aJsonRpcMethods.SEND_STREAMING_MESSAGE)
                        .containsEntry("responseMediaType", A2aProtocol.EVENT_STREAM_MEDIA_TYPE)
                        .containsEntry("streaming", true));
        assertThat(map(values.get("config")))
                .containsEntry("endpointPath", "/a2a/rpc")
                .containsEntry("smokePath", "/internal/a2a/smoke")
                .containsEntry("smokeEnabled", false)
                .containsEntry("routeCatalogPath", WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH)
                .containsEntry("routeCatalogEnabled", true)
                .containsEntry("diagnosticsReportPath", WayangA2aJsonRpcHttpAdapter.DEFAULT_DIAGNOSTICS_REPORT_PATH)
                .containsEntry("diagnosticsReportEnabled", true)
                .containsEntry("specComplianceReportPath",
                        WayangA2aJsonRpcHttpAdapter.DEFAULT_SPEC_COMPLIANCE_REPORT_PATH)
                .containsEntry("specComplianceReportEnabled", true)
                .containsEntry("bindingReportPath", WayangA2aJsonRpcHttpAdapter.DEFAULT_BINDING_REPORT_PATH)
                .containsEntry("bindingReportEnabled", true)
                .containsEntry("readinessPath", WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_PATH)
                .containsEntry("readinessEnabled", true)
                .containsEntry("readinessIssueSummaryPath",
                        WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_ISSUE_SUMMARY_PATH)
                .containsEntry("readinessIssueSummaryEnabled", true);
        assertThat(reportJson).startsWith("{\"binding\":");
        assertThat(reportJson.indexOf("\"diagnosticHandlers\""))
                .isGreaterThan(reportJson.indexOf("\"readinessIssueSummary\""));
        assertThat(reportJson.indexOf("\"methodCount\""))
                .isGreaterThan(reportJson.indexOf("\"diagnosticHandlers\""));
        assertThat(reportJson.indexOf("\"config\""))
                .isGreaterThan(reportJson.indexOf("\"streamingMethods\""));
    }

    @Test
    void includesMethodDispatchCoverageWhenProvided() {
        WayangA2aJsonRpcBindingReport report = new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                WayangA2aJsonRpcMethods.methods(),
                WayangA2aJsonRpcMethodDispatchCoverage.from(
                        WayangA2aJsonRpcMethods.methods(),
                        WayangA2aJsonRpcMethods.methods()));

        Map<String, Object> methodDispatch = map(report.toMap().get("methodDispatch"));

        assertThat(methodDispatch)
                .containsEntry("complete", true)
                .containsEntry("registeredMethodCount", WayangA2aJsonRpcMethods.methods().size())
                .containsEntry("dispatchMethodCount", WayangA2aJsonRpcMethods.methods().size());
        assertThat(WayangA2aMaps.stringList(methodDispatch.get("registeredMethods")))
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
        assertThat(WayangA2aMaps.stringList(methodDispatch.get("dispatchMethods")))
                .containsExactlyElementsOf(WayangA2aJsonRpcMethods.methods());
    }

    @Test
    void includesMethodRegistrySnapshotWhenProvided() {
        WayangA2aJsonRpcMethodDispatchTable.Handler handler =
                (request, preflight) -> WayangA2aHttpResponse.json(200, "{}");
        WayangA2aJsonRpcMethodHandlerRegistry registry = WayangA2aJsonRpcMethodHandlerRegistry.builder()
                .add(WayangA2aJsonRpcMethodHandlerGroup.of(
                        "task",
                        Map.of(WayangA2aJsonRpcMethods.GET_TASK, handler)))
                .build();
        WayangA2aJsonRpcBindingReport report = new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                WayangA2aJsonRpcMethods.methods(),
                null,
                WayangA2aJsonRpcMethodHandlerRegistrySnapshot.from(registry));

        Map<String, Object> methodRegistry = map(report.toMap().get("methodRegistry"));

        assertThat(methodRegistry)
                .containsEntry("reported", true)
                .containsEntry("groupCount", 1)
                .containsEntry("overridePolicy", "ALLOW_REPLACE")
                .containsEntry("overrideCount", 0);
        assertThat(WayangA2aMaps.objectList(methodRegistry.get("groups")))
                .singleElement()
                .satisfies(group -> assertThat(group)
                        .containsEntry("name", "task")
                        .containsEntry("methodCount", 1));
    }

    @Test
    void exposesReportThroughHttpResponseMetadata() {
        WayangA2aJsonRpcBindingReport report = WayangA2aJsonRpcBindingReport.defaults();

        WayangA2aHttpResponse response = report.response();

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.contentType()).isEqualTo(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
        assertThat(response.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcBindingReport.OPERATION_JSON_RPC_BINDING_REPORT)
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_PROTOCOL_VERSION, A2aProtocol.VERSION);
        assertThat(WayangA2aHttpJson.read(response.body()))
                .containsEntry("binding", A2aProtocol.BINDING_JSONRPC)
                .containsEntry("methodCount", WayangA2aJsonRpcMethods.methods().size());
    }

    @Test
    void exposesConfiguredRouteCatalogForFrameworkBindings() {
        WayangA2aJsonRpcHttpConfig config = WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/a2a/rpc")
                .smokeEnabled(false)
                .routeCatalogPath("/internal/a2a/routes")
                .build();
        WayangA2aJsonRpcHttpRouteCatalog catalog = WayangA2aJsonRpcHttpRouteCatalog.fromConfig(config);

        assertThat(catalog.routeCount()).isEqualTo(8);
        assertThat(catalog.enabledRouteCount()).isEqualTo(7);
        assertThat(catalog.routeForOperation(WayangA2aJsonRpcHttpRouteCatalog.OPERATION_JSON_RPC_ROUTE_CATALOG))
                .get()
                .satisfies(route -> assertThat(route.toMap())
                        .containsEntry("path", "/internal/a2a/routes")
                        .containsEntry("httpMethod", "GET")
                        .containsEntry("allow", "GET, OPTIONS"));
        assertThat(catalog.route("POST", "/a2a/rpc")).isPresent();
        assertThat(catalog.route("GET", "/a2a/rpc")).isEmpty();
        assertThat(catalog.toMap())
                .containsEntry("routeCount", 8)
                .containsEntry("enabledRouteCount", 7);
    }

    @Test
    void rejectsUnknownMethodsInCustomReports() {
        assertThatThrownBy(() -> new WayangA2aJsonRpcBindingReport(
                WayangA2aJsonRpcHttpConfig.defaults(),
                List.of(WayangA2aJsonRpcMethods.SEND_MESSAGE, "UnknownMethod")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported JSON-RPC method");
    }

    @Test
    void rejectsAmbiguousEnabledBindingReportPaths() {
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/diagnostics")
                .bindingReportPath("/diagnostics")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("binding report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .smokePath("/diagnostics")
                .bindingReportPath("/diagnostics")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("binding report path");

        WayangA2aJsonRpcHttpConfig disabledReport = WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/diagnostics")
                .bindingReportPath("/diagnostics")
                .bindingReportEnabled(false)
                .build();

        assertThat(disabledReport.toMap())
                .containsEntry("endpointPath", "/diagnostics")
                .containsEntry("bindingReportPath", "/diagnostics")
                .containsEntry("bindingReportEnabled", false);
    }

    @Test
    void rejectsAmbiguousEnabledRouteCatalogPaths() {
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/routes")
                .routeCatalogPath("/routes")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("route catalog path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .smokePath("/routes")
                .routeCatalogPath("/routes")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("route catalog path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .bindingReportPath("/routes")
                .routeCatalogPath("/routes")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("route catalog path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .readinessPath("/routes")
                .routeCatalogPath("/routes")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("route catalog path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .readinessIssueSummaryPath("/routes")
                .routeCatalogPath("/routes")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("route catalog path");

        WayangA2aJsonRpcHttpConfig disabledRouteCatalog = WayangA2aJsonRpcHttpConfig.builder()
                .readinessPath("/routes")
                .routeCatalogPath("/routes")
                .routeCatalogEnabled(false)
                .build();

        assertThat(disabledRouteCatalog.toMap())
                .containsEntry("readinessPath", "/routes")
                .containsEntry("routeCatalogPath", "/routes")
                .containsEntry("routeCatalogEnabled", false);
    }

    @Test
    void rejectsAmbiguousEnabledDiagnosticsReportPaths() {
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/diagnostics")
                .diagnosticsReportPath("/diagnostics")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diagnostics report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .smokePath("/diagnostics")
                .diagnosticsReportPath("/diagnostics")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diagnostics report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .routeCatalogPath("/diagnostics")
                .diagnosticsReportPath("/diagnostics")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diagnostics report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .bindingReportPath("/diagnostics")
                .diagnosticsReportPath("/diagnostics")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diagnostics report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .readinessPath("/diagnostics")
                .diagnosticsReportPath("/diagnostics")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diagnostics report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .readinessIssueSummaryPath("/diagnostics")
                .diagnosticsReportPath("/diagnostics")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("diagnostics report path");

        WayangA2aJsonRpcHttpConfig disabledDiagnosticsReport = WayangA2aJsonRpcHttpConfig.builder()
                .readinessPath("/diagnostics")
                .diagnosticsReportPath("/diagnostics")
                .diagnosticsReportEnabled(false)
                .build();

        assertThat(disabledDiagnosticsReport.toMap())
                .containsEntry("readinessPath", "/diagnostics")
                .containsEntry("diagnosticsReportPath", "/diagnostics")
                .containsEntry("diagnosticsReportEnabled", false);
    }

    @Test
    void rejectsAmbiguousEnabledSpecComplianceReportPaths() {
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/spec")
                .specComplianceReportPath("/spec")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spec compliance report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .smokePath("/spec")
                .specComplianceReportPath("/spec")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spec compliance report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .routeCatalogPath("/spec")
                .specComplianceReportPath("/spec")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spec compliance report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .diagnosticsReportPath("/spec")
                .specComplianceReportPath("/spec")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spec compliance report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .bindingReportPath("/spec")
                .specComplianceReportPath("/spec")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spec compliance report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .readinessPath("/spec")
                .specComplianceReportPath("/spec")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spec compliance report path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .readinessIssueSummaryPath("/spec")
                .specComplianceReportPath("/spec")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spec compliance report path");

        WayangA2aJsonRpcHttpConfig disabledSpecCompliance = WayangA2aJsonRpcHttpConfig.builder()
                .diagnosticsReportPath("/spec")
                .specComplianceReportPath("/spec")
                .specComplianceReportEnabled(false)
                .build();

        assertThat(disabledSpecCompliance.toMap())
                .containsEntry("diagnosticsReportPath", "/spec")
                .containsEntry("specComplianceReportPath", "/spec")
                .containsEntry("specComplianceReportEnabled", false);
    }

    @Test
    void rejectsAmbiguousEnabledReadinessPaths() {
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/ready")
                .readinessPath("/ready")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readiness path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .smokePath("/ready")
                .readinessPath("/ready")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readiness path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .bindingReportPath("/ready")
                .readinessPath("/ready")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readiness path");

        WayangA2aJsonRpcHttpConfig disabledReadiness = WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/ready")
                .readinessPath("/ready")
                .readinessEnabled(false)
                .build();

        assertThat(disabledReadiness.toMap())
                .containsEntry("endpointPath", "/ready")
                .containsEntry("readinessPath", "/ready")
                .containsEntry("readinessEnabled", false);
    }

    @Test
    void rejectsAmbiguousEnabledReadinessIssueSummaryPaths() {
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/ready/issues")
                .readinessIssueSummaryPath("/ready/issues")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readiness issue summary path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .smokePath("/ready/issues")
                .readinessIssueSummaryPath("/ready/issues")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readiness issue summary path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .bindingReportPath("/ready/issues")
                .readinessIssueSummaryPath("/ready/issues")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readiness issue summary path");
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpConfig.builder()
                .readinessPath("/ready/issues")
                .readinessIssueSummaryPath("/ready/issues")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("readiness issue summary path");

        WayangA2aJsonRpcHttpConfig disabledIssueSummary = WayangA2aJsonRpcHttpConfig.builder()
                .readinessPath("/ready/issues")
                .readinessIssueSummaryPath("/ready/issues")
                .readinessIssueSummaryEnabled(false)
                .build();

        assertThat(disabledIssueSummary.toMap())
                .containsEntry("readinessPath", "/ready/issues")
                .containsEntry("readinessIssueSummaryPath", "/ready/issues")
                .containsEntry("readinessIssueSummaryEnabled", false);
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return WayangA2aMaps.copyMap((Map<?, ?>) value);
    }

    private static List<Object> list(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return List.copyOf((List<?>) value);
    }

    private static List<Map<String, Object>> maps(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return ((List<?>) value).stream()
                .map(entry -> {
                    assertThat(entry).isInstanceOf(Map.class);
                    return WayangA2aMaps.copyMap((Map<?, ?>) entry);
                })
                .toList();
    }
}
