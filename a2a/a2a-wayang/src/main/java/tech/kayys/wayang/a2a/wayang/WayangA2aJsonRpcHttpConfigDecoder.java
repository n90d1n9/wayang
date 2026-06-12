package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Optional;

final class WayangA2aJsonRpcHttpConfigDecoder {

    private WayangA2aJsonRpcHttpConfigDecoder() {
    }

    static WayangA2aJsonRpcHttpConfig fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = WayangA2aMaps.copyMap(values);
        WayangA2aJsonRpcHttpConfig.Builder builder = WayangA2aJsonRpcHttpConfig.builder();
        WayangA2aMaps.firstString(resolved, "endpointPath", "endpoint", "path")
                .ifPresent(builder::endpointPath);
        WayangA2aMaps.firstString(resolved, "smokePath", "smokeEndpointPath")
                .ifPresent(builder::smokePath);
        firstBoolean(resolved, "smokeEnabled", "enableSmoke")
                .ifPresent(builder::smokeEnabled);
        WayangA2aMaps.firstString(resolved, "routeCatalogPath", "routesPath", "httpRouteCatalogPath", "catalogPath")
                .ifPresent(builder::routeCatalogPath);
        firstBoolean(resolved, "routeCatalogEnabled", "enableRouteCatalog", "routesEnabled", "httpRouteCatalogEnabled")
                .ifPresent(builder::routeCatalogEnabled);
        WayangA2aMaps.firstString(
                        resolved,
                        "diagnosticsReportPath",
                        "aggregateDiagnosticsPath",
                        "jsonRpcDiagnosticsPath",
                        "reportPath")
                .ifPresent(builder::diagnosticsReportPath);
        firstBoolean(
                        resolved,
                        "diagnosticsReportEnabled",
                        "enableDiagnosticsReport",
                        "aggregateDiagnosticsEnabled",
                        "jsonRpcDiagnosticsEnabled")
                .ifPresent(builder::diagnosticsReportEnabled);
        WayangA2aMaps.firstString(
                        resolved,
                        "specComplianceReportPath",
                        "specCompliancePath",
                        "jsonRpcSpecCompliancePath",
                        "complianceReportPath")
                .ifPresent(builder::specComplianceReportPath);
        firstBoolean(
                        resolved,
                        "specComplianceReportEnabled",
                        "enableSpecComplianceReport",
                        "specComplianceEnabled",
                        "jsonRpcSpecComplianceEnabled")
                .ifPresent(builder::specComplianceReportEnabled);
        WayangA2aMaps.firstString(resolved, "bindingReportPath", "bindingPath", "diagnosticsPath")
                .ifPresent(builder::bindingReportPath);
        firstBoolean(resolved, "bindingReportEnabled", "enableBindingReport", "diagnosticsEnabled")
                .ifPresent(builder::bindingReportEnabled);
        WayangA2aMaps.firstString(resolved, "readinessPath", "healthPath")
                .ifPresent(builder::readinessPath);
        firstBoolean(resolved, "readinessEnabled", "enableReadiness", "healthEnabled")
                .ifPresent(builder::readinessEnabled);
        WayangA2aMaps.firstString(
                        resolved,
                        "readinessIssueSummaryPath",
                        "readinessIssuesPath",
                        "healthIssueSummaryPath",
                        "issueSummaryPath")
                .ifPresent(builder::readinessIssueSummaryPath);
        firstBoolean(
                        resolved,
                        "readinessIssueSummaryEnabled",
                        "enableReadinessIssueSummary",
                        "healthIssueSummaryEnabled",
                        "issueSummaryEnabled")
                .ifPresent(builder::readinessIssueSummaryEnabled);
        return builder.build();
    }

    static Optional<Boolean> firstBoolean(Map<String, ?> values, String... keys) {
        if (values == null || values.isEmpty()) {
            return Optional.empty();
        }
        for (String key : keys) {
            Object value = values.get(key);
            if (value instanceof Boolean booleanValue) {
                return Optional.of(booleanValue);
            }
            String text = WayangA2aMaps.optional(value);
            if (text != null) {
                if ("true".equalsIgnoreCase(text) || "yes".equalsIgnoreCase(text) || "1".equals(text)) {
                    return Optional.of(true);
                }
                if ("false".equalsIgnoreCase(text) || "no".equalsIgnoreCase(text) || "0".equals(text)) {
                    return Optional.of(false);
                }
            }
        }
        return Optional.empty();
    }
}
