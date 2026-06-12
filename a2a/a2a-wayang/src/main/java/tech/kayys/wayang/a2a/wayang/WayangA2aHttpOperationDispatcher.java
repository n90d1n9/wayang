package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;
import tech.kayys.wayang.a2a.core.A2aHttpRoute;
import tech.kayys.wayang.a2a.core.A2aHttpRouteCatalog;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Dispatches dependency-free A2A HTTP requests to registered operation handlers.
 */
public final class WayangA2aHttpOperationDispatcher {

    private final WayangA2aHttpRouteMatcher matcher;
    private final WayangA2aHttpRouteResponsePolicy routeResponses;
    private final WayangA2aHttpAgentCardResponder agentCardResponder;
    private final WayangA2aHttpOperationPreflightPolicy operationPreflightPolicy;
    private final WayangA2aHttpOperationHandlerExecutor handlerExecutor;

    public WayangA2aHttpOperationDispatcher(A2aAgentCard publicAgentCard) {
        this(publicAgentCard, null, Map.of(), A2aHttpRouteCatalog.standard(), WayangA2aHttpRouteGuard.strict());
    }

    public WayangA2aHttpOperationDispatcher(
            A2aAgentCard publicAgentCard,
            Map<String, ? extends WayangA2aHttpOperationHandler> handlers) {
        this(publicAgentCard, null, handlers, A2aHttpRouteCatalog.standard(), WayangA2aHttpRouteGuard.strict());
    }

    public WayangA2aHttpOperationDispatcher(
            A2aAgentCard publicAgentCard,
            A2aAgentCard extendedAgentCard,
            Map<String, ? extends WayangA2aHttpOperationHandler> handlers,
            A2aHttpRouteCatalog routeCatalog,
            WayangA2aHttpRouteGuard guard) {
        this(
                publicAgentCard,
                extendedAgentCard,
                handlers,
                routeCatalog,
                guard,
                WayangA2aExtendedAgentCardAuthorizer.allowAll());
    }

    public WayangA2aHttpOperationDispatcher(
            A2aAgentCard publicAgentCard,
            A2aAgentCard extendedAgentCard,
            Map<String, ? extends WayangA2aHttpOperationHandler> handlers,
            A2aHttpRouteCatalog routeCatalog,
            WayangA2aHttpRouteGuard guard,
            WayangA2aExtendedAgentCardAuthorizer extendedAgentCardAuthorizer) {
        A2aAgentCard resolvedPublicAgentCard = Objects.requireNonNull(publicAgentCard, "publicAgentCard");
        this.matcher = new WayangA2aHttpRouteMatcher(routeCatalog);
        WayangA2aHttpRouteGuard resolvedGuard = guard == null ? WayangA2aHttpRouteGuard.strict() : guard;
        this.routeResponses = WayangA2aHttpRouteResponsePolicy.from(this.matcher, resolvedGuard);
        this.agentCardResponder = WayangA2aHttpAgentCardResponder.fromAgentCards(
                resolvedPublicAgentCard,
                extendedAgentCard,
                extendedAgentCardAuthorizer);
        this.operationPreflightPolicy = WayangA2aHttpOperationPreflightPolicy.fromAgentCards(
                resolvedPublicAgentCard,
                extendedAgentCard,
                resolvedGuard);
        this.handlerExecutor = WayangA2aHttpOperationHandlerExecutor.fromHandlers(handlers, resolvedGuard);
    }

    public List<String> operations() {
        Map<String, Boolean> operations = new LinkedHashMap<>();
        agentCardResponder.operations().forEach(operation -> operations.put(operation, true));
        handlerExecutor.operations().forEach(operation -> operations.put(operation, true));
        return List.copyOf(operations.keySet());
    }

    public boolean supports(A2aHttpRoute route) {
        if (route == null) {
            return false;
        }
        return agentCardResponder.supports(route.operation()) || handlerExecutor.supports(route.operation());
    }

    public WayangA2aHttpResponse dispatch(WayangA2aHttpRequest request) {
        WayangA2aHttpRequest resolvedRequest = Objects.requireNonNull(request, "request");
        if (resolvedRequest.method("OPTIONS")) {
            return dispatchOptions(resolvedRequest);
        }
        return matcher.match(resolvedRequest)
                .map(match -> dispatchMatched(resolvedRequest, match))
                .orElseGet(() -> routeResponses.unmatched(resolvedRequest));
    }

    public WayangA2aHttpResponse dispatchOptions(WayangA2aHttpRequest request) {
        return routeResponses.options(request);
    }

    private WayangA2aHttpResponse dispatchMatched(
            WayangA2aHttpRequest request,
            WayangA2aHttpRouteMatch match) {
        A2aHttpRoute route = match.route();
        WayangA2aHttpOperationPreflightPolicy.Result preflight =
                operationPreflightPolicy.validate(request, route);
        if (preflight.error().isPresent()) {
            return routeResponses.withAllow(preflight.error().get(), route);
        }
        return routeResponses.withAllow(dispatchValidated(preflight.request(), match), route);
    }

    private WayangA2aHttpResponse dispatchValidated(
            WayangA2aHttpRequest request,
            WayangA2aHttpRouteMatch match) {
        A2aHttpRoute route = match.route();
        Optional<WayangA2aHttpResponse> agentCardResponse =
                agentCardResponder.respond(request, route.operation());
        if (agentCardResponse.isPresent()) {
            return routeResponses.routeResponse(route, agentCardResponse.get());
        }
        return handlerExecutor.execute(request, match);
    }

}
