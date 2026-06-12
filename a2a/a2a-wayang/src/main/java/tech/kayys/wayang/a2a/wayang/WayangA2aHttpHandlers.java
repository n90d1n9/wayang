package tech.kayys.wayang.a2a.wayang;

import java.util.Map;

/**
 * Convenience composer for A2A HTTP operation handler maps.
 */
public final class WayangA2aHttpHandlers {

    private WayangA2aHttpHandlers() {
    }

    public static Map<String, WayangA2aHttpOperationHandler> merge(
            Map<String, ? extends WayangA2aHttpOperationHandler> first,
            Map<String, ? extends WayangA2aHttpOperationHandler> second) {
        return WayangA2aHttpHandlerMaps.merge(first, second);
    }

    public static Map<String, WayangA2aHttpOperationHandler> forExecution(
            WayangA2aTaskStore store,
            WayangA2aAgentExecutor executor) {
        WayangA2aSendMessageService service = new WayangA2aSendMessageService(store, executor);
        return merge(
                WayangA2aSendMessageHttpHandlers.forService(service),
                WayangA2aTaskHttpHandlers.forStore(store));
    }
}
