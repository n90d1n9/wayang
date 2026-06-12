package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.a2ui.wayang.support.RecordValues;
import tech.kayys.wayang.a2ui.wayang.support.RecordNumbers;

/**
 * Shared issue taxonomy for mounted A2UI endpoint diagnostics.
 */
public final class WayangA2uiHttpEndpointDiagnosticIssueCatalog {

    public static final String CATEGORY_UNKNOWN_PATH = "unknown-path";
    public static final String CATEGORY_ROUTE_MISMATCH = "route-mismatch";
    public static final String CATEGORY_TRANSPORT_ERROR = "transport-error";
    public static final String CATEGORY_HTTP_STATUS = "http-status";

    public static final String ERROR_ENDPOINT_DIAGNOSTIC_ISSUE = "endpoint_diagnostic_issue";
    public static final String ERROR_UNKNOWN_ENDPOINT_PATH = "unknown_endpoint_path";
    public static final String ERROR_ENDPOINT_ROUTE_MISMATCH = "endpoint_route_mismatch";
    public static final String ERROR_HTTP_STATUS_PREFIX = "http_status_";

    public static String normalizeCategory(String category) {
        return RecordValues.textOrDefault(category, CATEGORY_HTTP_STATUS);
    }

    public static String normalizeErrorCode(String errorCode) {
        return RecordValues.textOrDefault(errorCode, ERROR_ENDPOINT_DIAGNOSTIC_ISSUE);
    }

    public static String category(
            boolean knownPath,
            boolean matched,
            boolean transportError) {
        if (!knownPath) {
            return CATEGORY_UNKNOWN_PATH;
        }
        if (!matched) {
            return CATEGORY_ROUTE_MISMATCH;
        }
        if (transportError) {
            return CATEGORY_TRANSPORT_ERROR;
        }
        return CATEGORY_HTTP_STATUS;
    }

    public static String fallbackErrorCode(
            boolean knownPath,
            boolean matched,
            int statusCode) {
        if (!knownPath) {
            return ERROR_UNKNOWN_ENDPOINT_PATH;
        }
        if (!matched) {
            return ERROR_ENDPOINT_ROUTE_MISMATCH;
        }
        return httpStatusErrorCode(statusCode);
    }

    public static String httpStatusErrorCode(int statusCode) {
        return ERROR_HTTP_STATUS_PREFIX + RecordNumbers.nonNegative(statusCode);
    }

    private WayangA2uiHttpEndpointDiagnosticIssueCatalog() {
    }
}
