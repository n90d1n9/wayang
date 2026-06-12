package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aProtocol;

import java.util.Objects;
import java.util.Optional;

/**
 * Applies HTTP operation preflight checks before handler execution.
 */
final class WayangA2aHttpOperationPreflightPolicy {

    private final WayangA2aHttpRouteGuard routeGuard;
    private final WayangA2aSendMessagePreflight sendMessagePreflight;
    private final WayangA2aTenantGuard tenantGuard;
    private final WayangA2aCapabilityGuard capabilityGuard;
    private final WayangA2aExtensionNegotiator extensionNegotiator;

    private WayangA2aHttpOperationPreflightPolicy(
            WayangA2aHttpRouteGuard routeGuard,
            WayangA2aSendMessagePreflight sendMessagePreflight,
            WayangA2aTenantGuard tenantGuard,
            WayangA2aCapabilityGuard capabilityGuard,
            WayangA2aExtensionNegotiator extensionNegotiator) {
        this.routeGuard = Objects.requireNonNull(routeGuard, "routeGuard");
        this.sendMessagePreflight = Objects.requireNonNull(sendMessagePreflight, "sendMessagePreflight");
        this.tenantGuard = Objects.requireNonNull(tenantGuard, "tenantGuard");
        this.capabilityGuard = Objects.requireNonNull(capabilityGuard, "capabilityGuard");
        this.extensionNegotiator = Objects.requireNonNull(extensionNegotiator, "extensionNegotiator");
    }

    static WayangA2aHttpOperationPreflightPolicy fromAgentCards(
            A2aAgentCard publicAgentCard,
            A2aAgentCard extendedAgentCard,
            WayangA2aHttpRouteGuard routeGuard) {
        A2aAgentCard resolved = Objects.requireNonNull(publicAgentCard, "publicAgentCard");
        return new WayangA2aHttpOperationPreflightPolicy(
                routeGuard == null ? WayangA2aHttpRouteGuard.strict() : routeGuard,
                WayangA2aSendMessagePreflight.fromAgentCard(resolved),
                WayangA2aTenantGuard.fromAgentCard(resolved),
                WayangA2aCapabilityGuard.forHttp(resolved, extendedAgentCard),
                WayangA2aExtensionNegotiator.fromAgentCard(resolved));
    }

    Result validate(WayangA2aHttpRequest request, A2aHttpRoute route) {
        WayangA2aHttpRequest resolvedRequest = Objects.requireNonNull(request, "request");
        A2aHttpRoute resolvedRoute = Objects.requireNonNull(route, "route");
        Optional<WayangA2aHttpResponse> routeError = routeGuard.validate(resolvedRequest, resolvedRoute);
        if (routeError.isPresent()) {
            return Result.blocked(resolvedRequest, routeError.get());
        }
        WayangA2aHttpRequest preflightRequest =
                sendMessagePreflight.enrichHttp(resolvedRequest, resolvedRoute.operation());
        Optional<WayangA2aHttpResponse> preGuardError = validateTenant(preflightRequest, resolvedRoute)
                .or(() -> validateCapabilities(resolvedRoute));
        if (preGuardError.isPresent()) {
            return Result.blocked(preflightRequest, preGuardError.get());
        }
        WayangA2aSendMessagePreflight.HttpResult sendMessage =
                sendMessagePreflight.validateHttp(preflightRequest, resolvedRoute.operation());
        if (sendMessage.error().isPresent()) {
            return Result.blocked(sendMessage.request(), routeResponse(resolvedRoute, sendMessage.error().get()));
        }
        WayangA2aHttpRequest dispatchRequest = sendMessage.request();
        Optional<WayangA2aHttpResponse> extensionError =
                validateRequiredExtensions(dispatchRequest, resolvedRoute);
        return extensionError
                .map(response -> Result.blocked(dispatchRequest, response))
                .orElseGet(() -> Result.valid(dispatchRequest));
    }

    private Optional<WayangA2aHttpResponse> validateTenant(
            WayangA2aHttpRequest request,
            A2aHttpRoute route) {
        return tenantGuard.validateHttp(request)
                .map(response -> routeResponse(route, response));
    }

    private Optional<WayangA2aHttpResponse> validateCapabilities(A2aHttpRoute route) {
        return capabilityGuard.validateHttp(route.operation())
                .map(response -> routeResponse(route, response));
    }

    private Optional<WayangA2aHttpResponse> validateRequiredExtensions(
            WayangA2aHttpRequest request,
            A2aHttpRoute route) {
        if (builtIn(route.operation())) {
            return Optional.empty();
        }
        return extensionNegotiator.validateHttp(request)
                .map(response -> routeResponse(route, response));
    }

    private WayangA2aHttpResponse routeResponse(A2aHttpRoute route, WayangA2aHttpResponse response) {
        return routeGuard.routeResponse(route, response);
    }

    private static boolean builtIn(String operation) {
        return A2aProtocol.OPERATION_DISCOVER_AGENT_CARD.equals(operation)
                || A2aProtocol.OPERATION_GET_EXTENDED_AGENT_CARD.equals(operation);
    }

    record Result(WayangA2aHttpRequest request, Optional<WayangA2aHttpResponse> error) {

        Result {
            request = Objects.requireNonNull(request, "request");
            error = error == null ? Optional.empty() : error;
        }

        static Result valid(WayangA2aHttpRequest request) {
            return new Result(request, Optional.empty());
        }

        static Result blocked(WayangA2aHttpRequest request, WayangA2aHttpResponse response) {
            return new Result(request, Optional.of(Objects.requireNonNull(response, "response")));
        }
    }
}
