package tech.kayys.wayang.agenticcommerce.core;

/**
 * Minimal transport boundary for Agentic Commerce checkout HTTP harnesses.
 */
@FunctionalInterface
public interface AgenticCommerceCheckoutHttpResponder {

    AgenticCommerceHttpResponse respond(AgenticCommerceHttpRequest request);
}
