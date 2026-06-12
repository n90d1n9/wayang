package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WayangPlatformReadinessProfileRegistryConfigReadiness {

    public static final String READINESS_ID = "wayang.platform.readiness-profile-registry-config.readiness";

    private WayangPlatformReadinessProfileRegistryConfigReadiness() {
    }

    public static WayangReadinessReport assess(
            WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics) {
        WayangPlatformReadinessProfileRegistryConfigDiagnostics model = diagnostics == null
                ? WayangPlatformReadinessProfileRegistryConfig.builtin().diagnostics()
                : diagnostics;
        boolean ready = model.valid();
        List<Map<String, Object>> issues = model.issues().stream()
                .map(WayangPlatformReadinessProfileRegistryConfigReadiness::issue)
                .toList();
        return WayangReadinessReport.from(
                READINESS_ID,
                ready,
                WayangReadinessReports.exitCode(ready),
                issues.size(),
                List.of(WayangReadinessReports.probe(
                        "registry-config",
                        true,
                        ready,
                        model.issueCount(),
                        model.toMap())),
                issues,
                model.toMap());
    }

    private static Map<String, Object> issue(
            WayangPlatformReadinessProfileRegistryConfigIssue issue) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("field", issue.field());
        return WayangReadinessReports.issue(
                issue.code(),
                READINESS_ID,
                issue.message(),
                fields);
    }
}
