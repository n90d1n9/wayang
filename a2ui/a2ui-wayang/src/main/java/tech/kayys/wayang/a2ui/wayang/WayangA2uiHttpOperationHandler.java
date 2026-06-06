package tech.kayys.wayang.a2ui.wayang;

/**
 * Handles one validated A2UI HTTP route operation.
 */
@FunctionalInterface
public interface WayangA2uiHttpOperationHandler {

    WayangA2uiBridgeResponse handle(WayangA2uiHttpRequest request, WayangA2uiHttpRoute route);
}
