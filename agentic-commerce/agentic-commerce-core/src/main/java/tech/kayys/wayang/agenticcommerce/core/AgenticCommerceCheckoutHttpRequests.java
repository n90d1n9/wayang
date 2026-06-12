package tech.kayys.wayang.agenticcommerce.core;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Checkout HTTP request builders for Agentic Commerce Protocol adapters.
 */
public final class AgenticCommerceCheckoutHttpRequests {

    private AgenticCommerceCheckoutHttpRequests() {
    }

    public static AgenticCommerceHttpRequest create(
            AgenticCommerceCreateCheckoutSessionRequest payload,
            AgenticCommerceHttpRequestOptions options) {
        return bodyRequest(
                "POST",
                AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS,
                payload,
                options,
                AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION,
                "");
    }

    public static AgenticCommerceHttpRequest retrieve(
            String checkoutSessionId,
            AgenticCommerceHttpRequestOptions options) {
        String sessionId = requireSessionId(checkoutSessionId);
        AgenticCommerceHttpRequestOptions resolved = options(options);
        return new AgenticCommerceHttpRequest(
                "GET",
                sessionPath(sessionId),
                "",
                resolved.requestHeaders(false),
                attributes(resolved, AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION, sessionId));
    }

    public static AgenticCommerceHttpRequest update(
            String checkoutSessionId,
            AgenticCommerceUpdateCheckoutSessionRequest payload,
            AgenticCommerceHttpRequestOptions options) {
        String sessionId = requireSessionId(checkoutSessionId);
        return bodyRequest(
                "POST",
                sessionPath(sessionId),
                payload,
                options,
                AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION,
                sessionId);
    }

    public static AgenticCommerceHttpRequest complete(
            String checkoutSessionId,
            AgenticCommerceCompleteCheckoutSessionRequest payload,
            AgenticCommerceHttpRequestOptions options) {
        String sessionId = requireSessionId(checkoutSessionId);
        return bodyRequest(
                "POST",
                sessionPath(sessionId) + "/complete",
                payload,
                options,
                AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION,
                sessionId);
    }

    public static AgenticCommerceHttpRequest cancel(
            String checkoutSessionId,
            AgenticCommerceHttpRequestOptions options) {
        return cancel(checkoutSessionId, null, options);
    }

    public static AgenticCommerceHttpRequest cancel(
            String checkoutSessionId,
            AgenticCommerceCancelCheckoutSessionRequest payload,
            AgenticCommerceHttpRequestOptions options) {
        String sessionId = requireSessionId(checkoutSessionId);
        AgenticCommerceHttpRequestOptions resolved = options(options);
        boolean bodyPresent = payload != null && !payload.isEmpty();
        return new AgenticCommerceHttpRequest(
                "POST",
                sessionPath(sessionId) + "/cancel",
                bodyPresent ? AgenticCommerceJson.write(payload.toMap()) : "",
                resolved.requestHeaders(bodyPresent),
                attributes(resolved, AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION, sessionId));
    }

    public static String sessionPath(String checkoutSessionId) {
        return AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS + "/" + pathSegment(requireSessionId(checkoutSessionId));
    }

    private static AgenticCommerceHttpRequest bodyRequest(
            String method,
            String path,
            AgenticCommerceCheckoutPayload payload,
            AgenticCommerceHttpRequestOptions options,
            String operation,
            String checkoutSessionId) {
        AgenticCommerceHttpRequestOptions resolved = options(options);
        Map<String, Object> body = payload == null ? Map.of() : payload.toMap();
        return new AgenticCommerceHttpRequest(
                method,
                path,
                AgenticCommerceJson.write(body),
                resolved.requestHeaders(true),
                attributes(resolved, operation, checkoutSessionId));
    }

    private static Map<String, Object> attributes(
            AgenticCommerceHttpRequestOptions options,
            String operation,
            String checkoutSessionId) {
        Map<String, Object> values = new LinkedHashMap<>(options.attributes());
        values.put("protocol", "agentic-commerce");
        values.put("operation", operation);
        if (!checkoutSessionId.isBlank()) {
            values.put("checkoutSessionId", checkoutSessionId);
        }
        return AgenticCommerceMaps.copy(values);
    }

    private static AgenticCommerceHttpRequestOptions options(AgenticCommerceHttpRequestOptions options) {
        return options == null ? AgenticCommerceHttpRequestOptions.defaults() : options;
    }

    private static String requireSessionId(String checkoutSessionId) {
        String normalized = AgenticCommerceValues.textValue(checkoutSessionId);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Agentic Commerce checkout session id must not be blank");
        }
        return normalized;
    }

    private static String pathSegment(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
