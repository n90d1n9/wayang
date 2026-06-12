package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Objects;

record WayangA2aJsonRpcHttpRouteSurface(
        String key,
        String routeName,
        String operation,
        String descriptorField,
        String pathField,
        String enabledField,
        boolean endpoint,
        String allow) {

    WayangA2aJsonRpcHttpRouteSurface {
        key = WayangA2aMaps.required(key, "key");
        routeName = WayangA2aMaps.required(routeName, "routeName");
        operation = WayangA2aMaps.required(operation, "operation");
        descriptorField = WayangA2aMaps.required(descriptorField, "descriptorField");
        pathField = WayangA2aMaps.required(pathField, "pathField");
        enabledField = enabledField == null ? "" : enabledField.trim();
        allow = WayangA2aMaps.required(allow, "allow");
    }

    static List<WayangA2aJsonRpcHttpRouteSurface> ordered() {
        return WayangA2aJsonRpcHttpRouteSurfaceCatalog.ordered();
    }

    static List<WayangA2aJsonRpcHttpRouteSurface> bindingReportRequiredOrder() {
        return WayangA2aJsonRpcHttpRouteSurfaceCatalog.bindingReportRequiredOrder();
    }

    static void requireDistinctConfigPaths(ConfigValues config) {
        ConfigValues resolved = Objects.requireNonNull(config, "config");
        List<ConfigPath> paths = ordered().stream()
                .map(surface -> surface.configPath(resolved))
                .toList();
        for (int left = 0; left < paths.size(); left++) {
            for (int right = left + 1; right < paths.size(); right++) {
                ConfigPath leftPath = paths.get(left);
                ConfigPath rightPath = paths.get(right);
                if (leftPath.enabled()
                        && rightPath.enabled()
                        && leftPath.path().equals(rightPath.path())) {
                    throw new IllegalArgumentException(
                            "JSON-RPC " + leftPath.name() + " and " + rightPath.name() + " must differ");
                }
            }
        }
    }

    static WayangA2aJsonRpcHttpRouteSurface endpointSurface() {
        return WayangA2aJsonRpcHttpRouteSurfaceCatalog.endpoint();
    }

    static WayangA2aJsonRpcHttpRouteSurface smokeSurface() {
        return WayangA2aJsonRpcHttpRouteSurfaceCatalog.smoke();
    }

    static WayangA2aJsonRpcHttpRouteSurface routeCatalogSurface() {
        return WayangA2aJsonRpcHttpRouteSurfaceCatalog.routeCatalog();
    }

    static WayangA2aJsonRpcHttpRouteSurface diagnosticsReportSurface() {
        return WayangA2aJsonRpcHttpRouteSurfaceCatalog.diagnosticsReport();
    }

    static WayangA2aJsonRpcHttpRouteSurface specComplianceReportSurface() {
        return WayangA2aJsonRpcHttpRouteSurfaceCatalog.specComplianceReport();
    }

    static WayangA2aJsonRpcHttpRouteSurface bindingReportSurface() {
        return WayangA2aJsonRpcHttpRouteSurfaceCatalog.bindingReport();
    }

    static WayangA2aJsonRpcHttpRouteSurface readinessSurface() {
        return WayangA2aJsonRpcHttpRouteSurfaceCatalog.readiness();
    }

    static WayangA2aJsonRpcHttpRouteSurface readinessIssueSummarySurface() {
        return WayangA2aJsonRpcHttpRouteSurfaceCatalog.readinessIssueSummary();
    }

    WayangA2aJsonRpcHttpRouteDescriptor descriptor(WayangA2aJsonRpcHttpConfig config) {
        WayangA2aJsonRpcHttpConfig resolved = Objects.requireNonNull(config, "config");
        if (endpoint) {
            return new WayangA2aJsonRpcHttpRouteDescriptor(
                    key,
                    routeName,
                    operation,
                    true,
                    resolved.endpointPath(),
                    "POST",
                    allow,
                    WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON,
                    List.of(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON, A2aProtocol.EVENT_STREAM_MEDIA_TYPE),
                    true);
        }
        return new WayangA2aJsonRpcHttpRouteDescriptor(
                key,
                routeName,
                operation,
                configEnabled(resolved),
                configPath(resolved),
                "GET",
                allow,
                "",
                List.of(WayangA2aJsonRpcDispatcher.MEDIA_TYPE_JSON),
                false);
    }

    String configPath(WayangA2aJsonRpcHttpConfig config) {
        return configPath(ConfigValues.from(config)).path();
    }

    boolean configEnabled(WayangA2aJsonRpcHttpConfig config) {
        return configPath(ConfigValues.from(config)).enabled();
    }

    private ConfigPath configPath(ConfigValues config) {
        return switch (key) {
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_SMOKE ->
                    new ConfigPath(routeName + " path", config.smokePath(), config.smokeEnabled());
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_ROUTE_CATALOG ->
                    new ConfigPath(routeName + " path", config.routeCatalogPath(), config.routeCatalogEnabled());
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_DIAGNOSTICS_REPORT ->
                    new ConfigPath(routeName + " path", config.diagnosticsReportPath(),
                            config.diagnosticsReportEnabled());
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_SPEC_COMPLIANCE_REPORT ->
                    new ConfigPath(routeName + " path", config.specComplianceReportPath(),
                            config.specComplianceReportEnabled());
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_BINDING_REPORT ->
                    new ConfigPath(routeName + " path", config.bindingReportPath(), config.bindingReportEnabled());
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS ->
                    new ConfigPath(routeName + " path", config.readinessPath(), config.readinessEnabled());
            case WayangA2aJsonRpcHttpRouteDescriptor.KEY_READINESS_ISSUE_SUMMARY ->
                    new ConfigPath(routeName + " path", config.readinessIssueSummaryPath(),
                            config.readinessIssueSummaryEnabled());
            default -> new ConfigPath(routeName + " path", config.endpointPath(), true);
        };
    }

    record ConfigValues(
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

        static ConfigValues from(WayangA2aJsonRpcHttpConfig config) {
            WayangA2aJsonRpcHttpConfig resolved = Objects.requireNonNull(config, "config");
            return new ConfigValues(
                    resolved.endpointPath(),
                    resolved.smokePath(),
                    resolved.smokeEnabled(),
                    resolved.routeCatalogPath(),
                    resolved.routeCatalogEnabled(),
                    resolved.diagnosticsReportPath(),
                    resolved.diagnosticsReportEnabled(),
                    resolved.specComplianceReportPath(),
                    resolved.specComplianceReportEnabled(),
                    resolved.bindingReportPath(),
                    resolved.bindingReportEnabled(),
                    resolved.readinessPath(),
                    resolved.readinessEnabled(),
                    resolved.readinessIssueSummaryPath(),
                    resolved.readinessIssueSummaryEnabled());
        }
    }

    private record ConfigPath(String name, String path, boolean enabled) {
    }
}
