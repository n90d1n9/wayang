package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.text;

record WayangA2aJsonRpcReadinessBindingReportIssueBucketRule(
        String bucket,
        String code,
        String field,
        boolean requiresConcreteActual,
        boolean fieldPrefix) {

    WayangA2aJsonRpcReadinessBindingReportIssueBucketRule {
        bucket = bucket == null ? "" : bucket.trim();
        code = code == null ? "" : code.trim();
        field = field == null ? "" : field.trim();
    }

    static List<WayangA2aJsonRpcReadinessBindingReportIssueBucketRule> defaults() {
        return List.of(
                diagnosticHandlers(),
                methodDispatch(),
                methodDispatchGroup());
    }

    static WayangA2aJsonRpcReadinessBindingReportIssueBucketRule diagnosticHandlers() {
        return new WayangA2aJsonRpcReadinessBindingReportIssueBucketRule(
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_DIAGNOSTIC_HANDLERS,
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE,
                WayangA2aJsonRpcReadinessIssueCatalog.FIELD_DIAGNOSTIC_HANDLERS_COMPLETE,
                true,
                false);
    }

    static WayangA2aJsonRpcReadinessBindingReportIssueBucketRule methodDispatch() {
        return new WayangA2aJsonRpcReadinessBindingReportIssueBucketRule(
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_METHOD_DISPATCH,
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE,
                WayangA2aJsonRpcReadinessIssueCatalog.FIELD_METHOD_DISPATCH_COMPLETE,
                false,
                false);
    }

    static WayangA2aJsonRpcReadinessBindingReportIssueBucketRule methodDispatchGroup() {
        return new WayangA2aJsonRpcReadinessBindingReportIssueBucketRule(
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_METHOD_DISPATCH,
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_DISPATCH_GROUP_COVERAGE_INCOMPLETE,
                WayangA2aJsonRpcReadinessIssueCatalog.FIELD_METHOD_DISPATCH_GROUP_PREFIX,
                false,
                true);
    }

    boolean matches(Map<String, Object> issue) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(issue);
        return code.equals(text(copy.get("code"), ""))
                && fieldMatches(text(copy.get("field"), ""))
                && (!requiresConcreteActual || concreteActual(copy));
    }

    private boolean fieldMatches(String value) {
        return fieldPrefix ? value.startsWith(field) : field.equals(value);
    }

    private static boolean concreteActual(Map<String, Object> issue) {
        String actual = text(issue.get("actual"), "");
        return !actual.isBlank() && !"missing=[], orphan=[]".equals(actual);
    }
}
