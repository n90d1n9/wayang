package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCancelCheckoutSessionRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpResponses;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpSmokeResult;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpSmokeRunner;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpRequests;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCompleteCheckoutSessionRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCreateCheckoutSessionRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequestOptions;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceUpdateCheckoutSessionRequest;

import java.util.Map;
import java.util.Objects;

/**
 * Wayang-facing checkout service backed by an Agentic Commerce connector.
 */
public final class AgenticCommerceCheckoutService {

    private final AgenticCommerceConnector connector;
    private final AgenticCommerceConnectorConfig config;

    public AgenticCommerceCheckoutService(AgenticCommerceConnector connector) {
        this(connector, AgenticCommerceConnectorConfig.defaults());
    }

    public AgenticCommerceCheckoutService(
            AgenticCommerceConnector connector,
            AgenticCommerceConnectorConfig config) {
        this.connector = Objects.requireNonNull(connector, "connector");
        this.config = config == null ? AgenticCommerceConnectorConfig.defaults() : config;
    }

    public AgenticCommerceCheckoutHttpResult create(AgenticCommerceCreateCheckoutSessionRequest payload) {
        return checkout(AgenticCommerceCheckoutHttpRequests.create(payload, options()));
    }

    public AgenticCommerceCheckoutHttpResult retrieve(String checkoutSessionId) {
        return checkout(AgenticCommerceCheckoutHttpRequests.retrieve(checkoutSessionId, options()));
    }

    public AgenticCommerceCheckoutHttpResult update(
            String checkoutSessionId,
            AgenticCommerceUpdateCheckoutSessionRequest payload) {
        return checkout(AgenticCommerceCheckoutHttpRequests.update(checkoutSessionId, payload, options()));
    }

    public AgenticCommerceCheckoutHttpResult complete(
            String checkoutSessionId,
            AgenticCommerceCompleteCheckoutSessionRequest payload) {
        return checkout(AgenticCommerceCheckoutHttpRequests.complete(checkoutSessionId, payload, options()));
    }

    public AgenticCommerceCheckoutHttpResult cancel(String checkoutSessionId) {
        return checkout(AgenticCommerceCheckoutHttpRequests.cancel(checkoutSessionId, options()));
    }

    public AgenticCommerceCheckoutHttpResult cancel(
            String checkoutSessionId,
            AgenticCommerceCancelCheckoutSessionRequest payload) {
        return checkout(AgenticCommerceCheckoutHttpRequests.cancel(checkoutSessionId, payload, options()));
    }

    public AgenticCommerceCheckoutHttpSmokeResult smoke() {
        return AgenticCommerceCheckoutHttpSmokeRunner.checkout().run(connector);
    }

    public AgenticCommerceConnectorConfig config() {
        return config;
    }

    private AgenticCommerceCheckoutHttpResult checkout(AgenticCommerceHttpRequest request) {
        AgenticCommerceHttpResponse response = connector.exchange(request);
        return AgenticCommerceCheckoutHttpResponses.decode(request, response);
    }

    private AgenticCommerceHttpRequestOptions options() {
        return config.requestOptions().withAttributes(Map.of(
                AgenticCommerceWayang.METADATA_PROTOCOL,
                AgenticCommerceWayang.PROTOCOL_ID));
    }
}
