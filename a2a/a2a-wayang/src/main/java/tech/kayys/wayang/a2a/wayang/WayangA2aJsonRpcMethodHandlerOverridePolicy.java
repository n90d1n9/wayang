package tech.kayys.wayang.a2a.wayang;

/**
 * Policy for duplicate JSON-RPC method handlers contributed by registry groups.
 */
enum WayangA2aJsonRpcMethodHandlerOverridePolicy {

    ALLOW_REPLACE,
    REJECT_DUPLICATES;

    void validate(WayangA2aJsonRpcMethodHandlerOverride override) {
        if (this == REJECT_DUPLICATES) {
            throw new IllegalStateException("Duplicate A2A JSON-RPC method handler for "
                    + override.method()
                    + ": "
                    + override.originalGroup()
                    + " -> "
                    + override.replacementGroup());
        }
    }
}
