package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Protocol and access preflight for JSON-RPC endpoint requests.
 */
final class WayangA2aJsonRpcEndpointPreflightGuard {

    private WayangA2aJsonRpcEndpointPreflightGuard() {
    }

    static Optional<WayangA2aHttpResponse> validate(
            WayangA2aHttpRequest request,
            WayangA2aJsonRpcHttpRouteDescriptor route,
            WayangA2aJsonRpcHttpRequestContext context,
            WayangA2aExtensionNegotiator extensionNegotiator,
            WayangA2aExtendedAgentCardAuthorizer extendedAgentCardAuthorizer) {
        WayangA2aHttpRequest resolved = Objects.requireNonNull(request, "request");
        WayangA2aJsonRpcHttpRouteDescriptor resolvedRoute = Objects.requireNonNull(route, "route");
        WayangA2aJsonRpcHttpRequestContext resolvedContext = Objects.requireNonNull(context, "context");
        WayangA2aExtensionNegotiator resolvedNegotiator =
                Objects.requireNonNull(extensionNegotiator, "extensionNegotiator");
        WayangA2aExtendedAgentCardAuthorizer resolvedAuthorizer =
                extendedAgentCardAuthorizer == null
                        ? WayangA2aExtendedAgentCardAuthorizer.allowAll()
                        : extendedAgentCardAuthorizer;

        Optional<String> requestedVersion = resolved.header(A2aProtocol.HEADER_VERSION);
        if (requestedVersion.isPresent() && !A2aProtocol.VERSION.equals(requestedVersion.get())) {
            return Optional.of(WayangA2aJsonRpcEndpointPreflightResponses.versionNotSupported(
                    resolvedContext,
                    requestedVersion.get(),
                    resolvedRoute));
        }

        String operation = resolvedContext.methodOr(resolvedRoute.operation());
        if (!A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD.equals(operation)) {
            List<String> missingExtensions = resolvedNegotiator.missingRequiredExtensions(resolved);
            if (!missingExtensions.isEmpty()) {
                return Optional.of(WayangA2aJsonRpcEndpointPreflightResponses.extensionSupportRequired(
                        resolved,
                        resolvedContext,
                        missingExtensions,
                        resolvedRoute,
                        resolvedNegotiator));
            }
        }
        if (A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD.equals(operation)
                && resolvedAuthorizer.authorize(resolved).isPresent()) {
            return Optional.of(WayangA2aJsonRpcEndpointPreflightResponses.extendedAgentCardUnauthorized(
                    resolvedContext,
                    resolvedRoute));
        }
        return Optional.empty();
    }
}
