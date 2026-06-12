package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceBuyer;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutHttpRequests;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutItem;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutSession;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCheckoutStatus;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceCreateCheckoutSessionRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceError;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceFulfillmentDetails;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpRequest;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceHttpResponse;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceLineItem;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceProtocol;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceTotals;
import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceUpdateCheckoutSessionRequest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deterministic seller connector for local harnesses, tests, and demos.
 */
public final class InMemoryAgenticCommerceConnector implements AgenticCommerceConnector {

    private final Map<String, AgenticCommerceCheckoutSession> sessions = new LinkedHashMap<>();
    private long sequence;

    @Override
    public synchronized AgenticCommerceHttpResponse exchange(AgenticCommerceHttpRequest request) {
        String operation = AgenticCommerceWayangMaps.firstText(request.attributes(), "operation");
        return switch (operation) {
            case AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION -> create(request);
            case AgenticCommerceProtocol.OPERATION_RETRIEVE_CHECKOUT_SESSION -> retrieve(request);
            case AgenticCommerceProtocol.OPERATION_UPDATE_CHECKOUT_SESSION -> update(request);
            case AgenticCommerceProtocol.OPERATION_COMPLETE_CHECKOUT_SESSION -> transition(
                    request,
                    AgenticCommerceCheckoutStatus.COMPLETED);
            case AgenticCommerceProtocol.OPERATION_CANCEL_CHECKOUT_SESSION -> transition(
                    request,
                    AgenticCommerceCheckoutStatus.CANCELED);
            default -> error(request, 400, "unsupported_operation", "Unsupported Agentic Commerce operation.");
        };
    }

    public synchronized Map<String, AgenticCommerceCheckoutSession> sessions() {
        return Map.copyOf(sessions);
    }

    private AgenticCommerceHttpResponse create(AgenticCommerceHttpRequest request) {
        AgenticCommerceCreateCheckoutSessionRequest payload =
                AgenticCommerceCreateCheckoutSessionRequest.fromMap(readBody(request));
        String id = fixtureSessionId(payload).orElseGet(() -> "cs_" + (++sequence));
        AgenticCommerceCheckoutSession session = session(
                id,
                AgenticCommerceCheckoutStatus.OPEN,
                currency(payload.currency()),
                lineItems(payload.lineItems()),
                payload.buyer(),
                payload.fulfillmentDetails(),
                payload.metadata());
        sessions.put(id, session);
        return response(request, 201, session);
    }

    private AgenticCommerceHttpResponse retrieve(AgenticCommerceHttpRequest request) {
        return existing(request)
                .map(session -> response(request, 200, session))
                .orElseGet(() -> missing(request));
    }

    private AgenticCommerceHttpResponse update(AgenticCommerceHttpRequest request) {
        return existing(request)
                .map(current -> {
                    AgenticCommerceUpdateCheckoutSessionRequest payload =
                            AgenticCommerceUpdateCheckoutSessionRequest.fromMap(readBody(request));
                    List<AgenticCommerceLineItem> lineItems = payload.lineItems().isEmpty()
                            ? current.lineItems()
                            : lineItems(payload.lineItems());
                    AgenticCommerceCheckoutSession updated = new AgenticCommerceCheckoutSession(
                            current.id(),
                            AgenticCommerceCheckoutStatus.READY_FOR_PAYMENT,
                            current.currency(),
                            lineItems,
                            totals(lineItems),
                            payload.buyer().isEmpty() ? current.buyer() : payload.buyer(),
                            payload.fulfillmentDetails().isEmpty()
                                    ? current.fulfillmentDetails()
                                    : payload.fulfillmentDetails(),
                            current.messages(),
                            current.links(),
                            current.returnUrl(),
                            current.continueUrl(),
                            current.expiresAt(),
                            merge(current.metadata(), payload.metadata()));
                    sessions.put(updated.id(), updated);
                    return response(request, 200, updated);
                })
                .orElseGet(() -> missing(request));
    }

    private AgenticCommerceHttpResponse transition(AgenticCommerceHttpRequest request, String status) {
        return existing(request)
                .map(current -> {
                    AgenticCommerceCheckoutSession updated = new AgenticCommerceCheckoutSession(
                            current.id(),
                            status,
                            current.currency(),
                            current.lineItems(),
                            current.totals(),
                            current.buyer(),
                            current.fulfillmentDetails(),
                            current.messages(),
                            current.links(),
                            current.returnUrl(),
                            current.continueUrl(),
                            current.expiresAt(),
                            current.metadata());
                    sessions.put(updated.id(), updated);
                    return response(request, 200, updated);
                })
                .orElseGet(() -> missing(request));
    }

    private java.util.Optional<AgenticCommerceCheckoutSession> existing(AgenticCommerceHttpRequest request) {
        return java.util.Optional.ofNullable(sessions.get(checkoutSessionId(request)));
    }

    private AgenticCommerceCheckoutSession session(
            String id,
            String status,
            String currency,
            List<AgenticCommerceLineItem> lineItems,
            AgenticCommerceBuyer buyer,
            AgenticCommerceFulfillmentDetails fulfillmentDetails,
            Map<String, Object> metadata) {
        return new AgenticCommerceCheckoutSession(
                id,
                status,
                currency,
                lineItems,
                totals(lineItems),
                buyer,
                fulfillmentDetails,
                List.of(),
                Map.of("self", AgenticCommerceCheckoutHttpRequests.sessionPath(id)),
                "",
                "",
                "",
                metadata);
    }

    private static List<AgenticCommerceLineItem> lineItems(List<AgenticCommerceCheckoutItem> items) {
        return items.stream()
                .map(InMemoryAgenticCommerceConnector::lineItem)
                .filter(item -> !item.isEmpty())
                .toList();
    }

    private static AgenticCommerceLineItem lineItem(AgenticCommerceCheckoutItem item) {
        long total = item.quantity() * item.unitAmount();
        return new AgenticCommerceLineItem(
                item.id(),
                item.name(),
                item.description(),
                item.quantity(),
                item.unitAmount(),
                total,
                item.toMap(),
                item.metadata());
    }

    private static AgenticCommerceTotals totals(List<AgenticCommerceLineItem> lineItems) {
        long subtotal = lineItems.stream().mapToLong(AgenticCommerceLineItem::totalAmount).sum();
        return new AgenticCommerceTotals(subtotal, 0, 0, 0, subtotal, Map.of());
    }

    private static Map<String, Object> readBody(AgenticCommerceHttpRequest request) {
        if (request.body().isBlank()) {
            return Map.of();
        }
        return AgenticCommerceJson.readObject(request.body());
    }

    private AgenticCommerceHttpResponse missing(AgenticCommerceHttpRequest request) {
        return error(request, 404, "checkout_session_not_found", "Checkout session was not found.");
    }

    private AgenticCommerceHttpResponse error(
            AgenticCommerceHttpRequest request,
            int statusCode,
            String code,
            String message) {
        Map<String, Object> body = Map.of("error", AgenticCommerceError.of(code, message).toMap());
        return new AgenticCommerceHttpResponse(
                statusCode,
                AgenticCommerceJson.write(body),
                responseHeaders(request),
                responseAttributes(request));
    }

    private AgenticCommerceHttpResponse response(
            AgenticCommerceHttpRequest request,
            int statusCode,
            AgenticCommerceCheckoutSession session) {
        return new AgenticCommerceHttpResponse(
                statusCode,
                AgenticCommerceJson.write(session.toMap()),
                responseHeaders(request),
                responseAttributes(request));
    }

    private Map<String, Object> responseHeaders(AgenticCommerceHttpRequest request) {
        Map<String, Object> headers = new LinkedHashMap<>();
        headers.put(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, AgenticCommerceProtocol.MIME_JSON);
        headers.put(
                AgenticCommerceProtocol.HEADER_REQUEST_ID,
                request.header(AgenticCommerceProtocol.HEADER_REQUEST_ID).orElseGet(() -> generatedRequestId(request)));
        request.header(AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY)
                .ifPresent(value -> headers.put(AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY, value));
        return Map.copyOf(headers);
    }

    private Map<String, Object> responseAttributes(AgenticCommerceHttpRequest request) {
        Map<String, Object> attributes = new LinkedHashMap<>(request.attributes());
        attributes.put(AgenticCommerceWayang.METADATA_CONNECTOR, "in-memory");
        return Map.copyOf(attributes);
    }

    private static String checkoutSessionId(AgenticCommerceHttpRequest request) {
        return AgenticCommerceWayangMaps.required(
                AgenticCommerceWayangMaps.firstText(request.attributes(), "checkoutSessionId"),
                "checkoutSessionId");
    }

    private static String currency(String value) {
        String normalized = AgenticCommerceWayangMaps.text(value).toUpperCase(java.util.Locale.ROOT);
        return normalized.isBlank() ? "USD" : normalized;
    }

    private static java.util.Optional<String> fixtureSessionId(AgenticCommerceCreateCheckoutSessionRequest payload) {
        String scenario = AgenticCommerceWayangMaps.text(payload.metadata().get("scenario"));
        return "smoke".equalsIgnoreCase(scenario) ? java.util.Optional.of("cs_smoke") : java.util.Optional.empty();
    }

    private static String generatedRequestId(AgenticCommerceHttpRequest request) {
        return "req_inmem_" + Integer.toUnsignedString(java.util.Objects.hash(
                request.method(),
                request.path(),
                request.body()));
    }

    private static Map<String, Object> merge(Map<String, Object> first, Map<String, Object> second) {
        if ((second == null || second.isEmpty()) && (first == null || first.isEmpty())) {
            return Map.of();
        }
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.putAll(AgenticCommerceWayangMaps.copy(first));
        merged.putAll(AgenticCommerceWayangMaps.copy(second));
        return Map.copyOf(merged);
    }
}
