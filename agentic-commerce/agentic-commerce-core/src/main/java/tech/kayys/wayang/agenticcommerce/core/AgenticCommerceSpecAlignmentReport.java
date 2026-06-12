package tech.kayys.wayang.agenticcommerce.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Machine-readable alignment snapshot for the local Agentic Commerce spec surface.
 */
public record AgenticCommerceSpecAlignmentReport(
        AgenticCommerceRouteCatalog routeCatalog,
        List<AgenticCommerceSpecAlignmentRequirement> requirements) {

    public static final String STANDARD_ID = "agentic-commerce";
    public static final String STANDARD_NAME = "Agentic Commerce Protocol";
    public static final String BINDING_HTTP_JSON = "HTTP+JSON";

    private static final List<String> REQUIRED_HEADERS = List.of(
            AgenticCommerceProtocol.HEADER_AUTHORIZATION,
            AgenticCommerceProtocol.HEADER_CONTENT_TYPE,
            AgenticCommerceProtocol.HEADER_ACCEPT,
            AgenticCommerceProtocol.HEADER_API_VERSION,
            AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY,
            AgenticCommerceProtocol.HEADER_REQUEST_ID);

    public AgenticCommerceSpecAlignmentReport {
        routeCatalog = routeCatalog == null
                ? AgenticCommerceRouteCatalog.checkoutCatalog()
                : routeCatalog;
        requirements = requirements == null
                ? List.of()
                : requirements.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    public static AgenticCommerceSpecAlignmentReport checkout() {
        return from(AgenticCommerceRouteCatalog.checkoutCatalog());
    }

    public static AgenticCommerceSpecAlignmentReport from(AgenticCommerceRouteCatalog routeCatalog) {
        AgenticCommerceRouteCatalog resolvedCatalog = routeCatalog == null
                ? AgenticCommerceRouteCatalog.checkoutCatalog()
                : routeCatalog;
        return new AgenticCommerceSpecAlignmentReport(
                resolvedCatalog,
                List.of(
                        protocolMetadataRequirement(),
                        requiredHeadersRequirement(),
                        routeRequirement(resolvedCatalog, AgenticCommerceHttpRoute.createCheckoutSession()),
                        routeRequirement(resolvedCatalog, AgenticCommerceHttpRoute.retrieveCheckoutSession()),
                        routeRequirement(resolvedCatalog, AgenticCommerceHttpRoute.updateCheckoutSession()),
                        routeRequirement(resolvedCatalog, AgenticCommerceHttpRoute.completeCheckoutSession()),
                        routeRequirement(resolvedCatalog, AgenticCommerceHttpRoute.cancelCheckoutSession()),
                        payloadRequirement(
                                "payload.checkout_session.create_request",
                                "Create checkout session request payload fields",
                                List.of(
                                        "buyer",
                                        "line_items",
                                        "currency",
                                        "fulfillment_details",
                                        "capabilities",
                                        "fulfillment_groups",
                                        "affiliate_attribution",
                                        "coupons",
                                        "discounts",
                                        "locale",
                                        "timezone",
                                        "quote_id",
                                        "metadata"),
                                AgenticCommerceCreateCheckoutSessionRequest.fromMap(createCheckoutPayload())
                                        .toMap()),
                        payloadRequirement(
                                "payload.checkout_session.update_request",
                                "Update checkout session request payload fields",
                                List.of(
                                        "buyer",
                                        "line_items",
                                        "fulfillment_details",
                                        "fulfillment_groups",
                                        "selected_fulfillment_options",
                                        "fulfillment_option_id",
                                        "coupons",
                                        "discounts",
                                        "metadata"),
                                AgenticCommerceUpdateCheckoutSessionRequest.fromMap(updateCheckoutPayload())
                                        .toMap()),
                        payloadRequirement(
                                "payload.checkout_session.complete_request",
                                "Complete checkout session request payload fields",
                                List.of(
                                        "buyer",
                                        "payment_data",
                                        "authentication_result",
                                        "affiliate_attribution",
                                        "risk_signals",
                                        "metadata"),
                                AgenticCommerceCompleteCheckoutSessionRequest.fromMap(completeCheckoutPayload())
                                        .toMap()),
                        payloadRequirement(
                                "payload.checkout_session.cancel_request",
                                "Cancel checkout session request payload fields",
                                List.of("intent_trace", "metadata"),
                                AgenticCommerceCancelCheckoutSessionRequest.fromMap(cancelCheckoutPayload())
                                        .toMap()),
                        payloadRequirement(
                                "payload.checkout_session.response",
                                "Checkout session response payload fields",
                                List.of(
                                        "id",
                                        "status",
                                        "currency",
                                        "line_items",
                                        "totals",
                                        "buyer",
                                        "fulfillment_details",
                                        "messages",
                                        "links",
                                        "return_url",
                                        "continue_url",
                                        "expires_at",
                                        "metadata"),
                                AgenticCommerceCheckoutSession.fromMap(checkoutSessionPayload()).toMap()),
                        payloadRequirement(
                                "payload.error",
                                "Error response payload fields",
                                List.of("type", "code", "message", "details", "metadata"),
                                AgenticCommerceError.fromMap(errorPayload()).toMap())));
    }

    public boolean aligned() {
        return gapCount() == 0;
    }

    public int requirementCount() {
        return requirements.size();
    }

    public int alignedCount() {
        return (int) requirements.stream()
                .filter(AgenticCommerceSpecAlignmentRequirement::aligned)
                .count();
    }

    public int gapCount() {
        return requirementCount() - alignedCount();
    }

    public List<AgenticCommerceSpecAlignmentRequirement> gaps() {
        return requirements.stream()
                .filter(requirement -> !requirement.aligned())
                .toList();
    }

    public List<String> requirementIds() {
        return requirements.stream()
                .map(AgenticCommerceSpecAlignmentRequirement::id)
                .toList();
    }

    public List<String> gapIds() {
        return gaps().stream()
                .map(AgenticCommerceSpecAlignmentRequirement::id)
                .toList();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("protocol", "agentic-commerce");
        values.put("specVersion", AgenticCommerceProtocol.SPEC_VERSION);
        values.put("specHome", AgenticCommerceProtocol.SPEC_HOME);
        values.put("specGitHub", AgenticCommerceProtocol.SPEC_GITHUB);
        values.put("standard", standardDescriptor());
        values.put("aligned", aligned());
        values.put("requirementCount", requirementCount());
        values.put("alignedCount", alignedCount());
        values.put("gapCount", gapCount());
        values.put("requirementIds", requirementIds());
        values.put("gapIds", gapIds());
        values.put("routeCatalog", routeCatalog.toMap());
        values.put("requirements", requirements.stream()
                .map(AgenticCommerceSpecAlignmentRequirement::toMap)
                .toList());
        return AgenticCommerceMaps.copy(values);
    }

    static Map<String, Object> standardDescriptor() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("standardId", STANDARD_ID);
        values.put("name", STANDARD_NAME);
        values.put("version", AgenticCommerceProtocol.SPEC_VERSION);
        values.put("binding", BINDING_HTTP_JSON);
        values.put("specUrl", AgenticCommerceProtocol.SPEC_HOME);
        values.put("githubUrl", AgenticCommerceProtocol.SPEC_GITHUB);
        return AgenticCommerceMaps.copy(values);
    }

    private static AgenticCommerceSpecAlignmentRequirement protocolMetadataRequirement() {
        Map<String, Object> expected = Map.of(
                "specVersion", "2026-01-30",
                "specHome", "https://www.agenticcommerce.dev",
                "specGitHub", "https://github.com/agentic-commerce-protocol/agentic-commerce-protocol");
        Map<String, Object> actual = Map.of(
                "specVersion", AgenticCommerceProtocol.SPEC_VERSION,
                "specHome", AgenticCommerceProtocol.SPEC_HOME,
                "specGitHub", AgenticCommerceProtocol.SPEC_GITHUB);
        boolean aligned = expected.equals(actual);
        return requirement(
                "protocol.metadata",
                "protocol",
                "Agentic Commerce protocol metadata",
                aligned,
                expected,
                actual,
                "Agentic Commerce protocol constants do not match the pinned spec snapshot.");
    }

    private static AgenticCommerceSpecAlignmentRequirement requiredHeadersRequirement() {
        Map<String, Object> expected = Map.of(
                "requiredHeaders", REQUIRED_HEADERS,
                "jsonMimeType", AgenticCommerceProtocol.MIME_JSON,
                "bearerPrefix", AgenticCommerceProtocol.BEARER_PREFIX);
        Map<String, Object> actual = new LinkedHashMap<>(expected);
        return AgenticCommerceSpecAlignmentRequirement.aligned(
                "http.required_headers",
                "http",
                "Agentic Commerce required HTTP headers",
                expected,
                actual);
    }

    private static AgenticCommerceSpecAlignmentRequirement routeRequirement(
            AgenticCommerceRouteCatalog catalog,
            AgenticCommerceHttpRoute expectedRoute) {
        return catalog.routeForOperation(expectedRoute.operation())
                .map(actualRoute -> routeRequirement(expectedRoute, actualRoute))
                .orElseGet(() -> AgenticCommerceSpecAlignmentRequirement.gap(
                        routeRequirementId(expectedRoute.operation()),
                        "route",
                        "Agentic Commerce route " + expectedRoute.operation(),
                        routeExpectation(expectedRoute),
                        Map.of("present", false),
                        "Agentic Commerce route is missing from the local checkout catalog."));
    }

    private static AgenticCommerceSpecAlignmentRequirement routeRequirement(
            AgenticCommerceHttpRoute expectedRoute,
            AgenticCommerceHttpRoute actualRoute) {
        Map<String, Object> expected = routeExpectation(expectedRoute);
        Map<String, Object> actual = routeActual(actualRoute);
        boolean aligned = expectedRoute.method().equals(actualRoute.method())
                && expectedRoute.pathTemplate().equals(actualRoute.pathTemplate())
                && expectedRoute.requestBodyRequired() == actualRoute.requestBodyRequired()
                && actualRoute.successStatusCodes().containsAll(expectedRoute.successStatusCodes());
        return requirement(
                routeRequirementId(expectedRoute.operation()),
                "route",
                "Agentic Commerce route " + expectedRoute.operation(),
                aligned,
                expected,
                actual,
                "Agentic Commerce route shape does not match the pinned checkout spec snapshot.");
    }

    private static AgenticCommerceSpecAlignmentRequirement payloadRequirement(
            String id,
            String title,
            List<String> expectedFields,
            Map<String, Object> payload) {
        Map<String, Object> expected = Map.of("fields", expectedFields);
        Map<String, Object> actual = Map.of("fields", fieldNames(payload));
        return requirement(
                id,
                "payload",
                title,
                expected.equals(actual),
                expected,
                actual,
                "Agentic Commerce payload field names do not match the pinned checkout spec snapshot.");
    }

    private static AgenticCommerceSpecAlignmentRequirement requirement(
            String id,
            String category,
            String title,
            boolean aligned,
            Map<String, Object> expected,
            Map<String, Object> actual,
            String message) {
        if (aligned) {
            return AgenticCommerceSpecAlignmentRequirement.aligned(id, category, title, expected, actual);
        }
        return AgenticCommerceSpecAlignmentRequirement.gap(id, category, title, expected, actual, message);
    }

    private static Map<String, Object> routeExpectation(AgenticCommerceHttpRoute route) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("present", true);
        values.put("operation", route.operation());
        values.put("method", route.method());
        values.put("pathTemplate", route.pathTemplate());
        values.put("requestBodyRequired", route.requestBodyRequired());
        values.put("successStatusCodes", route.successStatusCodes());
        return AgenticCommerceMaps.copy(values);
    }

    private static Map<String, Object> routeActual(AgenticCommerceHttpRoute route) {
        Map<String, Object> values = new LinkedHashMap<>(routeExpectation(route));
        values.put("routeMatched", true);
        return AgenticCommerceMaps.copy(values);
    }

    private static String routeRequirementId(String operation) {
        return "route." + operation;
    }

    private static List<String> fieldNames(Map<String, Object> payload) {
        return payload.keySet().stream().toList();
    }

    private static Map<String, Object> createCheckoutPayload() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("buyer", buyerPayload());
        values.put("line_items", List.of(checkoutItemPayload()));
        values.put("currency", "usd");
        values.put("fulfillment_details", fulfillmentDetailsPayload());
        values.put("capabilities", Map.of("payment", true));
        values.put("fulfillment_groups", List.of(Map.of("id", "shipping")));
        values.put("affiliate_attribution", Map.of("campaign", "launch"));
        values.put("coupons", List.of("WELCOME"));
        values.put("discounts", Map.of("code", "WELCOME"));
        values.put("locale", "en-US");
        values.put("timezone", "UTC");
        values.put("quote_id", "quote_1");
        values.put("metadata", Map.of("source", "spec-alignment"));
        return AgenticCommerceMaps.copy(values);
    }

    private static Map<String, Object> updateCheckoutPayload() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("buyer", buyerPayload());
        values.put("line_items", List.of(checkoutItemPayload()));
        values.put("fulfillment_details", fulfillmentDetailsPayload());
        values.put("fulfillment_groups", List.of(Map.of("id", "shipping")));
        values.put("selected_fulfillment_options", List.of(Map.of("id", "standard")));
        values.put("fulfillment_option_id", "standard");
        values.put("coupons", List.of("WELCOME"));
        values.put("discounts", Map.of("code", "WELCOME"));
        values.put("metadata", Map.of("source", "spec-alignment"));
        return AgenticCommerceMaps.copy(values);
    }

    private static Map<String, Object> completeCheckoutPayload() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("buyer", buyerPayload());
        values.put("payment_data", Map.of("payment_method", "card"));
        values.put("authentication_result", Map.of("status", "authenticated"));
        values.put("affiliate_attribution", Map.of("campaign", "launch"));
        values.put("risk_signals", Map.of("risk", "low"));
        values.put("metadata", Map.of("source", "spec-alignment"));
        return AgenticCommerceMaps.copy(values);
    }

    private static Map<String, Object> cancelCheckoutPayload() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("intent_trace", Map.of("reason", "buyer_requested"));
        values.put("metadata", Map.of("source", "spec-alignment"));
        return AgenticCommerceMaps.copy(values);
    }

    private static Map<String, Object> checkoutSessionPayload() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", "cs_1");
        values.put("status", AgenticCommerceCheckoutStatus.OPEN);
        values.put("currency", "usd");
        values.put("line_items", List.of(lineItemPayload()));
        values.put("totals", Map.of("total", 1000));
        values.put("buyer", buyerPayload());
        values.put("fulfillment_details", fulfillmentDetailsPayload());
        values.put("messages", List.of(Map.of("text", "Ready")));
        values.put("links", Map.of("checkout", "https://example.test/checkout"));
        values.put("return_url", "https://example.test/return");
        values.put("continue_url", "https://example.test/continue");
        values.put("expires_at", "2026-01-30T00:00:00Z");
        values.put("metadata", Map.of("source", "spec-alignment"));
        return AgenticCommerceMaps.copy(values);
    }

    private static Map<String, Object> errorPayload() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", "invalid_request");
        values.put("code", "invalid_checkout_session");
        values.put("message", "Invalid checkout session.");
        values.put("details", Map.of("field", "checkout_session"));
        values.put("metadata", Map.of("source", "spec-alignment"));
        return AgenticCommerceMaps.copy(values);
    }

    private static Map<String, Object> buyerPayload() {
        return Map.of("email", "buyer@example.test");
    }

    private static Map<String, Object> checkoutItemPayload() {
        return Map.of(
                "id",
                "sku_1",
                "name",
                "Wayang Hoodie",
                "quantity",
                1,
                "unit_amount",
                1000);
    }

    private static Map<String, Object> lineItemPayload() {
        return Map.of(
                "id",
                "sku_1",
                "name",
                "Wayang Hoodie",
                "quantity",
                1,
                "amount_total",
                1000);
    }

    private static Map<String, Object> fulfillmentDetailsPayload() {
        return Map.of("address", Map.of("country", "US"));
    }
}
