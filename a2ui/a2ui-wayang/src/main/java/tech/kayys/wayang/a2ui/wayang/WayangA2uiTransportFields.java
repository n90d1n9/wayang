package tech.kayys.wayang.a2ui.wayang;

/**
 * Canonical field names for Wayang A2UI transport request and response envelopes.
 */
public final class WayangA2uiTransportFields {

    public static final String KIND = "kind";
    public static final String BODY = "body";
    public static final String DATA_PART = "dataPart";

    public static final String MIME_TYPE = "mimeType";
    public static final String BODY_ENCODING = "bodyEncoding";
    public static final String DATA_PARTS = "dataParts";
    public static final String HANDLED_COUNT = "handledCount";
    public static final String REJECTED_COUNT = "rejectedCount";
    public static final String METADATA = "metadata";
    public static final String OUTCOME = "outcome";
    public static final String EMPTY = "empty";

    public static final String REQUEST_KIND = "requestKind";
    public static final String RESPONSE_KIND = "responseKind";
    public static final String ERROR_CODE = "errorCode";
    public static final String ERROR = "error";
    public static final String CODE = "code";
    public static final String MESSAGE = "message";
    public static final String ACTION_COUNT = "actionCount";
    public static final String MESSAGE_COUNT = "messageCount";
    public static final String DATA_PART_COUNT = "dataPartCount";
    public static final String SURFACE_KIND_COUNT = "surfaceKindCount";
    public static final String DESCRIPTOR_COUNT = "descriptorCount";
    public static final String POLICY_ACTION_COUNT = "policyActionCount";
    public static final String HANDLER_ACTION_COUNT = "handlerActionCount";
    public static final String ROUTE_COUNT = "routeCount";
    public static final String COMPLETE = "complete";
    public static final String ROUTE_OPERATION_COUNT = "routeOperationCount";
    public static final String HANDLER_OPERATION_COUNT = "handlerOperationCount";
    public static final String MISSING_HANDLER_COUNT = "missingHandlerCount";
    public static final String ORPHAN_HANDLER_COUNT = "orphanHandlerCount";
    public static final String PASSED = "passed";
    public static final String EXIT_CODE = "exitCode";
    public static final String SUITE_ID = "suiteId";
    public static final String SCENARIO_COUNT = "scenarioCount";
    public static final String ISSUE_COUNT = "issueCount";

    public static final String RESPONSE_KIND_SESSION_RESULT = "a2ui-session-result";
    public static final String RESPONSE_KIND_SURFACE_CATALOG = "surface-catalog";
    public static final String RESPONSE_KIND_ACTION_BINDING_REPORT = "action-binding-report";
    public static final String RESPONSE_KIND_HTTP_ROUTE_CATALOG = "http-route-catalog";
    public static final String RESPONSE_KIND_HTTP_BINDING_REPORT = "http-binding-report";
    public static final String RESPONSE_KIND_HTTP_SMOKE_RESULT = "http-smoke-result";
    public static final String RESPONSE_KIND_HTTP_READINESS_PROBE = "http-readiness-probe";
    public static final String RESPONSE_KIND_TRANSPORT_ERROR = "transport-error";

    private WayangA2uiTransportFields() {
    }
}
