package tech.kayys.wayang.agenticcommerce.core;

/**
 * Stable constants for Wayang's Agentic Commerce Protocol integration.
 */
public final class AgenticCommerceProtocol {

    public static final String SPEC_VERSION = "2026-01-30";
    public static final String SPEC_HOME = "https://www.agenticcommerce.dev";
    public static final String SPEC_GITHUB = "https://github.com/agentic-commerce-protocol/agentic-commerce-protocol";

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_API_VERSION = "API-Version";
    public static final String HEADER_IDEMPOTENCY_KEY = "Idempotency-Key";
    public static final String HEADER_REQUEST_ID = "Request-Id";

    public static final String MIME_JSON = "application/json";
    public static final String BEARER_PREFIX = "Bearer ";

    public static final String PATH_CHECKOUT_SESSIONS = "/checkout_sessions";
    public static final String PATH_CHECKOUT_SESSION = "/checkout_sessions/{checkout_session_id}";
    public static final String PATH_CHECKOUT_SESSION_COMPLETE = "/checkout_sessions/{checkout_session_id}/complete";
    public static final String PATH_CHECKOUT_SESSION_CANCEL = "/checkout_sessions/{checkout_session_id}/cancel";

    public static final String OPERATION_CREATE_CHECKOUT_SESSION = "agenticCommerce.checkoutSession.create";
    public static final String OPERATION_RETRIEVE_CHECKOUT_SESSION = "agenticCommerce.checkoutSession.retrieve";
    public static final String OPERATION_UPDATE_CHECKOUT_SESSION = "agenticCommerce.checkoutSession.update";
    public static final String OPERATION_COMPLETE_CHECKOUT_SESSION = "agenticCommerce.checkoutSession.complete";
    public static final String OPERATION_CANCEL_CHECKOUT_SESSION = "agenticCommerce.checkoutSession.cancel";
    public static final String OPERATION_CHECKOUT_SMOKE = "agenticCommerce.checkout.smoke";

    private AgenticCommerceProtocol() {
    }
}
