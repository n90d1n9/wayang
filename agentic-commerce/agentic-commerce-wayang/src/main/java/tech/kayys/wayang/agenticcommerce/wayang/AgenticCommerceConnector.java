package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpResponder;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;

/**
 * Seller transport boundary for Agentic Commerce Protocol requests.
 */
@FunctionalInterface
public interface AgenticCommerceConnector extends AgenticCommerceCheckoutHttpResponder {

    AgenticCommerceHttpResponse exchange(AgenticCommerceHttpRequest request);

    @Override
    default AgenticCommerceHttpResponse respond(AgenticCommerceHttpRequest request) {
        return exchange(request);
    }
}
