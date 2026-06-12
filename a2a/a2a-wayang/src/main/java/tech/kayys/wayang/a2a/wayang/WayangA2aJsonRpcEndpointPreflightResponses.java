package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds JSON-RPC endpoint preflight rejection responses.
 */
final class WayangA2aJsonRpcEndpointPreflightResponses {

    private WayangA2aJsonRpcEndpointPreflightResponses() {
    }

    static WayangA2aHttpResponse versionNotSupported(
            WayangA2aJsonRpcHttpRequestContext context,
            String requestedVersion,
            WayangA2aJsonRpcHttpRouteDescriptor route) {
        return jsonRpcError(
                400,
                route,
                context,
                WayangA2aJsonRpcError.versionNotSupported(requestedVersion));
    }

    static WayangA2aHttpResponse extensionSupportRequired(
            WayangA2aHttpRequest request,
            WayangA2aJsonRpcHttpRequestContext context,
            List<String> missingExtensions,
            WayangA2aJsonRpcHttpRouteDescriptor route,
            WayangA2aExtensionNegotiator extensionNegotiator) {
        WayangA2aExtensionNegotiator resolvedNegotiator =
                Objects.requireNonNull(extensionNegotiator, "extensionNegotiator");
        return jsonRpcError(
                400,
                route,
                context,
                WayangA2aJsonRpcError.extensionSupportRequired(
                        missingExtensions,
                        resolvedNegotiator.requiredExtensions(),
                        resolvedNegotiator.requestedExtensions(request)))
                .withHeaders(requiredExtensionHeaders(resolvedNegotiator));
    }

    static WayangA2aHttpResponse extendedAgentCardUnauthorized(
            WayangA2aJsonRpcHttpRequestContext context,
            WayangA2aJsonRpcHttpRouteDescriptor route) {
        return WayangA2aJsonRpcHttpResponses.jsonRpcError(
                401,
                A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD,
                Objects.requireNonNull(route, "route").allow(),
                jsonRpcId(context),
                WayangA2aJsonRpcError.authenticationRequired(
                        "A2A extended Agent Card requires authorization."))
                .withHeaders(authenticationHeaders());
    }

    private static WayangA2aHttpResponse jsonRpcError(
            int statusCode,
            WayangA2aJsonRpcHttpRouteDescriptor route,
            WayangA2aJsonRpcHttpRequestContext context,
            WayangA2aJsonRpcError error) {
        WayangA2aJsonRpcHttpRouteDescriptor resolvedRoute = Objects.requireNonNull(route, "route");
        return WayangA2aJsonRpcHttpResponses.jsonRpcError(
                statusCode,
                resolvedRoute.operation(),
                resolvedRoute.allow(),
                jsonRpcId(context),
                error);
    }

    private static Object jsonRpcId(WayangA2aJsonRpcHttpRequestContext context) {
        return Objects.requireNonNull(context, "context").id().orElse(null);
    }

    private static Map<String, Object> requiredExtensionHeaders(WayangA2aExtensionNegotiator extensionNegotiator) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(
                WayangA2aHttpResponse.HEADER_A2A_EXTENSIONS,
                extensionNegotiator.requiredExtensionsHeader());
        return WayangA2aMaps.copyMap(headers);
    }

    private static Map<String, Object> authenticationHeaders() {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(
                WayangA2aHttpResponse.HEADER_WWW_AUTHENTICATE,
                "Bearer realm=\"" + WayangA2aExtendedAgentCardAuthorizer.BEARER_REALM + "\"");
        return WayangA2aMaps.copyMap(headers);
    }
}
