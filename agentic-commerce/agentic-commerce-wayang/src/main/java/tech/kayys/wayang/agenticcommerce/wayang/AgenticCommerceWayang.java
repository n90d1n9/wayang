package tech.kayys.wayang.agenticcommerce.wayang;

/**
 * Wayang integration constants for Agentic Commerce Protocol support.
 */
public final class AgenticCommerceWayang {

    public static final String PROTOCOL_ID = "agentic-commerce";

    public static final String METADATA_PROTOCOL = "agenticCommerce.protocol";
    public static final String METADATA_SPEC_VERSION = "agenticCommerce.specVersion";
    public static final String METADATA_OPERATION = "agenticCommerce.operation";
    public static final String METADATA_HTTP_METHOD = "agenticCommerce.httpMethod";
    public static final String METADATA_PATH_TEMPLATE = "agenticCommerce.pathTemplate";
    public static final String METADATA_CONNECTOR = "agenticCommerce.connector";

    public static final String SKILL_CREATE_CHECKOUT = "commerce.checkout.create";
    public static final String SKILL_RETRIEVE_CHECKOUT = "commerce.checkout.retrieve";
    public static final String SKILL_UPDATE_CHECKOUT = "commerce.checkout.update";
    public static final String SKILL_COMPLETE_CHECKOUT = "commerce.checkout.complete";
    public static final String SKILL_CANCEL_CHECKOUT = "commerce.checkout.cancel";

    public static java.util.List<String> checkoutSkillIds() {
        return java.util.List.of(
                SKILL_CREATE_CHECKOUT,
                SKILL_RETRIEVE_CHECKOUT,
                SKILL_UPDATE_CHECKOUT,
                SKILL_COMPLETE_CHECKOUT,
                SKILL_CANCEL_CHECKOUT);
    }

    private AgenticCommerceWayang() {
    }
}
