package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;

record WayangA2aJsonRpcReadinessBindingReportIssueBuckets(
        List<Map<String, Object>> bindingReportIssues,
        List<Map<String, Object>> diagnosticHandlerIssues,
        List<Map<String, Object>> methodDispatchIssues) {

    WayangA2aJsonRpcReadinessBindingReportIssueBuckets {
        bindingReportIssues = copyObjects(bindingReportIssues);
        diagnosticHandlerIssues = copyObjects(diagnosticHandlerIssues);
        methodDispatchIssues = copyObjects(methodDispatchIssues);
    }

    static WayangA2aJsonRpcReadinessBindingReportIssueBuckets from(List<Map<String, Object>> issues) {
        return from(issues, WayangA2aJsonRpcReadinessBindingReportIssueClassifier.defaults());
    }

    static WayangA2aJsonRpcReadinessBindingReportIssueBuckets from(
            List<Map<String, Object>> issues,
            WayangA2aJsonRpcReadinessBindingReportIssueClassifier classifier) {
        if (issues == null || issues.isEmpty()) {
            return empty();
        }
        WayangA2aJsonRpcReadinessBindingReportIssueClassifier resolvedClassifier =
                classifier == null
                        ? WayangA2aJsonRpcReadinessBindingReportIssueClassifier.defaults()
                        : classifier;
        List<Map<String, Object>> bindingReportIssues = new ArrayList<>();
        List<Map<String, Object>> diagnosticHandlerIssues = new ArrayList<>();
        List<Map<String, Object>> methodDispatchIssues = new ArrayList<>();
        for (Map<String, Object> issue : issues) {
            switch (resolvedClassifier.bucket(issue)) {
                case WayangA2aJsonRpcReadinessIssueCatalog.PROBE_DIAGNOSTIC_HANDLERS
                        -> diagnosticHandlerIssues.add(issue);
                case WayangA2aJsonRpcReadinessIssueCatalog.PROBE_METHOD_DISPATCH
                        -> methodDispatchIssues.add(issue);
                default -> bindingReportIssues.add(issue);
            }
        }
        return new WayangA2aJsonRpcReadinessBindingReportIssueBuckets(
                bindingReportIssues,
                diagnosticHandlerIssues,
                methodDispatchIssues);
    }

    static WayangA2aJsonRpcReadinessBindingReportIssueBuckets empty() {
        return new WayangA2aJsonRpcReadinessBindingReportIssueBuckets(List.of(), List.of(), List.of());
    }
}
