package tech.kayys.wayang.agenticcommerce.core;

import java.util.Map;

/**
 * Map-ready request body contract for Agentic Commerce checkout operations.
 */
public interface AgenticCommerceCheckoutPayload {

    Map<String, Object> toMap();

    default boolean isEmpty() {
        return toMap().isEmpty();
    }
}
