package tech.kayys.wayang.agenticcommerce.core;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Framework-neutral descriptor for one Agentic Commerce HTTP route.
 */
public record AgenticCommerceHttpRoute(
        String operation,
        String method,
        String pathTemplate,
        boolean requestBodyRequired,
        List<Integer> successStatusCodes) {

    public AgenticCommerceHttpRoute {
        operation = normalizeOperation(operation);
        method = AgenticCommerceHttpRequest.normalizeMethod(method);
        pathTemplate = AgenticCommerceHttpRequest.normalizePath(pathTemplate);
        successStatusCodes = successStatusCodes == null || successStatusCodes.isEmpty()
                ? List.of(200)
                : successStatusCodes.stream()
                        .filter(Objects::nonNull)
                        .map(status -> Math.max(100, status))
                        .distinct()
                        .toList();
    }

    public static AgenticCommerceHttpRoute createCheckoutSession() {
        return new AgenticCommerceHttpRoute(
                AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION,
                "POST",
                AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS,
                true,
                List.of(201));
    }

    public static AgenticCommerceHttpRoute retrieveCheckoutSession() {
        return new AgenticCommerceHttpRoute(
                AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION,
                "GET",
                AgenticCommerceProtocol.PATH_CHECKOUT_SESSION,
                false,
                List.of(200));
    }

    public static AgenticCommerceHttpRoute updateCheckoutSession() {
        return new AgenticCommerceHttpRoute(
                AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION,
                "POST",
                AgenticCommerceProtocol.PATH_CHECKOUT_SESSION,
                true,
                List.of(200));
    }

    public static AgenticCommerceHttpRoute completeCheckoutSession() {
        return new AgenticCommerceHttpRoute(
                AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION,
                "POST",
                AgenticCommerceProtocol.PATH_CHECKOUT_SESSION_COMPLETE,
                true,
                List.of(200));
    }

    public static AgenticCommerceHttpRoute cancelCheckoutSession() {
        return new AgenticCommerceHttpRoute(
                AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION,
                "POST",
                AgenticCommerceProtocol.PATH_CHECKOUT_SESSION_CANCEL,
                false,
                List.of(200));
    }

    public boolean matches(AgenticCommerceHttpRequest request) {
        return method(request) && path(request);
    }

    public boolean method(AgenticCommerceHttpRequest request) {
        return request != null && method.equals(request.method());
    }

    public boolean path(AgenticCommerceHttpRequest request) {
        return request != null && pathMatches(pathTemplate, request.path());
    }

    public boolean operation(String expectedOperation) {
        return operation.equals(normalizeOperation(expectedOperation));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("operation", operation);
        values.put("method", method);
        values.put("pathTemplate", pathTemplate);
        values.put("requestBodyRequired", requestBodyRequired);
        values.put("successStatusCodes", successStatusCodes);
        return AgenticCommerceMaps.copy(values);
    }

    private static boolean pathMatches(String template, String path) {
        List<String> templateParts = parts(template);
        List<String> pathParts = parts(path);
        if (templateParts.size() != pathParts.size()) {
            return false;
        }
        for (int index = 0; index < templateParts.size(); index++) {
            String templatePart = templateParts.get(index);
            String pathPart = pathParts.get(index);
            if (templatePart.startsWith("{") && templatePart.endsWith("}")) {
                if (pathPart.isBlank()) {
                    return false;
                }
            } else if (!templatePart.equals(pathPart)) {
                return false;
            }
        }
        return true;
    }

    private static List<String> parts(String path) {
        String normalized = AgenticCommerceHttpRequest.normalizePath(path);
        return Arrays.stream(normalized.substring(1).split("/"))
                .filter(part -> !part.isBlank())
                .toList();
    }

    private static String normalizeOperation(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Agentic Commerce route operation must not be blank");
        }
        return normalized;
    }
}
