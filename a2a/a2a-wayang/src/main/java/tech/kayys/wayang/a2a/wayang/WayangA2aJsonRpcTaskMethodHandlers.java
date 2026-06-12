package tech.kayys.wayang.a2a.wayang;

import java.util.Map;
import java.util.Objects;

/**
 * JSON-RPC handlers for task lifecycle methods.
 */
final class WayangA2aJsonRpcTaskMethodHandlers implements WayangA2aJsonRpcMethodHandlerProvider {

    private final WayangA2aTaskStore store;

    private WayangA2aJsonRpcTaskMethodHandlers(WayangA2aTaskStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    static WayangA2aJsonRpcTaskMethodHandlers fromStore(WayangA2aTaskStore store) {
        return new WayangA2aJsonRpcTaskMethodHandlers(store);
    }

    Map<String, WayangA2aJsonRpcMethodDispatchTable.Handler> handlers() {
        return WayangA2aJsonRpcMethodHandlerMaps.builder()
                .putAll(WayangA2aJsonRpcTaskQueryMethodHandlers.fromStore(store).handlers())
                .putAll(WayangA2aJsonRpcTaskCancelMethodHandlers.fromStore(store).handlers())
                .putAll(WayangA2aJsonRpcTaskSubscriptionMethodHandlers.fromStore(store).handlers())
                .putAll(WayangA2aJsonRpcPushConfigMethodHandlers.fromStore(store).handlers())
                .build();
    }

    @Override
    public WayangA2aJsonRpcMethodHandlerGroup group() {
        return WayangA2aJsonRpcMethodHandlerGroup.of(
                WayangA2aJsonRpcCoreMethodHandlerContributions.GROUP_TASK,
                handlers(),
                WayangA2aJsonRpcCoreMethodHandlerContributions.task());
    }
}
