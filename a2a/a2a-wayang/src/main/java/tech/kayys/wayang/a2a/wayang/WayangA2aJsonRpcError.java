package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-RPC error object with standard and A2A-specific codes.
 */
public record WayangA2aJsonRpcError(int code, String message, List<Map<String, Object>> data) {

    public static final int PARSE_ERROR = -32700;
    public static final int INVALID_REQUEST = -32600;
    public static final int METHOD_NOT_FOUND = -32601;
    public static final int INVALID_PARAMS = -32602;
    public static final int INTERNAL_ERROR = -32603;

    public static final int TASK_NOT_FOUND = -32001;
    public static final int PUSH_NOTIFICATION_NOT_SUPPORTED = -32003;
    public static final int UNSUPPORTED_OPERATION = -32004;
    public static final int EXTENDED_AGENT_CARD_NOT_CONFIGURED = -32007;
    public static final int EXTENSION_SUPPORT_REQUIRED = -32008;
    public static final int VERSION_NOT_SUPPORTED = -32009;
    public static final int AUTHENTICATION_REQUIRED = -32010;

    public WayangA2aJsonRpcError {
        message = WayangA2aMaps.required(message, "message");
        data = data == null || data.isEmpty()
                ? List.of()
                : data.stream().map(WayangA2aMaps::copyMap).toList();
    }

    public static WayangA2aJsonRpcError parseError(String message) {
        return new WayangA2aJsonRpcError(PARSE_ERROR, blankFallback(message, "Invalid JSON payload"), List.of());
    }

    public static WayangA2aJsonRpcError invalidRequest(String message) {
        return new WayangA2aJsonRpcError(INVALID_REQUEST, blankFallback(message, "Request payload validation error"),
                List.of());
    }

    public static WayangA2aJsonRpcError methodNotFound(String method) {
        return new WayangA2aJsonRpcError(METHOD_NOT_FOUND, "Method not found: " + method, List.of());
    }

    public static WayangA2aJsonRpcError invalidParams(String message) {
        return new WayangA2aJsonRpcError(INVALID_PARAMS, blankFallback(message, "Invalid parameters"), List.of());
    }

    public static WayangA2aJsonRpcError internalError(String message) {
        return new WayangA2aJsonRpcError(INTERNAL_ERROR, blankFallback(message, "Internal error"), List.of());
    }

    public static WayangA2aJsonRpcError taskNotFound(String taskId) {
        return a2a(TASK_NOT_FOUND, "Task not found", "TASK_NOT_FOUND", metadata("taskId", taskId));
    }

    public static WayangA2aJsonRpcError pushNotificationNotSupported() {
        return a2a(
                PUSH_NOTIFICATION_NOT_SUPPORTED,
                "Push Notification is not supported",
                "PUSH_NOTIFICATION_NOT_SUPPORTED",
                metadata("capability", "pushNotifications"));
    }

    public static WayangA2aJsonRpcError unsupportedOperation(String message) {
        return a2a(UNSUPPORTED_OPERATION, blankFallback(message, "Unsupported operation"),
                "UNSUPPORTED_OPERATION", Map.of());
    }

    public static WayangA2aJsonRpcError extendedAgentCardNotConfigured() {
        return a2a(
                EXTENDED_AGENT_CARD_NOT_CONFIGURED,
                "Extended Agent Card is not configured",
                "EXTENDED_AGENT_CARD_NOT_CONFIGURED",
                metadata("capability", "extendedAgentCard"));
    }

    public static WayangA2aJsonRpcError extensionSupportRequired(
            List<String> missingExtensions,
            List<String> requiredExtensions,
            List<String> providedExtensions) {
        List<String> missing = strings(missingExtensions);
        return a2a(
                EXTENSION_SUPPORT_REQUIRED,
                "A2A request requires extension support: " + String.join(", ", missing) + ".",
                "EXTENSION_SUPPORT_REQUIRED",
                metadata(
                        "missingExtensions", missing,
                        "requiredExtensions", strings(requiredExtensions),
                        "providedExtensions", strings(providedExtensions)));
    }

    public static WayangA2aJsonRpcError authenticationRequired(String message) {
        return a2a(
                AUTHENTICATION_REQUIRED,
                blankFallback(message, "A2A request requires authentication"),
                "AUTHENTICATION_REQUIRED",
                metadata("scheme", "Bearer"));
    }

    public static WayangA2aJsonRpcError versionNotSupported(String requestedVersion) {
        String resolved = blankFallback(requestedVersion, "");
        return a2a(
                VERSION_NOT_SUPPORTED,
                "A2A protocol version is not supported: " + resolved,
                "VERSION_NOT_SUPPORTED",
                metadata(
                        "requestedVersion", resolved,
                        "supportedVersions", List.of(A2aProtocol.VERSION)));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("message", message);
        if (!data.isEmpty()) {
            values.put("data", data);
        }
        return WayangA2aMaps.copyMap(values);
    }

    private static WayangA2aJsonRpcError a2a(
            int code,
            String message,
            String reason,
            Map<String, Object> metadata) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("@type", "type.googleapis.com/google.rpc.ErrorInfo");
        detail.put("reason", reason);
        detail.put("domain", "a2a-protocol.org");
        Map<String, Object> detailMetadata = new LinkedHashMap<>(WayangA2aMaps.copyMap(metadata));
        detailMetadata.put("timestamp", Instant.now().toString());
        detail.put("metadata", WayangA2aMaps.copyMap(detailMetadata));
        return new WayangA2aJsonRpcError(code, message, List.of(WayangA2aMaps.copyMap(detail)));
    }

    private static String blankFallback(String value, String fallback) {
        String normalized = WayangA2aMaps.optional(value);
        return normalized == null ? fallback : normalized;
    }

    private static List<String> strings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(WayangA2aMaps::optional)
                .filter(value -> value != null)
                .distinct()
                .toList();
    }

    private static Map<String, Object> metadata(String key, Object value) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(key, value);
        return WayangA2aMaps.copyMap(metadata);
    }

    private static Map<String, Object> metadata(
            String firstKey,
            Object firstValue,
            String secondKey,
            Object secondValue) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(firstKey, firstValue);
        metadata.put(secondKey, secondValue);
        return WayangA2aMaps.copyMap(metadata);
    }

    private static Map<String, Object> metadata(
            String firstKey,
            Object firstValue,
            String secondKey,
            Object secondValue,
            String thirdKey,
            Object thirdValue) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put(firstKey, firstValue);
        metadata.put(secondKey, secondValue);
        metadata.put(thirdKey, thirdValue);
        return WayangA2aMaps.copyMap(metadata);
    }
}
