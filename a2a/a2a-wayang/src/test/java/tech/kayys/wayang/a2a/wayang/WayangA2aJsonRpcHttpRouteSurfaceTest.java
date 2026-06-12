package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aJsonRpcHttpRouteSurfaceTest {

    @Test
    void exposesCanonicalPublicationOrderAndDescriptorFields() {
        assertThat(WayangA2aJsonRpcHttpRouteSurface.ordered())
                .extracting(WayangA2aJsonRpcHttpRouteSurface::key)
                .containsExactly(
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_DIAGNOSTICS_REPORT,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_SPEC_COMPLIANCE_REPORT,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_BINDING_REPORT,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS,
                        WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS_ISSUE_SUMMARY);
        assertThat(WayangA2aJsonRpcHttpRouteSurface.ordered())
                .extracting(WayangA2aJsonRpcHttpRouteSurface::descriptorField)
                .containsExactly(
                        "endpointDescriptor",
                        "smokeDescriptor",
                        "routeCatalogDescriptor",
                        "diagnosticsReportDescriptor",
                        "specComplianceReportDescriptor",
                        "bindingReportDescriptor",
                        "readinessDescriptor",
                        "readinessIssueSummaryDescriptor");
        assertThat(WayangA2aJsonRpcHttpRouteSurface.ordered())
                .extracting(WayangA2aJsonRpcHttpRouteSurface::pathField)
                .containsExactly(
                        "endpointPath",
                        "smokePath",
                        "routeCatalogPath",
                        "diagnosticsReportPath",
                        "specComplianceReportPath",
                        "bindingReportPath",
                        "readinessPath",
                        "readinessIssueSummaryPath");
        assertThat(WayangA2aJsonRpcHttpRouteSurface.ordered())
                .extracting(WayangA2aJsonRpcHttpRouteSurface::enabledField)
                .containsExactly(
                        "",
                        "smokeEnabled",
                        "routeCatalogEnabled",
                        "diagnosticsReportEnabled",
                        "specComplianceReportEnabled",
                        "bindingReportEnabled",
                        "readinessEnabled",
                        "readinessIssueSummaryEnabled");
    }

    @Test
    void exposesBindingReportRequiredOrder() {
        assertThat(WayangA2aJsonRpcHttpRouteSurface.bindingReportRequiredOrder())
                .extracting(WayangA2aJsonRpcHttpRouteSurface::key)
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
    void buildsDescriptorsFromConfig() {
        WayangA2aJsonRpcHttpConfig config = WayangA2aJsonRpcHttpConfig.builder()
                .endpointPath("/a2a/rpc")
                .smokePath("/internal/a2a/smoke")
                .smokeEnabled(false)
                .routeCatalogPath("/internal/a2a/routes")
                .build();

        WayangA2aJsonRpcHttpRouteDescriptor endpoint =
                WayangA2aJsonRpcHttpRouteSurface.endpointSurface().descriptor(config);
        WayangA2aJsonRpcHttpRouteDescriptor smoke =
                WayangA2aJsonRpcHttpRouteSurface.smokeSurface().descriptor(config);

        assertThat(endpoint)
                .returns("endpoint", WayangA2aJsonRpcHttpRouteDescriptor::routeName)
                .returns(WayangA2aJsonRpcHttpAdapter.OPERATION_JSON_RPC,
                        WayangA2aJsonRpcHttpRouteDescriptor::operation)
                .returns(true, WayangA2aJsonRpcHttpRouteDescriptor::enabled)
                .returns("/a2a/rpc", WayangA2aJsonRpcHttpRouteDescriptor::path)
                .returns("POST", WayangA2aJsonRpcHttpRouteDescriptor::httpMethod)
                .returns(true, WayangA2aJsonRpcHttpRouteDescriptor::requestBodyRequired);
        assertThat(smoke)
                .returns("smoke", WayangA2aJsonRpcHttpRouteDescriptor::routeName)
                .returns(WayangA2aJsonRpcSmokeProbeResult.OPERATION_JSON_RPC_SMOKE,
                        WayangA2aJsonRpcHttpRouteDescriptor::operation)
                .returns(false, WayangA2aJsonRpcHttpRouteDescriptor::enabled)
                .returns("/internal/a2a/smoke", WayangA2aJsonRpcHttpRouteDescriptor::path)
                .returns("GET", WayangA2aJsonRpcHttpRouteDescriptor::httpMethod)
                .returns(false, WayangA2aJsonRpcHttpRouteDescriptor::requestBodyRequired);
    }

    @Test
    void rejectsDuplicateEnabledConfigPathsWithCanonicalRouteNames() {
        assertThatThrownBy(() -> WayangA2aJsonRpcHttpRouteSurface.requireDistinctConfigPaths(configValues(
                "/a2a/rpc",
                "/internal/a2a/smoke",
                true,
                "/internal/a2a/routes",
                true,
                "/internal/a2a/diagnostics",
                true,
                "/internal/a2a/spec",
                true,
                "/a2a/rpc",
                true,
                "/internal/a2a/readiness",
                true,
                "/internal/a2a/readiness/issues",
                true)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endpoint path")
                .hasMessageContaining("binding report path");
    }

    @Test
    void allowsDuplicateDisabledConfigPaths() {
        WayangA2aJsonRpcHttpRouteSurface.requireDistinctConfigPaths(configValues(
                "/a2a/rpc",
                "/internal/a2a/smoke",
                true,
                "/internal/a2a/routes",
                true,
                "/internal/a2a/diagnostics",
                true,
                "/internal/a2a/spec",
                true,
                "/a2a/rpc",
                false,
                "/internal/a2a/readiness",
                true,
                "/internal/a2a/readiness/issues",
                true));
    }

    private static WayangA2aJsonRpcHttpRouteSurface.ConfigValues configValues(
            String endpointPath,
            String smokePath,
            boolean smokeEnabled,
            String routeCatalogPath,
            boolean routeCatalogEnabled,
            String diagnosticsReportPath,
            boolean diagnosticsReportEnabled,
            String specComplianceReportPath,
            boolean specComplianceReportEnabled,
            String bindingReportPath,
            boolean bindingReportEnabled,
            String readinessPath,
            boolean readinessEnabled,
            String readinessIssueSummaryPath,
            boolean readinessIssueSummaryEnabled) {
        return new WayangA2aJsonRpcHttpRouteSurface.ConfigValues(
                endpointPath,
                smokePath,
                smokeEnabled,
                routeCatalogPath,
                routeCatalogEnabled,
                diagnosticsReportPath,
                diagnosticsReportEnabled,
                specComplianceReportPath,
                specComplianceReportEnabled,
                bindingReportPath,
                bindingReportEnabled,
                readinessPath,
                readinessEnabled,
                readinessIssueSummaryPath,
                readinessIssueSummaryEnabled);
    }
}
