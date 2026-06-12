package tech.kayys.wayang.a2a.wayang;

/**
 * Contributes one named JSON-RPC method handler group to the dispatch registry.
 */
interface WayangA2aJsonRpcMethodHandlerProvider {

    WayangA2aJsonRpcMethodHandlerGroup group();

    default WayangA2aJsonRpcMethodHandlerContribution contribution() {
        return group().contribution();
    }
}
