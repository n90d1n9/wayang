package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpSmokeResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpSmokeProbeResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpSmokeRunner;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceError;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRoute;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceRequestValidator;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceRouteCatalog;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceValidationIssue;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceValidationReport;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dependency-free HTTP-shaped adapter for exposing Agentic Commerce checkout routes.
 */
public final class AgenticCommerceHttpAdapter {

    public static final String DEFAULT_CHECKOUT_BASE_PATH = "/agentic-commerce";
    public static final String DEFAULT_SMOKE_PATH = "/agentic-commerce/smoke";
    public static final String DEFAULT_BINDING_REPORT_PATH = "/agentic-commerce/binding";
    public static final String HEADER_ALLOW = "Allow";
    public static final String HEADER_ROUTE_OPERATION = "X-Wayang-Agentic-Commerce-Route-Operation";
    public static final String HEADER_SPEC_VERSION = "X-Wayang-Agentic-Commerce-Spec-Version";
    public static final String HEADER_SMOKE_PASSED = "X-Wayang-Agentic-Commerce-Smoke-Passed";
    public static final String HEADER_SMOKE_EXIT_CODE = "X-Wayang-Agentic-Commerce-Smoke-Exit-Code";
    public static final String HEADER_SMOKE_SCENARIO = "X-Wayang-Agentic-Commerce-Smoke-Scenario";
    public static final String OPERATION_CHECKOUT_OPTIONS = "agenticCommerce.checkout.options";
    public static final String ALLOW_SMOKE = "GET, OPTIONS";
    public static final String ALLOW_BINDING_REPORT = "GET, OPTIONS";

    private final AgenticCommerceConnector connector;
    private final AgenticCommerceCheckoutHttpSmokeRunner smokeRunner;
    private final AgenticCommerceHttpAdapterConfig config;
    private final AgenticCommerceRouteCatalog catalog;
    private final AgenticCommerceRequestValidator validator;

    public AgenticCommerceHttpAdapter(AgenticCommerceConnector connector) {
        this(connector, AgenticCommerceHttpAdapterConfig.defaults());
    }

    public AgenticCommerceHttpAdapter(
            AgenticCommerceConnector connector,
            AgenticCommerceHttpAdapterConfig config) {
        this(connector, AgenticCommerceCheckoutHttpSmokeRunner.checkout(), config);
    }

    public AgenticCommerceHttpAdapter(
            AgenticCommerceConnector connector,
            AgenticCommerceCheckoutHttpSmokeRunner smokeRunner,
            AgenticCommerceHttpAdapterConfig config) {
        this.connector = Objects.requireNonNull(connector, "connector");
        this.smokeRunner = smokeRunner == null ? AgenticCommerceCheckoutHttpSmokeRunner.checkout() : smokeRunner;
        this.config = config == null ? AgenticCommerceHttpAdapterConfig.defaults() : config;
        this.catalog = AgenticCommerceRouteCatalog.checkoutCatalog();
        this.validator = AgenticCommerceRequestValidator.checkout();
    }

    public static AgenticCommerceHttpAdapter of(AgenticCommerceConnector connector) {
        return new AgenticCommerceHttpAdapter(connector);
    }

    public static AgenticCommerceHttpAdapter configured(
            AgenticCommerceConnector connector,
            AgenticCommerceHttpAdapterConfig config) {
        return new AgenticCommerceHttpAdapter(connector, config);
    }

    public AgenticCommerceHttpAdapterConfig config() {
        return config;
    }

    public String checkoutBasePath() {
        return config.checkoutBasePath();
    }

    public String smokePath() {
        return config.smokePath();
    }

    public String bindingReportPath() {
        return config.bindingReportPath();
    }

    public AgenticCommerceHttpBindingReport bindingReport() {
        return AgenticCommerceHttpBindingReport.from(this);
    }

    public AgenticCommerceHttpResponse smoke() {
        return dispatch(new AgenticCommerceHttpRequest(
                "GET",
                config.smokePath(),
                "",
                Map.of(AgenticCommerceProtocol.HEADER_ACCEPT, AgenticCommerceProtocol.MIME_JSON),
                Map.of()));
    }

    public AgenticCommerceCheckoutHttpSmokeProbeResult smokeProbe() {
        return AgenticCommerceCheckoutHttpSmokeProbeResult.fromResponse(smoke());
    }

    public AgenticCommerceHttpResponse dispatch(AgenticCommerceHttpRequest request) {
        AgenticCommerceHttpRequest resolved = Objects.requireNonNull(request, "request");
        if (config.smokeEnabled() && resolved.path().equals(config.smokePath())) {
            return resolved.method().equals("OPTIONS")
                    ? options(config.smokePath(), AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE, ALLOW_SMOKE)
                    : dispatchSmoke(resolved);
        }
        if (config.bindingReportEnabled() && resolved.path().equals(config.bindingReportPath())) {
            return resolved.method().equals("OPTIONS")
                    ? options(
                            config.bindingReportPath(),
                            AgenticCommerceHttpBindingReport.OPERATION_CHECKOUT_BINDING_REPORT,
                            ALLOW_BINDING_REPORT)
                    : dispatchBindingReport(resolved);
        }
        return checkoutRoutePath(resolved.path())
                .map(routePath -> dispatchCheckout(resolved, routePath))
                .orElseGet(() -> error(
                        resolved,
                        404,
                        "checkout_path_not_found",
                        "No Agentic Commerce checkout path matches " + resolved.path() + ".",
                        "",
                        ""));
    }

    private AgenticCommerceHttpResponse dispatchCheckout(AgenticCommerceHttpRequest request, String routePath) {
        Optional<AgenticCommerceHttpRoute> pathRoute = catalog.routeForPath(routePath);
        if (pathRoute.isEmpty()) {
            return error(
                    request,
                    404,
                    "checkout_path_not_found",
                    "No Agentic Commerce checkout route matches " + routePath + ".",
                    "",
                    "");
        }
        String allow = allow(routePath);
        if (request.method().equals("OPTIONS")) {
            return options(request.path(), OPERATION_CHECKOUT_OPTIONS, allow);
        }
        AgenticCommerceHttpRequest translated = translate(request, routePath, pathRoute.get());
        Optional<AgenticCommerceHttpRoute> exactRoute = catalog.route(translated);
        if (exactRoute.isEmpty()) {
            return error(
                    request,
                    405,
                    "method_not_allowed",
                    "Agentic Commerce checkout route " + request.path() + " does not allow " + request.method() + ".",
                    allow,
                    pathRoute.get().operation());
        }
        if (!accepts(request, AgenticCommerceProtocol.MIME_JSON)) {
            String actual = request.header(AgenticCommerceProtocol.HEADER_ACCEPT).orElse("none");
            return error(
                    request,
                    406,
                    "not_acceptable",
                    "Agentic Commerce checkout routes produce application/json, but Accept was " + actual + ".",
                    allow,
                    exactRoute.get().operation());
        }
        AgenticCommerceValidationReport report = validator.validate(translate(request, routePath, exactRoute.get()));
        if (!report.valid()) {
            AgenticCommerceValidationIssue issue = report.issues().get(0);
            return error(
                    request,
                    status(issue.code()),
                    issue.code(),
                    issue.message(),
                    allow,
                    exactRoute.get().operation());
        }
        return connector.exchange(report.request())
                .withHeaders(routeHeaders(exactRoute.get().operation(), allow));
    }

    private AgenticCommerceHttpResponse dispatchSmoke(AgenticCommerceHttpRequest request) {
        if (!request.method().equals("GET")) {
            return error(
                    request,
                    405,
                    "method_not_allowed",
                    "Agentic Commerce smoke path " + config.smokePath() + " requires GET.",
                    ALLOW_SMOKE,
                    AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE);
        }
        if (!accepts(request, AgenticCommerceProtocol.MIME_JSON)) {
            String actual = request.header(AgenticCommerceProtocol.HEADER_ACCEPT).orElse("none");
            return error(
                    request,
                    406,
                    "not_acceptable",
                    "Agentic Commerce smoke path produces application/json, but Accept was " + actual + ".",
                    ALLOW_SMOKE,
                    AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE);
        }
        AgenticCommerceCheckoutHttpSmokeResult result = smokeRunner.run(connector);
        Map<String, Object> headers = new LinkedHashMap<>(routeHeaders(
                AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE,
                ALLOW_SMOKE));
        headers.put(HEADER_SMOKE_PASSED, result.passed());
        headers.put(HEADER_SMOKE_EXIT_CODE, result.exitCode());
        headers.put(HEADER_SMOKE_SCENARIO, result.scenarioResult().scenario().id());
        return new AgenticCommerceHttpResponse(
                200,
                AgenticCommerceJson.write(result.toMap()),
                headers,
                Map.of(
                        "operation",
                        AgenticCommerceProtocol.OPERATION_CHECKOUT_SMOKE,
                        AgenticCommerceWayang.METADATA_PROTOCOL,
                        AgenticCommerceWayang.PROTOCOL_ID));
    }

    private AgenticCommerceHttpResponse dispatchBindingReport(AgenticCommerceHttpRequest request) {
        if (!request.method().equals("GET")) {
            return error(
                    request,
                    405,
                    "method_not_allowed",
                    "Agentic Commerce binding report path " + config.bindingReportPath() + " requires GET.",
                    ALLOW_BINDING_REPORT,
                    AgenticCommerceHttpBindingReport.OPERATION_CHECKOUT_BINDING_REPORT);
        }
        if (!accepts(request, AgenticCommerceProtocol.MIME_JSON)) {
            String actual = request.header(AgenticCommerceProtocol.HEADER_ACCEPT).orElse("none");
            return error(
                    request,
                    406,
                    "not_acceptable",
                    "Agentic Commerce binding report produces application/json, but Accept was " + actual + ".",
                    ALLOW_BINDING_REPORT,
                    AgenticCommerceHttpBindingReport.OPERATION_CHECKOUT_BINDING_REPORT);
        }
        return bindingReport().response()
                .withHeaders(routeHeaders(
                        AgenticCommerceHttpBindingReport.OPERATION_CHECKOUT_BINDING_REPORT,
                        ALLOW_BINDING_REPORT));
    }

    private AgenticCommerceHttpResponse options(String path, String operation, String allow) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        payload.put("specVersion", AgenticCommerceProtocol.SPEC_VERSION);
        payload.put("operation", operation);
        payload.put("path", path);
        payload.put("allow", allow);
        payload.put("checkoutBasePath", config.checkoutBasePath());
        payload.put("smokePath", config.smokePath());
        payload.put("smokeEnabled", config.smokeEnabled());
        payload.put("bindingReportPath", config.bindingReportPath());
        payload.put("bindingReportEnabled", config.bindingReportEnabled());
        payload.put("routeCount", catalog.routeCount());
        return new AgenticCommerceHttpResponse(
                200,
                AgenticCommerceJson.write(Map.copyOf(payload)),
                routeHeaders(operation, allow),
                Map.of("operation", operation));
    }

    private AgenticCommerceHttpResponse error(
            AgenticCommerceHttpRequest request,
            int status,
            String code,
            String message,
            String allow,
            String operation) {
        Map<String, Object> body = Map.of("error", AgenticCommerceError.of(code, message).toMap());
        return new AgenticCommerceHttpResponse(
                status,
                AgenticCommerceJson.write(body),
                routeHeadersWithRequestId(request, operation, allow),
                Map.of("operation", operation));
    }

    private AgenticCommerceHttpRequest translate(
            AgenticCommerceHttpRequest request,
            String routePath,
            AgenticCommerceHttpRoute route) {
        Map<String, Object> attributes = new LinkedHashMap<>(request.attributes());
        attributes.put("protocol", AgenticCommerceWayang.PROTOCOL_ID);
        attributes.put("operation", route.operation());
        attributes.put(AgenticCommerceWayang.METADATA_PROTOCOL, AgenticCommerceWayang.PROTOCOL_ID);
        attributes.put(AgenticCommerceWayang.METADATA_SPEC_VERSION, AgenticCommerceProtocol.SPEC_VERSION);
        attributes.put(AgenticCommerceWayang.METADATA_OPERATION, route.operation());
        attributes.put(AgenticCommerceWayang.METADATA_PATH_TEMPLATE, route.pathTemplate());
        checkoutSessionId(routePath).ifPresent(value -> attributes.put("checkoutSessionId", value));
        return new AgenticCommerceHttpRequest(
                request.method(),
                routePath,
                request.body(),
                request.headers(),
                Map.copyOf(attributes));
    }

    private Optional<String> checkoutRoutePath(String path) {
        String normalized = normalizePath(path);
        String base = config.checkoutBasePath();
        if ("/".equals(base)) {
            return Optional.of(normalized);
        }
        if (normalized.equals(base)) {
            return Optional.of("/");
        }
        if (normalized.startsWith(base + "/")) {
            return Optional.of(normalized.substring(base.length()));
        }
        return Optional.empty();
    }

    private String allow(String routePath) {
        AgenticCommerceHttpRequest pathRequest = AgenticCommerceHttpRequest.get(routePath);
        List<String> methods = catalog.routes().stream()
                .filter(route -> route.path(pathRequest))
                .map(AgenticCommerceHttpRoute::method)
                .distinct()
                .sorted()
                .toList();
        return methods.isEmpty() ? "" : String.join(", ", methods) + ", OPTIONS";
    }

    private static Map<String, Object> routeHeaders(String operation, String allow) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON);
        headers.put(HEADER_SPEC_VERSION, AgenticCommerceProtocol.SPEC_VERSION);
        if (!AgenticCommerceWayangMaps.text(operation).isBlank()) {
            headers.put(HEADER_ROUTE_OPERATION, operation);
        }
        if (!AgenticCommerceWayangMaps.text(allow).isBlank()) {
            headers.put(HEADER_ALLOW, allow.trim());
        }
        return Map.copyOf(headers);
    }

    private static Map<String, Object> routeHeadersWithRequestId(
            AgenticCommerceHttpRequest request,
            String operation,
            String allow) {
        Map<String, Object> headers = new LinkedHashMap<>(routeHeaders(operation, allow));
        headers.put(
                AgenticCommerceProtocol.HEADER_REQUEST_ID,
                request.header(AgenticCommerceProtocol.HEADER_REQUEST_ID).orElseGet(() -> generatedRequestId(request)));
        return Map.copyOf(headers);
    }

    private static Optional<String> checkoutSessionId(String routePath) {
        List<String> parts = List.of(normalizePath(routePath).substring(1).split("/"));
        if (parts.size() >= 2 && "checkout_sessions".equals(parts.get(0)) && !parts.get(1).isBlank()) {
            return Optional.of(parts.get(1));
        }
        return Optional.empty();
    }

    private static String normalizePath(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value);
        int queryStart = normalized.indexOf('?');
        if (queryStart >= 0) {
            normalized = normalized.substring(0, queryStart);
        }
        return new AgenticCommerceHttpRequest("GET", normalized, "", Map.of(), Map.of()).path();
    }

    private static int status(String code) {
        return switch (code) {
            case "missing_authorization", "invalid_authorization" -> 401;
            case "method_not_allowed" -> 405;
            case "unsupported_content_type", "missing_content_type" -> 415;
            case "unknown_route" -> 404;
            default -> 400;
        };
    }

    private static boolean accepts(AgenticCommerceHttpRequest request, String responseContentType) {
        String expected = normalizeMediaType(responseContentType);
        String accepted = request.header(AgenticCommerceProtocol.HEADER_ACCEPT).orElse("");
        if (accepted.isBlank()) {
            return true;
        }
        for (String candidate : accepted.split(",")) {
            if (acceptedMediaType(candidate, expected)) {
                return true;
            }
        }
        return false;
    }

    private static boolean acceptedMediaType(String candidate, String expected) {
        String mediaType = normalizeMediaType(candidate);
        if (mediaType.isBlank() || qualityZero(candidate)) {
            return false;
        }
        if ("*/*".equals(mediaType)) {
            return true;
        }
        if (mediaType.endsWith("/*")) {
            return expected.startsWith(mediaType.substring(0, mediaType.length() - 1));
        }
        return mediaType.equals(expected);
    }

    private static boolean qualityZero(String value) {
        if (value == null || !value.contains(";")) {
            return false;
        }
        for (String parameter : value.split(";")) {
            String normalized = parameter.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("q=")) {
                try {
                    return Double.parseDouble(normalized.substring(2).trim()) <= 0.0d;
                } catch (NumberFormatException ignored) {
                    return false;
                }
            }
        }
        return false;
    }

    private static String normalizeMediaType(String value) {
        String normalized = value == null ? "" : value.trim();
        int parameterStart = normalized.indexOf(';');
        if (parameterStart >= 0) {
            normalized = normalized.substring(0, parameterStart);
        }
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private static String generatedRequestId(AgenticCommerceHttpRequest request) {
        return "req_adapter_" + Integer.toUnsignedString(Objects.hash(
                request.method(),
                request.path(),
                request.body()));
    }
}
