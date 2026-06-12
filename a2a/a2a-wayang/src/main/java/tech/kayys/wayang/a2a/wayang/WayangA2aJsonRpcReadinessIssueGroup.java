package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.copyObjects;

record WayangA2aJsonRpcReadinessIssueGroup(
        String probe,
        List<Map<String, Object>> issues) {

    WayangA2aJsonRpcReadinessIssueGroup {
        probe = WayangA2aJsonRpcReadinessIssueCatalog.normalizeProbe(probe);
        issues = copyObjects(issues);
    }

    int issueCount() {
        return issues.size();
    }

    List<Map<String, Object>> wrappedIssues() {
        return WayangA2aJsonRpcReadinessIssueEnvelope.wrapAll(probe, issues);
    }
}
