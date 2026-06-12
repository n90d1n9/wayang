package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRoute;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceRouteCatalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JSON-ready diagnostics for the Agentic Commerce checkout HTTP binding.
 */
public record AgenticCommerceHttpBindingReport(
        AgenticCommerceHttpAdapterConfig config,
        List<AgenticCommerceHttpRoute> routes) {

    public static final String OPERATION_CHECKOUT_BINDING_REPORT = "agenticCommerce.checkout.bindingReport";

    public AgenticCommerceHttpBindingReport {
        config = Objects.requireNonNull(config, "config");
        routes = routes == null || routes.isEmpty()
                ? AgenticCommerceRouteCatalog.checkoutCatalog().routes()
                : routes.stream()
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
    }

    public static AgenticCommerceHttpBindingReport defaults() {
        return fromConfig(AgenticCommerceHttpAdapterConfig.defaults());
    }

    public static AgenticCommerceHttpBindingReport from(AgenticCommerceHttpAdapter adapter) {
        return fromConfig(Objects.requireNonNull(adapter, "adapter").config());
    }

    public static AgenticCommerceHttpBindingReport fromConfig(AgenticCommerceHttpAdapterConfig config) {
        return new AgenticCommerceHttpBindingReport(config, AgenticCommerceRouteCatalog.checkoutCatalog().routes());
    }

    public int routeCount() {
        return routes.size();
    }

    public AgenticCommerceHttpResponse response() {
        return new AgenticCommerceHttpResponse(
                200,
                AgenticCommerceJson.write(toMap()),
                Map.of(
                        AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON,
                        AgenticCommerceHttpAdapter.HEADER_ROUTE_OPERATION, OPERATION_CHECKOUT_BINDING_REPORT,
                        AgenticCommerceHttpAdapter.HEADER_SPEC_VERSION, AgenticCommerceProtocol.SPEC_VERSION,
                        AgenticCommerceHttpAdapter.HEADER_ALLOW, AgenticCommerceHttpAdapter.ALLOW_BINDING_REPORT),
                Map.of("operation", OPERATION_CHECKOUT_BINDING_REPORT));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        values.put("specVersion", AgenticCommerceProtocol.SPEC_VERSION);
        values.put("checkout", checkout());
        values.put("smoke", smoke());
        values.put("bindingReport", bindingReport());
        values.put("routeCount", routeCount());
        values.put("routes", routes.stream().map(this::route).toList());
        values.put("config", config.toMap());
        return Map.copyOf(values);
    }

    public String toJson() {
        return AgenticCommerceJson.write(toMap());
    }

    private Map<String, Object> checkout() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("basePath", config.checkoutBasePath());
        values.put("requestMediaType", AgenticCommerceProtocol.MIME_JSON);
        values.put("responseMediaType", AgenticCommerceProtocol.MIME_JSON);
        values.put("routeCount", routeCount());
        return Map.copyOf(values);
    }

    private Map<String, Object> smoke() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", config.smokeEnabled());
        values.put("path", config.smokePath());
        values.put("httpMethod", "GET");
        values.put("allow", AgenticCommerceHttpAdapter.ALLOW_SMOKE);
        values.put("responseMediaType", AgenticCommerceProtocol.MIME_JSON);
        return Map.copyOf(values);
    }

    private Map<String, Object> bindingReport() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", config.bindingReportEnabled());
        values.put("path", config.bindingReportPath());
        values.put("httpMethod", "GET");
        values.put("allow", AgenticCommerceHttpAdapter.ALLOW_BINDING_REPORT);
        values.put("responseMediaType", AgenticCommerceProtocol.MIME_JSON);
        return Map.copyOf(values);
    }

    private Map<String, Object> route(AgenticCommerceHttpRoute route) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("operation", route.operation());
        values.put("httpMethod", route.method());
        values.put("pathTemplate", route.pathTemplate());
        values.put("publicPathTemplate", publicPath(route.pathTemplate()));
        values.put("allow", route.method() + ", OPTIONS");
        values.put("requestBodyRequired", route.requestBodyRequired());
        values.put("successStatusCodes", route.successStatusCodes());
        values.put("requestMediaType", route.requestBodyRequired() ? AgenticCommerceProtocol.MIME_JSON : "");
        values.put("responseMediaType", AgenticCommerceProtocol.MIME_JSON);
        return Map.copyOf(values);
    }

    private String publicPath(String pathTemplate) {
        if ("/".equals(config.checkoutBasePath())) {
            return pathTemplate;
        }
        return config.checkoutBasePath() + pathTemplate;
    }
}
