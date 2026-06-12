package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceResponseValidatorTest {

    private final AgenticCommerceResponseValidator validator = AgenticCommerceResponseValidator.checkout();

    @Test
    void acceptsValidCheckoutSessionCreateResponses() {
        AgenticCommerceHttpRequest request = createRequest();
        AgenticCommerceHttpResponse response = AgenticCommerceHttpResponse
                .json(201, "{\"id\":\"cs_123\",\"status\":\"open\"}")
                .withHeaders(Map.of(
                        AgenticCommerceProtocol.HEADER_REQUEST_ID,
                        "req_123",
                        AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY,
                        "idem-1"));

        AgenticCommerceResponseValidationReport report = validator.validate(request, response);

        assertThat(report.valid()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.route()).contains(AgenticCommerceHttpRoute.createCheckoutSession());
        assertThat(report.toMap())
                .containsEntry("valid", true)
                .containsEntry("operation", AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION)
                .containsEntry("issueCount", 0);
    }

    @Test
    void acceptsValidCheckoutSessionRetrieveResponses() {
        AgenticCommerceHttpRequest request = new AgenticCommerceHttpRequest(
                "GET",
                "/checkout_sessions/cs_123",
                "",
                requestHeaders(),
                Map.of());
        AgenticCommerceHttpResponse response = AgenticCommerceHttpResponse
                .json(200, "{\"id\":\"cs_123\",\"status\":\"open\"}")
                .withHeaders(Map.of(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req_123"));

        AgenticCommerceResponseValidationReport report = validator.validate(request, response);

        assertThat(report.valid()).isTrue();
        assertThat(report.route()).contains(AgenticCommerceHttpRoute.retrieveCheckoutSession());
        assertThat(report.issues()).isEmpty();
    }

    @Test
    void rejectsUnexpectedStatusContentTypeBodyAndMissingRequestId() {
        AgenticCommerceResponseValidationReport report = validator.validate(
                createRequest(),
                new AgenticCommerceHttpResponse(
                        200,
                        "",
                        Map.of(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, "text/plain"),
                        Map.of()));

        assertThat(report.valid()).isFalse();
        assertThat(report.issues())
                .extracting(AgenticCommerceValidationIssue::code)
                .containsExactly(
                        "unexpected_status",
                        "unsupported_content_type",
                        "missing_response_body",
                        "missing_request_id");
        assertThat(report.toMap())
                .containsEntry("valid", false)
                .containsEntry("issueCount", 4)
                .containsEntry("operation", AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION);
    }

    @Test
    void rejectsIdempotencyEchoMismatch() {
        AgenticCommerceHttpResponse response = AgenticCommerceHttpResponse
                .json(201, "{\"id\":\"cs_123\"}")
                .withHeaders(Map.of(
                        AgenticCommerceProtocol.HEADER_REQUEST_ID,
                        "req_123",
                        AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY,
                        "other-idem"));

        AgenticCommerceResponseValidationReport report = validator.validate(createRequest(), response);

        assertThat(report.valid()).isFalse();
        assertThat(report.issues())
                .extracting(AgenticCommerceValidationIssue::code)
                .containsExactly("idempotency_key_mismatch");
    }

    @Test
    void reportsUnknownRoutesAndMethodMismatchesForResponses() {
        AgenticCommerceResponseValidationReport unknown = validator.validate(
                new AgenticCommerceHttpRequest("GET", "/future", "", requestHeaders(), Map.of()),
                validResponse(200));
        AgenticCommerceResponseValidationReport methodMismatch = validator.validate(
                new AgenticCommerceHttpRequest("GET", AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS, "", requestHeaders(), Map.of()),
                validResponse(200));

        assertThat(unknown.route()).isEmpty();
        assertThat(unknown.issues())
                .extracting(AgenticCommerceValidationIssue::code)
                .contains("unknown_route");
        assertThat(methodMismatch.route()).isEmpty();
        assertThat(methodMismatch.issues())
                .extracting(AgenticCommerceValidationIssue::code)
                .contains("method_not_allowed");
    }

    @Test
    void normalizesResponsesHeadersAndAttributes() {
        AgenticCommerceHttpResponse response = new AgenticCommerceHttpResponse(
                        -1,
                        null,
                        Map.of("content-type", "APPLICATION/JSON; charset=UTF-8"),
                        Map.of("source", "seller"))
                .withHeaders(Map.of(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req_123"))
                .withAttributes(Map.of("traceId", "trace-1"));

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEmpty();
        assertThat(response.header(AgenticCommerceProtocol.HEADER_CONTENT_TYPE))
                .contains("APPLICATION/JSON; charset=UTF-8");
        assertThat(response.contentType(AgenticCommerceProtocol.MIME_JSON)).isTrue();
        assertThat(response.requestId()).isEqualTo("req_123");
        assertThat(response.attributes())
                .containsEntry("source", "seller")
                .containsEntry("traceId", "trace-1");
    }

    private static AgenticCommerceHttpRequest createRequest() {
        return new AgenticCommerceHttpRequest(
                "POST",
                AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS,
                "{\"buyer\":{\"email\":\"buyer@example.com\"}}",
                requestHeaders(),
                Map.of());
    }

    private static AgenticCommerceHttpResponse validResponse(int statusCode) {
        return AgenticCommerceHttpResponse
                .json(statusCode, "{\"id\":\"cs_123\"}")
                .withHeaders(Map.of(AgenticCommerceProtocol.HEADER_REQUEST_ID, "req_123"));
    }

    private static Map<String, Object> requestHeaders() {
        return Map.of(
                AgenticCommerceProtocol.HEADER_AUTHORIZATION,
                "Bearer token",
                AgenticCommerceProtocol.HEADER_API_VERSION,
                AgenticCommerceProtocol.SPEC_VERSION,
                AgenticCommerceProtocol.HEADER_CONTENT_TYPE,
                AgenticCommerceProtocol.MIME_JSON,
                AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY,
                "idem-1");
    }
}
