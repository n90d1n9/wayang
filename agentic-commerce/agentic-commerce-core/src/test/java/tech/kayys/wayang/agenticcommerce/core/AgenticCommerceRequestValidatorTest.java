package tech.kayys.wayang.agenticcommerce.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceRequestValidatorTest {

    private final AgenticCommerceRequestValidator validator = AgenticCommerceRequestValidator.checkout();

    @Test
    void acceptsValidCheckoutSessionCreateRequests() {
        AgenticCommerceValidationReport report = validator.validate(new AgenticCommerceHttpRequest(
                "POST",
                AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS,
                "{\"buyer\":{\"email\":\"buyer@example.com\"}}",
                validHeaders(),
                Map.of("traceId", "trace-1")));

        assertThat(report.valid()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.route()).contains(AgenticCommerceHttpRoute.createCheckoutSession());
        assertThat(report.toMap())
                .containsEntry("valid", true)
                .containsEntry("operation", AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION)
                .containsEntry("issueCount", 0);
    }

    @Test
    void acceptsValidCheckoutSessionRetrieveRequestsWithoutBodyContentType() {
        AgenticCommerceValidationReport report = validator.validate(new AgenticCommerceHttpRequest(
                "GET",
                "/checkout_sessions/cs_123",
                "",
                Map.of(
                        AgenticCommerceProtocol.HEADER_AUTHORIZATION,
                        "Bearer token",
                        AgenticCommerceProtocol.HEADER_API_VERSION,
                        AgenticCommerceProtocol.SPEC_VERSION),
                Map.of()));

        assertThat(report.valid()).isTrue();
        assertThat(report.route()).contains(AgenticCommerceHttpRoute.retrieveCheckoutSession());
        assertThat(report.issues()).isEmpty();
    }

    @Test
    void rejectsMissingRequiredHeadersAndBodyShape() {
        AgenticCommerceValidationReport report = validator.validate(new AgenticCommerceHttpRequest(
                "POST",
                AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS,
                "",
                Map.of(AgenticCommerceProtocol.HEADER_CONTENT_TYPE, "text/plain"),
                Map.of()));

        assertThat(report.valid()).isFalse();
        assertThat(report.issues())
                .extracting(AgenticCommerceValidationIssue::code)
                .containsExactly(
                        "missing_authorization",
                        "missing_api_version",
                        "missing_request_body",
                        "unsupported_content_type");
        assertThat(report.toMap())
                .containsEntry("valid", false)
                .containsEntry("issueCount", 4)
                .containsEntry("operation", AgenticCommerceProtocol.OPERATION_CREATE_CHECKOUT_SESSION);
    }

    @Test
    void rejectsInvalidBearerAndVersionFormat() {
        AgenticCommerceValidationReport report = validator.validate(new AgenticCommerceHttpRequest(
                "POST",
                "/checkout_sessions/cs_123",
                "{}",
                Map.of(
                        AgenticCommerceProtocol.HEADER_AUTHORIZATION,
                        "Basic token",
                        AgenticCommerceProtocol.HEADER_API_VERSION,
                        "v1",
                        AgenticCommerceProtocol.HEADER_CONTENT_TYPE,
                        "application/json; charset=UTF-8"),
                Map.of()));

        assertThat(report.valid()).isFalse();
        assertThat(report.route()).contains(AgenticCommerceHttpRoute.updateCheckoutSession());
        assertThat(report.issues())
                .extracting(AgenticCommerceValidationIssue::code)
                .containsExactly("invalid_authorization", "invalid_api_version");
    }

    @Test
    void reportsUnknownRoutesAndMethodMismatches() {
        AgenticCommerceValidationReport unknown = validator.validate(new AgenticCommerceHttpRequest(
                "GET",
                "/future",
                "",
                validHeaders(),
                Map.of()));
        AgenticCommerceValidationReport methodMismatch = validator.validate(new AgenticCommerceHttpRequest(
                "GET",
                AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS,
                "",
                validHeaders(),
                Map.of()));

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
    void normalizesRequestsHeadersAndAttributes() {
        AgenticCommerceHttpRequest request = new AgenticCommerceHttpRequest(
                        " post ",
                        "checkout_sessions",
                        null,
                        Map.of("content-type", "APPLICATION/JSON; charset=UTF-8"),
                        Map.of("tenant", "demo"))
                .withHeaders(Map.of(AgenticCommerceProtocol.HEADER_API_VERSION, AgenticCommerceProtocol.SPEC_VERSION))
                .withAttributes(Map.of("traceId", "trace-1"));

        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo(AgenticCommerceProtocol.PATH_CHECKOUT_SESSIONS);
        assertThat(request.body()).isEmpty();
        assertThat(request.header(AgenticCommerceProtocol.HEADER_CONTENT_TYPE))
                .contains("APPLICATION/JSON; charset=UTF-8");
        assertThat(request.contentType(AgenticCommerceProtocol.MIME_JSON)).isTrue();
        assertThat(request.apiVersion()).isEqualTo(AgenticCommerceProtocol.SPEC_VERSION);
        assertThat(request.attributes())
                .containsEntry("tenant", "demo")
                .containsEntry("traceId", "trace-1");
    }

    private static Map<String, Object> validHeaders() {
        return Map.of(
                AgenticCommerceProtocol.HEADER_AUTHORIZATION,
                "Bearer token",
                AgenticCommerceProtocol.HEADER_API_VERSION,
                AgenticCommerceProtocol.SPEC_VERSION,
                AgenticCommerceProtocol.HEADER_CONTENT_TYPE,
                AgenticCommerceProtocol.MIME_JSON);
    }
}
