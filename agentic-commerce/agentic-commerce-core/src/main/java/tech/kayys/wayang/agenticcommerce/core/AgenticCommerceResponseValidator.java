package tech.kayys.wayang.agenticcommerce.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Seller response-shape validator for Agentic Commerce Protocol HTTP bindings.
 */
public final class AgenticCommerceResponseValidator {

    private final AgenticCommerceRouteCatalog catalog;

    public AgenticCommerceResponseValidator() {
        this(AgenticCommerceRouteCatalog.checkoutCatalog());
    }

    public AgenticCommerceResponseValidator(AgenticCommerceRouteCatalog catalog) {
        this.catalog = catalog == null ? AgenticCommerceRouteCatalog.checkoutCatalog() : catalog;
    }

    public static AgenticCommerceResponseValidator checkout() {
        return new AgenticCommerceResponseValidator(AgenticCommerceRouteCatalog.checkoutCatalog());
    }

    public AgenticCommerceResponseValidationReport validate(
            AgenticCommerceHttpRequest request,
            AgenticCommerceHttpResponse response) {
        AgenticCommerceHttpRequest resolvedRequest = java.util.Objects.requireNonNull(request, "request");
        AgenticCommerceHttpResponse resolvedResponse = java.util.Objects.requireNonNull(response, "response");
        Optional<AgenticCommerceHttpRoute> exactRoute = catalog.route(resolvedRequest);
        Optional<AgenticCommerceHttpRoute> pathRoute = catalog.routeForPath(resolvedRequest.path());
        List<AgenticCommerceValidationIssue> issues = new ArrayList<>();
        if (pathRoute.isEmpty()) {
            issues.add(issue(
                    "unknown_route",
                    "path",
                    "Unknown Agentic Commerce route.",
                    "known checkout route",
                    resolvedRequest.path()));
        } else if (exactRoute.isEmpty()) {
            issues.add(issue(
                    "method_not_allowed",
                    "method",
                    "HTTP method does not match the Agentic Commerce route.",
                    pathRoute.get().method(),
                    resolvedRequest.method()));
        }
        exactRoute.ifPresent(route -> validateStatus(route, resolvedResponse, issues));
        validateResponseHeaders(resolvedRequest, resolvedResponse, issues);
        return new AgenticCommerceResponseValidationReport(
                resolvedRequest,
                resolvedResponse,
                exactRoute,
                issues);
    }

    private static void validateStatus(
            AgenticCommerceHttpRoute route,
            AgenticCommerceHttpResponse response,
            List<AgenticCommerceValidationIssue> issues) {
        if (!route.successStatusCodes().contains(response.statusCode())) {
            issues.add(issue(
                    "unexpected_status",
                    "statusCode",
                    "Response status does not match the Agentic Commerce route.",
                    String.valueOf(route.successStatusCodes()),
                    String.valueOf(response.statusCode())));
        }
    }

    private static void validateResponseHeaders(
            AgenticCommerceHttpRequest request,
            AgenticCommerceHttpResponse response,
            List<AgenticCommerceValidationIssue> issues) {
        if (response.contentType().isBlank()) {
            issues.add(issue(
                    "missing_content_type",
                    AgenticCommerceProtocol.HEADER_CONTENT_TYPE,
                    "Content-Type header is required for Agentic Commerce JSON responses.",
                    AgenticCommerceProtocol.MIME_JSON,
                    ""));
        } else if (!response.contentType(AgenticCommerceProtocol.MIME_JSON)) {
            issues.add(issue(
                    "unsupported_content_type",
                    AgenticCommerceProtocol.HEADER_CONTENT_TYPE,
                    "Content-Type must be application/json.",
                    AgenticCommerceProtocol.MIME_JSON,
                    response.contentType()));
        }

        if (response.body().isBlank()) {
            issues.add(issue(
                    "missing_response_body",
                    "body",
                    "Agentic Commerce responses must include a JSON body.",
                    "non-empty JSON body",
                    ""));
        }

        if (response.requestId().isBlank()) {
            issues.add(issue(
                    "missing_request_id",
                    AgenticCommerceProtocol.HEADER_REQUEST_ID,
                    "Request-Id response header is required for request correlation.",
                    "<request id>",
                    ""));
        }

        String requestIdempotencyKey = request.header(AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY).orElse("");
        String responseIdempotencyKey = response.idempotencyKey();
        if (!requestIdempotencyKey.isBlank()
                && !responseIdempotencyKey.isBlank()
                && !requestIdempotencyKey.equals(responseIdempotencyKey)) {
            issues.add(issue(
                    "idempotency_key_mismatch",
                    AgenticCommerceProtocol.HEADER_IDEMPOTENCY_KEY,
                    "Response Idempotency-Key must match the request when echoed.",
                    requestIdempotencyKey,
                    responseIdempotencyKey));
        }
    }

    private static AgenticCommerceValidationIssue issue(
            String code,
            String field,
            String message,
            String expected,
            String actual) {
        return AgenticCommerceValidationIssue.of(code, field, message, expected, actual);
    }
}
