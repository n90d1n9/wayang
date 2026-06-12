package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Runtime-neutral exposure settings for the Agentic Commerce HTTP adapter.
 */
public record AgenticCommerceHttpAdapterConfig(
        String checkoutBasePath,
        String smokePath,
        boolean smokeEnabled,
        String bindingReportPath,
        boolean bindingReportEnabled) {

    public AgenticCommerceHttpAdapterConfig {
        checkoutBasePath = normalizePath(checkoutBasePath);
        smokePath = normalizePath(smokePath);
        bindingReportPath = normalizePath(bindingReportPath);
        if (smokeEnabled && checkoutBasePath.equals(smokePath)) {
            throw new IllegalArgumentException("Agentic Commerce checkout base path and smoke path must differ");
        }
        if (bindingReportEnabled && checkoutBasePath.equals(bindingReportPath)) {
            throw new IllegalArgumentException("Agentic Commerce checkout base path and binding report path must differ");
        }
        if (smokeEnabled && bindingReportEnabled && smokePath.equals(bindingReportPath)) {
            throw new IllegalArgumentException("Agentic Commerce smoke path and binding report path must differ");
        }
    }

    public static AgenticCommerceHttpAdapterConfig defaults() {
        return builder().build();
    }

    public static AgenticCommerceHttpAdapterConfig fromMap(Map<?, ?> values) {
        Map<String, Object> resolved = AgenticCommerceWayangMaps.copy(values);
        Builder builder = builder();
        AgenticCommerceWayangMaps.optional(AgenticCommerceWayangMaps.first(
                resolved,
                "checkoutBasePath",
                "basePath",
                "endpointPath",
                "path")).ifPresent(builder::checkoutBasePath);
        AgenticCommerceWayangMaps.optional(AgenticCommerceWayangMaps.first(
                resolved,
                "smokePath",
                "smokeEndpointPath")).ifPresent(builder::smokePath);
        AgenticCommerceWayangMaps.firstBoolean(resolved, "smokeEnabled", "enableSmoke").ifPresent(builder::smokeEnabled);
        AgenticCommerceWayangMaps.optional(AgenticCommerceWayangMaps.first(
                resolved,
                "bindingReportPath",
                "bindingPath",
                "diagnosticsPath")).ifPresent(builder::bindingReportPath);
        AgenticCommerceWayangMaps.firstBoolean(
                resolved,
                "bindingReportEnabled",
                "enableBindingReport",
                "diagnosticsEnabled")
                .ifPresent(builder::bindingReportEnabled);
        return builder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("checkoutBasePath", checkoutBasePath);
        values.put("smokePath", smokePath);
        values.put("smokeEnabled", smokeEnabled);
        values.put("bindingReportPath", bindingReportPath);
        values.put("bindingReportEnabled", bindingReportEnabled);
        return Map.copyOf(values);
    }

    private static String normalizePath(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        int queryStart = normalized.indexOf('?');
        if (queryStart >= 0) {
            normalized = normalized.substring(0, queryStart);
        }
        return new AgenticCommerceHttpRequest("GET", normalized, "", Map.of(), Map.of()).path();
    }

    public static final class Builder {

        private String checkoutBasePath = AgenticCommerceHttpAdapter.DEFAULT_CHECKOUT_BASE_PATH;
        private String smokePath = AgenticCommerceHttpAdapter.DEFAULT_SMOKE_PATH;
        private boolean smokeEnabled = true;
        private String bindingReportPath = AgenticCommerceHttpAdapter.DEFAULT_BINDING_REPORT_PATH;
        private boolean bindingReportEnabled = true;

        private Builder() {
        }

        public Builder checkoutBasePath(String checkoutBasePath) {
            this.checkoutBasePath = checkoutBasePath;
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

        public Builder bindingReportPath(String bindingReportPath) {
            this.bindingReportPath = bindingReportPath;
            return this;
        }

        public Builder bindingReportEnabled(boolean bindingReportEnabled) {
            this.bindingReportEnabled = bindingReportEnabled;
            return this;
        }

        public AgenticCommerceHttpAdapterConfig build() {
            return new AgenticCommerceHttpAdapterConfig(
                    checkoutBasePath,
                    smokePath,
                    smokeEnabled,
                    bindingReportPath,
                    bindingReportEnabled);
        }
    }
}
