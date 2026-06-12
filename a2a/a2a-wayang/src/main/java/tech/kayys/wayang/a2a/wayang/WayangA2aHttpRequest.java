package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Dependency-free HTTP-shaped request for A2A bindings.
 */
public record WayangA2aHttpRequest(
        String method,
        String path,
        String body,
        Map<String, Object> headers,
        Map<String, Object> attributes) {

    public WayangA2aHttpRequest {
        method = normalizeMethod(method);
        path = normalizePath(path);
        body = body == null ? "" : body;
        headers = WayangA2aMaps.copyMap(headers);
        attributes = WayangA2aMaps.copyMap(attributes);
    }

    public static WayangA2aHttpRequest get(String path) {
        return new WayangA2aHttpRequest("GET", path, "", Map.of(), Map.of());
    }

    public static WayangA2aHttpRequest postJson(String path, String body) {
        return new WayangA2aHttpRequest(
                "POST",
                path,
                body,
                Map.of(WayangA2aHttpResponse.HEADER_CONTENT_TYPE, A2aProtocol.MEDIA_TYPE),
                Map.of());
    }

    public static WayangA2aHttpRequest sendMessage(String body) {
        return postJson("/message:send", body);
    }

    public static WayangA2aHttpRequest streamMessage(String body) {
        return postJson("/message:stream", body);
    }

    public WayangA2aHttpRequest withAttribute(String key, Object value) {
        String normalizedKey = WayangA2aMaps.required(key, "key");
        Map<String, Object> updated = new LinkedHashMap<>(attributes);
        if (value == null) {
            updated.remove(normalizedKey);
        } else {
            updated.put(normalizedKey, value);
        }
        return new WayangA2aHttpRequest(method, path, body, headers, updated);
    }

    public A2aSendMessageRequest sendMessageRequest() {
        Object cached = attributes.get(WayangA2a.SEND_MESSAGE_REQUEST_ATTRIBUTE);
        if (cached instanceof A2aSendMessageRequest request) {
            return request;
        }
        return A2aSendMessageRequest.fromJson(body);
    }

    public boolean method(String expectedMethod) {
        return method.equals(normalizeMethod(expectedMethod));
    }

    public Optional<String> header(String name) {
        String normalizedName = normalizeHeaderName(name);
        if (normalizedName.isBlank()) {
            return Optional.empty();
        }
        return headers.entrySet().stream()
                .filter(entry -> normalizeHeaderName(entry.getKey()).equals(normalizedName))
                .map(Map.Entry::getValue)
                .filter(value -> value != null && !String.valueOf(value).isBlank())
                .map(value -> String.valueOf(value).trim())
                .findFirst();
    }

    public String contentType() {
        return header(WayangA2aHttpResponse.HEADER_CONTENT_TYPE).orElse("");
    }

    public boolean jsonContentType() {
        String normalized = normalizeMediaType(contentType());
        return normalized.isBlank()
                || normalized.equals("application/json")
                || normalized.equals(A2aProtocol.MEDIA_TYPE);
    }

    public String accept() {
        return header(WayangA2aHttpResponse.HEADER_ACCEPT).orElse("");
    }

    public boolean accepts(String responseContentType) {
        String expected = normalizeMediaType(responseContentType);
        if (expected.isBlank()) {
            return false;
        }
        String accepted = accept();
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

    static String normalizeMethod(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.isBlank() ? "GET" : normalized.toUpperCase(Locale.ROOT);
    }

    static String normalizePath(String value) {
        String normalized = value == null ? "" : value.trim();
        int queryStart = normalized.indexOf('?');
        if (queryStart >= 0) {
            normalized = normalized.substring(0, queryStart);
        }
        if (normalized.isBlank()) {
            return "/";
        }
        return normalized.startsWith("/") ? normalized : "/" + normalized;
    }

    private static String normalizeHeaderName(Object value) {
        return value == null ? "" : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeMediaType(String value) {
        String normalized = value == null ? "" : value.trim();
        int parameterStart = normalized.indexOf(';');
        if (parameterStart >= 0) {
            normalized = normalized.substring(0, parameterStart);
        }
        return normalized.trim().toLowerCase(Locale.ROOT);
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
            String prefix = mediaType.substring(0, mediaType.length() - 1);
            return expected.startsWith(prefix);
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
}
