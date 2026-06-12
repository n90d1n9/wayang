package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

record WayangA2aJsonRpcReadinessBindingReportIssueClassifier(
        List<WayangA2aJsonRpcReadinessBindingReportIssueBucketRule> rules) {

    private static final WayangA2aJsonRpcReadinessBindingReportIssueClassifier DEFAULT =
            new WayangA2aJsonRpcReadinessBindingReportIssueClassifier(
                    WayangA2aJsonRpcReadinessBindingReportIssueBucketRule.defaults());

    WayangA2aJsonRpcReadinessBindingReportIssueClassifier {
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    static WayangA2aJsonRpcReadinessBindingReportIssueClassifier defaults() {
        return DEFAULT;
    }

    String bucket(Map<String, Object> issue) {
        Map<String, Object> resolved = issue == null ? Map.of() : issue;
        return rules.stream()
                .filter(rule -> rule.matches(resolved))
                .map(WayangA2aJsonRpcReadinessBindingReportIssueBucketRule::bucket)
                .findFirst()
                .orElse(WayangA2aJsonRpcReadinessIssueCatalog.PROBE_BINDING_REPORT);
    }
}
