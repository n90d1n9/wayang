package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aJsonRpcHttpRouteDescriptorTest {

    @Test
    void derivesRouteCatalogAndBindingReportMetadataFromConfig() {
        WayangA2aJsonRpcHttpConfig config = WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/a2a/rpc")
                .smokePath("/internal/a2a/smoke")
                .smokeEnabled(false)
                .routeCatalogPath("/internal/a2a/routes")
                .readinessIssueSummaryEnabled(false)
                .build();

        List<WayangA2aJsonRpcHttpRouteDescriptor> descriptors =
                WayangA2aJsonRpcHttpRouteDescriptor.fromConfig(config);

        assertThat(descriptors)
                .extracting(WayangA2aJsonRpcHttpRouteDescriptor::key)
                .containsExactly(
                        "endpoint",
                        "smoke",
                        "routeCatalog",
                        "diagnosticsReport",
                        "specComplianceReport",
                        "bindingReport",
                        "readiness",
                        "readinessIssueSummary");

        WayangA2aJsonRpcHttpRouteDescriptor endpoint = descriptors.getFirst();
        Map<String, Object> endpointReport = endpoint.toBindingReportMap();
        WayangA2aJsonRpcHttpRoute endpointRoute = endpoint.toRoute();
        assertThat(endpointReport)
                .containsEntry("path", "/a2a/rpc")
                .containsEntry("httpMethod", "POST")
                .containsEntry("allow", WayangA2aJsonRpcHttpAdapter.ALLOW_ENDPOINT)
                .containsEntry("requestMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .doesNotContainKey("enabled");
        assertThat(list(endpointReport.get("responseMediaTypes")))
                .containsExactly(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON, A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(endpointRoute)
                .returns(WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC, WayangA2aJsonRpcHttpRoute::operation)
                .returns(true, WayangA2aJsonRpcHttpRoute::enabled)
                .returns("/a2a/rpc", WayangA2aJsonRpcHttpRoute::path)
                .returns("POST", WayangA2aJsonRpcHttpRoute::httpMethod)
                .returns(true, WayangA2aJsonRpcHttpRoute::requestBodyRequired);
        assertThat(endpointRoute.responseMediaTypes())
                .containsExactly(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON, A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(endpoint.matchesPath(options("/a2a/rpc"))).isTrue();
        WayangA2aHttpResponse endpointOptions = endpoint.optionsResponse();
        assertThat(endpointOptions.headers())
                .containsEntry(WayangA2aHttpResponse.HEADER_A2A_ROUTE_OPERATION,
                        WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC)
                .containsEntry(WayangA2aHttpResponse.HEADER_ALLOW, WayangA2aJsonRpcHttpAdapter.ALLOW_ENDPOINT);
        assertThat(endpointOptions.body()).startsWith("{\"binding\":");
        assertThat(endpointOptions.body().indexOf("\"protocolVersion\""))
                .isGreaterThan(endpointOptions.body().indexOf("\"allow\""));
        assertThat(WayangA2aHttpJson.read(endpointOptions.body()))
                .containsEntry("operation", WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC)
                .containsEntry("path", "/a2a/rpc")
                .containsEntry("allow", WayangA2aJsonRpcHttpAdapter.ALLOW_ENDPOINT);

        WayangA2aJsonRpcHttpRouteDescriptor smoke = descriptors.get(1);
        Map<String, Object> smokeReport = smoke.toBindingReportMap();
        WayangA2aJsonRpcHttpRoute smokeRoute = smoke.toRoute();
        assertThat(smokeReport)
                .containsEntry("enabled", false)
                .containsEntry("path", "/internal/a2a/smoke")
                .containsEntry("httpMethod", "GET")
                .containsEntry("allow", WayangA2aJsonRpcHttpAdapter.ALLOW_SMOKE)
                .containsEntry("responseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON)
                .doesNotContainKey("requestMediaType");
        assertThat(smokeRoute)
                .returns(WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE,
                        WayangA2aJsonRpcHttpRoute::operation)
                .returns(false, WayangA2aJsonRpcHttpRoute::enabled)
                .returns("", WayangA2aJsonRpcHttpRoute::requestMediaType)
                .returns(false, WayangA2aJsonRpcHttpRoute::requestBodyRequired);
        assertThat(smoke)
                .returns("smoke", WayangA2aJsonRpcHttpRouteDescriptor::routeName)
                .returns(false, route -> route.matchesPath(options("/internal/a2a/smoke")));
    }

    @Test
    void normalizesRouteMetadataAndDefaultsAllowHeader() {
        WayangA2aJsonRpcHttpRouteDescriptor descriptor = new WayangA2aJsonRpcHttpRouteDescriptor(
                " smoke ",
                " smoke probe ",
                "JsonRpcSmoke",
                true,
                "internal/a2a/smoke",
                "get",
                "",
                " application/json ",
                List.of(
                        " application/json ",
                        "application/json",
                        A2aProtocol.EVENT_STREAM_MEDIA_TYPE),
                false);

        assertThat(descriptor)
                .returns("smoke", WayangA2aJsonRpcHttpRouteDescriptor::key)
                .returns("smoke probe", WayangA2aJsonRpcHttpRouteDescriptor::routeName)
                .returns("JsonRpcSmoke", WayangA2aJsonRpcHttpRouteDescriptor::operation)
                .returns("/internal/a2a/smoke", WayangA2aJsonRpcHttpRouteDescriptor::path)
                .returns("GET", WayangA2aJsonRpcHttpRouteDescriptor::httpMethod)
                .returns("GET, OPTIONS", WayangA2aJsonRpcHttpRouteDescriptor::allow)
                .returns("application/json", WayangA2aJsonRpcHttpRouteDescriptor::requestMediaType);
        assertThat(descriptor.responseMediaTypes())
                .containsExactly(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON, A2aProtocol.EVENT_STREAM_MEDIA_TYPE);
        assertThat(descriptor.toBindingReportMap())
                .containsEntry("enabled", true)
                .containsEntry("responseMediaType", WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON);
    }

    private static List<Object> list(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return List.copyOf((List<?>) value).stream()
                .map(Object.class::cast)
                .toList();
    }

    private static WayangA2aHttpRequest options(String path) {
        return new WayangA2aHttpRequest("OPTIONS", path, "", Map.of(), Map.of());
    }
}
