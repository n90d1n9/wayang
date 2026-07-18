package tech.kayys.wayang.readiness;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;

public record WayangPlatformReadinessProfileRegistryConfigDiagnostics(
        WayangPlatformReadinessProfileRegistryConfig config,
        List<WayangPlatformReadinessProfileRegistryConfigIssue> issues) {

    public WayangPlatformReadinessProfileRegistryConfigDiagnostics {
        config = config == null ? WayangPlatformReadinessProfileRegistryConfig.builtin() : config;
        issues = issues == null || issues.isEmpty()
                ? List.of()
                : issues.stream()
                        .filter(issue -> issue != null)
                        .toList();
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("valid", valid());
        values.put("issueCount", issueCount());
        values.put("config", config.toMap());
        values.put("issues", issues.stream()
                .map(WayangPlatformReadinessProfileRegistryConfigIssue::toMap)
                .toList());
        return SdkMaps.copy(values);
    }
}
