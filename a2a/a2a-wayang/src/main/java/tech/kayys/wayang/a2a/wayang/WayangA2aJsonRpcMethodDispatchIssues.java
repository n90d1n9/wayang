package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonRpcDiagnosticIssues.issue;

final class WayangA2aJsonRpcMethodDispatchIssues {

    private WayangA2aJsonRpcMethodDispatchIssues() {
    }

    static List<Map<String, Object>> from(WayangA2aJsonRpcMethodDispatchCoverage coverage) {
        if (coverage == null || !coverage.reported() || coverage.complete()) {
            return List.of();
        }
        List<Map<String, Object>> issues = new ArrayList<>();
        issues.add(summaryIssue(coverage));
        coverage.methodGroups().stream()
                .filter(group -> !group.complete())
                .map(WayangA2aJsonRpcMethodDispatchIssues::groupIssue)
                .forEach(issues::add);
        return List.copyOf(issues);
    }

    private static Map<String, Object> summaryIssue(WayangA2aJsonRpcMethodDispatchCoverage coverage) {
        return issue(
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_BINDING_REPORT,
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_DISPATCH_COVERAGE_INCOMPLETE,
                WayangA2aJsonRpcReadinessIssueCatalog.FIELD_METHOD_DISPATCH_COMPLETE,
                "true",
                actual(coverage.missingDispatchMethods(), coverage.orphanDispatchMethods()),
                "A2A JSON-RPC binding report method dispatch coverage was incomplete.");
    }

    private static Map<String, Object> groupIssue(WayangA2aJsonRpcMethodDispatchGroupCoverage group) {
        return issue(
                WayangA2aJsonRpcReadinessIssueCatalog.PROBE_BINDING_REPORT,
                WayangA2aJsonRpcReadinessIssueCatalog.ISSUE_METHOD_DISPATCH_GROUP_COVERAGE_INCOMPLETE,
                WayangA2aJsonRpcReadinessIssueCatalog.methodDispatchGroupCompleteField(group.group()),
                "true",
                actual(group.missingDispatchMethods(), group.orphanDispatchMethods()),
                "A2A JSON-RPC " + group.group() + " method dispatch coverage was incomplete.");
    }

    private static String actual(List<String> missing, List<String> orphan) {
        return "missing=" + normalize(missing) + ", orphan=" + normalize(orphan);
    }

    private static List<String> normalize(List<String> methods) {
        return methods == null ? List.of() : List.copyOf(methods);
    }
}
