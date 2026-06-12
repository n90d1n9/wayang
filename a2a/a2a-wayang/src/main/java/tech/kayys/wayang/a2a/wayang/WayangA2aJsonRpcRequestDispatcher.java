package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aAgentCard;

import java.util.List;
import java.util.Objects;

/**
 * Dispatches validated JSON-RPC request envelopes to registered method handlers.
 */
final class WayangA2aJsonRpcRequestDispatcher {

    private final WayangA2aJsonRpcMethodDispatchTable methodDispatchTable;
    private final WayangA2aJsonRpcMethodPreflightPolicy methodPreflightPolicy;
    private final WayangA2aJsonRpcMethodHandlerExecutor methodHandlerExecutor;
    private final WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodHandlerRegistrySnapshot;

    private WayangA2aJsonRpcRequestDispatcher(
            WayangA2aJsonRpcMethodDispatchTable methodDispatchTable,
            WayangA2aJsonRpcMethodPreflightPolicy methodPreflightPolicy,
            WayangA2aJsonRpcMethodHandlerExecutor methodHandlerExecutor,
            WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodHandlerRegistrySnapshot) {
        this.methodDispatchTable = Objects.requireNonNull(methodDispatchTable, "methodDispatchTable");
        this.methodPreflightPolicy = Objects.requireNonNull(methodPreflightPolicy, "methodPreflightPolicy");
        this.methodHandlerExecutor = Objects.requireNonNull(methodHandlerExecutor, "methodHandlerExecutor");
        this.methodHandlerRegistrySnapshot = methodHandlerRegistrySnapshot == null
                ? WayangA2aJsonRpcMethodHandlerRegistrySnapshot.from(null)
                : methodHandlerRegistrySnapshot;
    }

    static WayangA2aJsonRpcRequestDispatcher fromAgentCard(
            A2aAgentCard extendedAgentCard,
            WayangA2aTaskStore store,
            WayangA2aSendMessageService sendMessageService) {
        return fromAgentCard(extendedAgentCard, store, sendMessageService, List.of());
    }

    static WayangA2aJsonRpcRequestDispatcher fromAgentCard(
            A2aAgentCard extendedAgentCard,
            WayangA2aTaskStore store,
            WayangA2aSendMessageService sendMessageService,
            List<? extends WayangA2aJsonRpcMethodHandlerProvider> extraProviders) {
        A2aAgentCard resolvedAgentCard = Objects.requireNonNull(extendedAgentCard, "extendedAgentCard");
        WayangA2aJsonRpcMethodHandlerRegistry registry =
                WayangA2aJsonRpcMethodDispatchTableFactory.coreHandlerRegistry(
                        resolvedAgentCard,
                        store,
                        sendMessageService,
                        extraProviders);
        return create(
                WayangA2aJsonRpcMethodDispatchTableFactory.create(registry),
                WayangA2aJsonRpcMethodPreflightPolicy.fromAgentCard(resolvedAgentCard),
                WayangA2aJsonRpcMethodHandlerExecutor.create(),
                WayangA2aJsonRpcMethodHandlerRegistrySnapshot.from(registry));
    }

    static WayangA2aJsonRpcRequestDispatcher create(
            WayangA2aJsonRpcMethodDispatchTable methodDispatchTable,
            WayangA2aJsonRpcMethodPreflightPolicy methodPreflightPolicy,
            WayangA2aJsonRpcMethodHandlerExecutor methodHandlerExecutor) {
        return create(methodDispatchTable, methodPreflightPolicy, methodHandlerExecutor, null);
    }

    static WayangA2aJsonRpcRequestDispatcher create(
            WayangA2aJsonRpcMethodDispatchTable methodDispatchTable,
            WayangA2aJsonRpcMethodPreflightPolicy methodPreflightPolicy,
            WayangA2aJsonRpcMethodHandlerExecutor methodHandlerExecutor,
            WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodHandlerRegistrySnapshot) {
        return new WayangA2aJsonRpcRequestDispatcher(
                methodDispatchTable,
                methodPreflightPolicy,
                methodHandlerExecutor,
                methodHandlerRegistrySnapshot);
    }

    List<String> methods() {
        return methodDispatchTable.methods();
    }

    WayangA2aJsonRpcMethodDispatchCoverage coverage() {
        return methodDispatchTable.coverage();
    }

    WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodHandlerRegistrySnapshot() {
        return methodHandlerRegistrySnapshot;
    }

    WayangA2aHttpResponse dispatch(WayangA2aJsonRpcRequest request) {
        WayangA2aJsonRpcRequest resolved = Objects.requireNonNull(request, "request");
        WayangA2aJsonRpcMethodDispatchTable.Entry entry = methodDispatchTable.entry(resolved.method()).orElse(null);
        if (entry == null) {
            return error(resolved, WayangA2aJsonRpcError.methodNotFound(resolved.method()));
        }
        WayangA2aJsonRpcMethodPreflightPolicy.Result preflight =
                methodPreflightPolicy.validate(resolved, entry);
        if (preflight.error().isPresent()) {
            return error(resolved, preflight.error().orElseThrow());
        }
        return methodHandlerExecutor.execute(resolved, entry, preflight.sendMessage());
    }

    private static WayangA2aHttpResponse error(
            WayangA2aJsonRpcRequest request,
            WayangA2aJsonRpcError error) {
        return WayangA2aJsonRpcHttpResponses.jsonRpc(WayangA2aJsonRpcResponse.error(
                request.id(),
                error));
    }
}
