package tech.kayys.wayang.a2ui.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Mounted-endpoint diagnostics runner for raw A2UI HTTP framework calls.
 */
public final class WayangA2uiHttpEndpointDiagnostics {

    public static final String DEFAULT_ID = "a2ui-http-endpoint-diagnostics";

    private static final String ATTRIBUTE_DIAGNOSTIC_KIND = "diagnosticKind";
    private static final String ATTRIBUTE_ROUTE_COUNT = "routeCount";
    private static final String ATTRIBUTE_ROUTE_OPERATION = "routeOperation";
    private static final String ATTRIBUTE_ROUTE_METHOD = "routeMethod";

    private final WayangA2uiHttpEndpointBinding endpoint;
    private final WayangA2uiHttpEndpointDiagnosticConfig config;

    public WayangA2uiHttpEndpointDiagnostics(WayangA2uiHttpEndpointBinding endpoint) {
        this(endpoint, WayangA2uiHttpEndpointDiagnosticConfig.defaults());
    }

    public WayangA2uiHttpEndpointDiagnostics(
            WayangA2uiHttpEndpointBinding endpoint,
            WayangA2uiHttpEndpointDiagnosticConfig config) {
        this.endpoint = Objects.requireNonNull(endpoint, "endpoint");
        this.config = config == null ? WayangA2uiHttpEndpointDiagnosticConfig.defaults() : config;
    }

    public static WayangA2uiHttpEndpointDiagnostics of(WayangA2uiHttpEndpointBinding endpoint) {
        return new WayangA2uiHttpEndpointDiagnostics(endpoint);
    }

    public static WayangA2uiHttpEndpointDiagnostics of(
            WayangA2uiHttpEndpointBinding endpoint,
            WayangA2uiHttpEndpointDiagnosticConfig config) {
        return new WayangA2uiHttpEndpointDiagnostics(endpoint, config);
    }

    public WayangA2uiHttpEndpointDiagnosticConfig config() {
        return config;
    }

    public WayangA2uiHttpEndpointDiagnosticResult runDefault() {
        return run(
                DEFAULT_ID,
                defaultRequests(),
                defaultAttributes());
    }

    public WayangA2uiHttpEndpointDiagnosticResult run(
            String diagnosticsId,
            List<WayangA2uiHttpEndpointDiagnosticRequest> requests) {
        return run(diagnosticsId, requests, Map.of());
    }

    public WayangA2uiHttpEndpointDiagnosticResult run(WayangA2uiHttpEndpointDiagnosticPlan plan) {
        WayangA2uiHttpEndpointDiagnosticPlan resolved = Objects.requireNonNull(plan, "plan");
        WayangA2uiHttpEndpointDiagnostics planned =
                new WayangA2uiHttpEndpointDiagnostics(endpoint, resolved.config());
        List<WayangA2uiHttpEndpointDiagnosticRequest> requests = resolved.usesDefaultRequests()
                ? planned.defaultRequests()
                : resolved.requests();
        Map<String, Object> attributes = resolved.usesDefaultRequests()
                ? merge(planned.defaultAttributes(), resolved.attributes())
                : resolved.attributes();
        return planned.run(resolved.diagnosticsId(), requests, attributes);
    }

    public WayangA2uiHttpEndpointDiagnosticResult runPlanMap(Map<?, ?> plan) {
        return run(WayangA2uiHttpEndpointDiagnosticPlan.fromMap(plan));
    }

    public WayangA2uiHttpEndpointDiagnosticResult runPlanJson(String planJson) {
        return run(WayangA2uiHttpEndpointDiagnosticPlan.fromJson(planJson));
    }

    public WayangA2uiHttpEndpointDiagnosticResult runFromMaps(
            String diagnosticsId,
            List<? extends Map<?, ?>> requests) {
        return runFromMaps(diagnosticsId, requests, Map.of());
    }

    public WayangA2uiHttpEndpointDiagnosticResult runFromMaps(
            String diagnosticsId,
            List<? extends Map<?, ?>> requests,
            Map<?, ?> attributes) {
        List<WayangA2uiHttpEndpointDiagnosticRequest> decoded = requests == null
                ? List.of()
                : requests.stream()
                        .filter(Objects::nonNull)
                        .map(WayangA2uiHttpEndpointDiagnosticRequest::fromMap)
                        .toList();
        return run(diagnosticsId, decoded, attributes);
    }

    public WayangA2uiHttpEndpointDiagnosticResult run(
            String diagnosticsId,
            List<WayangA2uiHttpEndpointDiagnosticRequest> requests,
            Map<?, ?> attributes) {
        List<WayangA2uiHttpEndpointExchange> exchanges = new ArrayList<>();
        if (requests != null) {
            for (WayangA2uiHttpEndpointDiagnosticRequest request : requests) {
                if (request != null) {
                    WayangA2uiHttpEndpointDiagnosticRequest resolved = withDefaults(request);
                    exchanges.add(endpoint.exchange(
                            resolved.method(),
                            resolved.rawPath(),
                            resolved.body(),
                            resolved.headers(),
                            resolved.attributes()));
                }
            }
        }
        return new WayangA2uiHttpEndpointDiagnosticResult(
                diagnosticsId,
                exchanges,
                WayangA2uiTransportMaps.copy(attributes));
    }

    public List<WayangA2uiHttpEndpointDiagnosticRequest> defaultRequests() {
        WayangA2uiHttpEndpointPublication publication = endpoint.publication();
        List<WayangA2uiHttpEndpointDiagnosticRequest> requests = new ArrayList<>();
        if (config.routeCatalogProbe()) {
            addOperationRequest(requests, publication, WayangA2uiHttpRoute.OPERATION_ROUTE_CATALOG);
        }
        if (config.bindingReportProbe()) {
            addOperationRequest(requests, publication, WayangA2uiHttpRoute.OPERATION_BINDING_REPORT);
        }
        if (config.smokeProbe()) {
            addOperationRequest(requests, publication, WayangA2uiHttpRoute.OPERATION_SMOKE);
        }
        if (config.readinessProbe()) {
            addOperationRequest(requests, publication, WayangA2uiHttpRoute.OPERATION_READINESS);
        }
        if (config.routeOptionsProbe()) {
            for (Map<String, Object> route : publication.routes()) {
                requests.add(WayangA2uiHttpEndpointDiagnosticRequest.of(
                        "OPTIONS",
                        routePath(route),
                        "",
                        Map.of(),
                        Map.of(
                                ATTRIBUTE_ROUTE_OPERATION, string(route.get("operation")),
                                ATTRIBUTE_ROUTE_METHOD, string(route.get("method")))));
            }
        }
        return requests.stream()
                .map(this::withDefaults)
                .toList();
    }

    private static void addOperationRequest(
            List<WayangA2uiHttpEndpointDiagnosticRequest> requests,
            WayangA2uiHttpEndpointPublication publication,
            String operation) {
        publication.routeForOperation(operation)
                .map(WayangA2uiHttpEndpointDiagnostics::routePath)
                .map(WayangA2uiHttpEndpointDiagnosticRequest::get)
                .ifPresent(requests::add);
    }

    private static String routePath(Map<String, Object> route) {
        return string(route.get("path"));
    }

    private WayangA2uiHttpEndpointDiagnosticRequest withDefaults(
            WayangA2uiHttpEndpointDiagnosticRequest request) {
        WayangA2uiHttpEndpointDiagnosticRequest resolved = Objects.requireNonNull(request, "request");
        return new WayangA2uiHttpEndpointDiagnosticRequest(
                resolved.method(),
                resolved.rawPath(),
                resolved.body(),
                merge(config.defaultHeaders(), resolved.headers()),
                merge(config.defaultAttributes(), resolved.attributes()));
    }

    private Map<String, Object> defaultAttributes() {
        Map<String, Object> attributes = new LinkedHashMap<>(config.defaultAttributes());
        attributes.put(ATTRIBUTE_DIAGNOSTIC_KIND, "endpoint-default");
        attributes.put(ATTRIBUTE_ROUTE_COUNT, endpoint.routes().size());
        attributes.put("diagnosticConfig", config.toMap());
        return WayangA2uiTransportMaps.freeze(attributes);
    }

    private static Map<String, Object> merge(Map<String, Object> defaults, Map<String, Object> values) {
        Map<String, Object> merged = new LinkedHashMap<>(defaults);
        merged.putAll(values);
        return WayangA2uiTransportMaps.freeze(merged);
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
