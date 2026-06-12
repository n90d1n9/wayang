package tech.kayys.wayang.a2a.wayang;

import java.util.Map;

/**
 * Runtime-neutral exposure settings for the A2A JSON-RPC HTTP adapter.
 */
public record WayangA2aJsonRpcHttpConfig(
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

    public WayangA2aJsonRpcHttpConfig {
        endpointPath = WayangA2aHttpRequest.normalizePath(endpointPath);
        smokePath = WayangA2aHttpRequest.normalizePath(smokePath);
        routeCatalogPath = WayangA2aHttpRequest.normalizePath(routeCatalogPath);
        diagnosticsReportPath = WayangA2aHttpRequest.normalizePath(diagnosticsReportPath);
        specComplianceReportPath = WayangA2aHttpRequest.normalizePath(specComplianceReportPath);
        bindingReportPath = WayangA2aHttpRequest.normalizePath(bindingReportPath);
        readinessPath = WayangA2aHttpRequest.normalizePath(readinessPath);
        readinessIssueSummaryPath = WayangA2aHttpRequest.normalizePath(readinessIssueSummaryPath);
        WayangA2aJsonRpcHttpRouteSurface.requireDistinctConfigPaths(new WayangA2aJsonRpcHttpRouteSurface.ConfigValues(
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
                readinessIssueSummaryEnabled));
    }

    public static WayangA2aJsonRpcHttpConfig defaults() {
        return builder().build();
    }

    public static WayangA2aJsonRpcHttpConfig fromMap(Map<?, ?> values) {
        return WayangA2aJsonRpcHttpConfigDecoder.fromMap(values);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcHttpConfigProjection.config(this);
    }

    public static final class Builder {

        private String endpointPath = WayangA2aJsonRpcHttpAdapter.DEFAULT_ENDPOINT_PATH;
        private String smokePath = WayangA2aJsonRpcHttpAdapter.DEFAULT_SMOKE_PATH;
        private boolean smokeEnabled = true;
        private String routeCatalogPath = WayangA2aJsonRpcHttpAdapter.DEFAULT_ROUTE_CATALOG_PATH;
        private boolean routeCatalogEnabled = true;
        private String diagnosticsReportPath = WayangA2aJsonRpcHttpAdapter.DEFAULT_DIAGNOSTICS_REPORT_PATH;
        private boolean diagnosticsReportEnabled = true;
        private String specComplianceReportPath =
                WayangA2aJsonRpcHttpAdapter.DEFAULT_SPEC_COMPLIANCE_REPORT_PATH;
        private boolean specComplianceReportEnabled = true;
        private String bindingReportPath = WayangA2aJsonRpcHttpAdapter.DEFAULT_BINDING_REPORT_PATH;
        private boolean bindingReportEnabled = true;
        private String readinessPath = WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_PATH;
        private boolean readinessEnabled = true;
        private String readinessIssueSummaryPath =
                WayangA2aJsonRpcHttpAdapter.DEFAULT_READINESS_ISSUE_SUMMARY_PATH;
        private boolean readinessIssueSummaryEnabled = true;

        private Builder() {
        }

        public Builder endpointPath(String endpointPath) {
            this.endpointPath = endpointPath;
            return this;
        }

        public Builder smokePath(String smokePath) {
            this.smokePath = smokePath;
            return this;
        }

        public Builder smokeEnabled(boolean smokeEnabled) {
            this.smokeEnabled = smokeEnabled;
            return this;
        }

        public Builder routeCatalogPath(String routeCatalogPath) {
            this.routeCatalogPath = routeCatalogPath;
            return this;
        }

        public Builder routeCatalogEnabled(boolean routeCatalogEnabled) {
            this.routeCatalogEnabled = routeCatalogEnabled;
            return this;
        }

        public Builder diagnosticsReportPath(String diagnosticsReportPath) {
            this.diagnosticsReportPath = diagnosticsReportPath;
            return this;
        }

        public Builder diagnosticsReportEnabled(boolean diagnosticsReportEnabled) {
            this.diagnosticsReportEnabled = diagnosticsReportEnabled;
            return this;
        }

        public Builder specComplianceReportPath(String specComplianceReportPath) {
            this.specComplianceReportPath = specComplianceReportPath;
            return this;
        }

        public Builder specComplianceReportEnabled(boolean specComplianceReportEnabled) {
            this.specComplianceReportEnabled = specComplianceReportEnabled;
            return this;
        }

        public Builder bindingReportPath(String bindingReportPath) {
            this.bindingReportPath = bindingReportPath;
            return this;
        }

        public Builder bindingReportEnabled(boolean bindingReportEnabled) {
            this.bindingReportEnabled = bindingReportEnabled;
            return this;
        }

        public Builder readinessPath(String readinessPath) {
            this.readinessPath = readinessPath;
            return this;
        }

        public Builder readinessEnabled(boolean readinessEnabled) {
            this.readinessEnabled = readinessEnabled;
            return this;
        }

        public Builder readinessIssueSummaryPath(String readinessIssueSummaryPath) {
            this.readinessIssueSummaryPath = readinessIssueSummaryPath;
            return this;
        }

        public Builder readinessIssueSummaryEnabled(boolean readinessIssueSummaryEnabled) {
            this.readinessIssueSummaryEnabled = readinessIssueSummaryEnabled;
            return this;
        }

        public WayangA2aJsonRpcHttpConfig build() {
            return new WayangA2aJsonRpcHttpConfig(
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
}
