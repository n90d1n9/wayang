package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

final class WayangA2aJsonRpcReadinessIssueCatalog {

    static final String PROBE_READINESS = "readiness";
    static final String PROBE_BINDING_REPORT = "bindingReport";
    static final String PROBE_DIAGNOSTIC_HANDLERS = "diagnosticHandlers";
    static final String PROBE_METHOD_DISPATCH = "methodDispatch";
    static final String PROBE_METHOD_REGISTRY = "methodRegistry";
    static final String PROBE_ROUTE_CATALOG = "routeCatalog";
    static final String PROBE_SMOKE = "smoke";
    static final String PROBE_SPEC_ALIGNMENT = "specAlignment";

    static final String ISSUE_BINDING_REPORT_PROBE_FAILED = "binding_report_probe_failed";
    static final String ISSUE_ROUTE_CATALOG_PROBE_FAILED = "route_catalog_probe_failed";
    static final String ISSUE_SMOKE_PROBE_FAILED = "smoke_probe_failed";
    static final String ISSUE_BINDING_REPORT_ROUTE_MISMATCH = "binding_report_route_mismatch";
    static final String ISSUE_METHOD_COUNT_MISSING = "method_count_missing";
    static final String ISSUE_DIAGNOSTIC_HANDLER_COVERAGE_INCOMPLETE =
            "diagnostic_handler_coverage_incomplete";
    static final String ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE =
            "method_dispatch_coverage_incomplete";
    static final String ISSUE_METHOD_DISPATCH_GROUP_COVERAGE_INCOMPLETE =
            "method_dispatch_group_coverage_incomplete";

    static final String FIELD_DIAGNOSTIC_HANDLERS_COMPLETE = "diagnosticHandlers.complete";
    static final String FIELD_METHOD_DISPATCH_COMPLETE = "methodDispatch.complete";
    static final String FIELD_METHOD_DISPATCH_GROUP_PREFIX = "methodDispatch.methodGroups.";

    private static final List<String> SUMMARY_PROBES = List.of(
            PROBE_READINESS,
            PROBE_BINDING_REPORT,
            PROBE_DIAGNOSTIC_HANDLERS,
            PROBE_METHOD_DISPATCH,
            PROBE_ROUTE_CATALOG,
            PROBE_SMOKE);

    private WayangA2aJsonRpcReadinessIssueCatalog() {
    }

    static List<String> summaryProbeNames() {
        return SUMMARY_PROBES;
    }

    static WayangA2aJsonRpcReadinessIssueGroup group(
            String probe,
            List<Map<String, Object>> issues) {
        return new WayangA2aJsonRpcReadinessIssueGroup(probe, issues);
    }

    static String normalizeProbe(String probe) {
        return probe == null ? "" : probe.trim();
    }

    static String methodDispatchGroupCompleteField(String group) {
        String resolved = normalizeProbe(group);
        return FIELD_METHOD_DISPATCH_GROUP_PREFIX
                + (resolved.isBlank() ? "unknown" : resolved)
                + ".complete";
    }

    static String specAlignmentCategoryProbe(String category) {
        String resolved = normalizeProbe(category);
        return resolved.isBlank()
                ? PROBE_SPEC_ALIGNMENT
                : PROBE_SPEC_ALIGNMENT + ":" + resolved;
    }
}
