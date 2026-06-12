package tech.kayys.wayang.a2a.wayang;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

record WayangA2aJsonRpcHttpRouteHandlers(
        WayangA2aJsonRpcDispatcher dispatcher,
        WayangA2aExtensionNegotiator extensionNegotiator,
        WayangA2aExtendedAgentCardAuthorizer extendedAgentCardAuthorizer,
        WayangA2aJsonRpcHttpDiagnosticHandlers diagnosticHandlers) {

    WayangA2aJsonRpcHttpRouteHandlers {
        dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        extensionNegotiator = Objects.requireNonNull(extensionNegotiator, "extensionNegotiator");
        extendedAgentCardAuthorizer = extendedAgentCardAuthorizer == null
                ? WayangA2aExtendedAgentCardAuthorizer.allowAll()
                : extendedAgentCardAuthorizer;
        diagnosticHandlers = Objects.requireNonNull(diagnosticHandlers, "diagnosticHandlers");
    }

    WayangA2aHttpResponse dispatch(
            WayangA2aJsonRpcHttpRouteDescriptor route,
            WayangA2aHttpRequest request) {
        if (WayangA2aJsonRpcHttpRouteDescriptor.KEY_ENDPOINT.equals(route.key())) {
            return dispatchEndpoint(request, route);
        }
        return dispatchDiagnostic(request, route, diagnosticHandlers.responseSupplier(route));
    }

    private WayangA2aHttpResponse dispatchEndpoint(
            WayangA2aHttpRequest request,
            WayangA2aJsonRpcHttpRouteDescriptor route) {
        WayangA2aJsonRpcHttpRequestContext context = WayangA2aJsonRpcHttpRequestContext.from(request);
        Optional<WayangA2aHttpResponse> invalid = WayangA2aJsonRpcEndpointRouteGuard.validate(
                request,
                route,
                context);
        if (invalid.isPresent()) {
            return invalid.orElseThrow();
        }
        Optional<WayangA2aHttpResponse> blocked = WayangA2aJsonRpcEndpointPreflightGuard.validate(
                request,
                route,
                context,
                extensionNegotiator,
                extendedAgentCardAuthorizer);
        if (blocked.isPresent()) {
            return blocked.orElseThrow();
        }
        String operation = context.methodOr(route.operation());
        return dispatcher.dispatchJson(request.body())
                .withHeaders(WayangA2aJsonRpcHttpResponses.routeHeaders(operation, route.allow()));
    }

    private static WayangA2aHttpResponse dispatchDiagnostic(
            WayangA2aHttpRequest request,
            WayangA2aJsonRpcHttpRouteDescriptor route,
            Supplier<WayangA2aHttpResponse> responseSupplier) {
        return WayangA2aJsonRpcDiagnosticRouteGuard.dispatchGetJson(
                request,
                route.path(),
                route.allow(),
                route.operation(),
                route.routeName(),
                responseSupplier);
    }
}
