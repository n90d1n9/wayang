package tech.kayys.wayang.agenticcommerce.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Request-shape validator for Agentic Commerce Protocol HTTP bindings.
 */
public final class AgenticCommerceRequestValidator {

    private static final Pattern DATE_VERSION = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    private final AgenticCommerceRouteCatalog catalog;

    public AgenticCommerceRequestValidator() {
        this(AgenticCommerceRouteCatalog.checkoutCatalog());
    }

    public AgenticCommerceRequestValidator(AgenticCommerceRouteCatalog catalog) {
        this.catalog = catalog == null ? AgenticCommerceRouteCatalog.checkoutCatalog() : catalog;
    }

    public static AgenticCommerceRequestValidator checkout() {
        return new AgenticCommerceRequestValidator(AgenticCommerceRouteCatalog.checkoutCatalog());
    }

    public AgenticCommerceValidationReport validate(AgenticCommerceHttpRequest request) {
        AgenticCommerceHttpRequest resolved = java.util.Objects.requireNonNull(request, "request");
        Optional<AgenticCommerceHttpRoute> exactRoute = catalog.route(resolved);
        Optional<AgenticCommerceHttpRoute> pathRoute = catalog.routeForPath(resolved.path());
        List<AgenticCommerceValidationIssue> issues = new ArrayList<>();
        if (pathRoute.isEmpty()) {
            issues.add(issue(
                    "unknown_route",
                    "path",
                    "Unknown Agentic Commerce route.",
                    "known checkout route",
                    resolved.path()));
        } else if (exactRoute.isEmpty()) {
            issues.add(issue(
                    "method_not_allowed",
                    "method",
                    "HTTP method does not match the Agentic Commerce route.",
                    pathRoute.get().method(),
                    resolved.method()));
        }
        validateHeaders(resolved, exactRoute.or(() -> pathRoute), issues);
        return new AgenticCommerceValidationReport(resolved, exactRoute, issues);
    }

    private static void validateHeaders(
            AgenticCommerceHttpRequest request,
            Optional<AgenticCommerceHttpRoute> route,
            List<AgenticCommerceValidationIssue> issues) {
        String authorization = request.authorization();
        if (authorization.isBlank()) {
            issues.add(issue(
                    "missing_authorization",
                    AgenticCommerceProtocol.HEADER_AUTHORIZATION,
                    "Authorization header is required.",
                    AgenticCommerceProtocol.BEARER_PREFIX + "<token>",
                    ""));
        } else if (!authorization.startsWith(AgenticCommerceProtocol.BEARER_PREFIX)
                || authorization.substring(AgenticCommerceProtocol.BEARER_PREFIX.length()).isBlank()) {
            issues.add(issue(
                    "invalid_authorization",
                    AgenticCommerceProtocol.HEADER_AUTHORIZATION,
                    "Authorization header must use a non-empty Bearer token.",
                    AgenticCommerceProtocol.BEARER_PREFIX + "<token>",
                    authorization));
        }

        String apiVersion = request.apiVersion();
        if (apiVersion.isBlank()) {
            issues.add(issue(
                    "missing_api_version",
                    AgenticCommerceProtocol.HEADER_API_VERSION,
                    "API-Version header is required.",
                    AgenticCommerceProtocol.SPEC_VERSION,
                    ""));
        } else if (!DATE_VERSION.matcher(apiVersion).matches()) {
            issues.add(issue(
                    "invalid_api_version",
                    AgenticCommerceProtocol.HEADER_API_VERSION,
                    "API-Version must be a date version in yyyy-MM-dd format.",
                    "yyyy-MM-dd",
                    apiVersion));
        }

        if (route.map(AgenticCommerceHttpRoute::requestBodyRequired).orElse(false)) {
            if (request.body().isBlank()) {
                issues.add(issue(
                        "missing_request_body",
                        "body",
                        "Request body is required for this Agentic Commerce operation.",
                        "non-empty JSON body",
                        ""));
            }
            if (request.contentType().isBlank()) {
                issues.add(issue(
                        "missing_content_type",
                        AgenticCommerceProtocol.HEADER_CONTENT_TYPE,
                        "Content-Type header is required for JSON body requests.",
                        AgenticCommerceProtocol.MIME_JSON,
                        ""));
            } else if (!request.contentType(AgenticCommerceProtocol.MIME_JSON)) {
                issues.add(issue(
                        "unsupported_content_type",
                        AgenticCommerceProtocol.HEADER_CONTENT_TYPE,
                        "Content-Type must be application/json.",
                        AgenticCommerceProtocol.MIME_JSON,
                        request.contentType()));
            }
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
