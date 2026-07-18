package tech.kayys.wayang.readiness;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileRegistryConfig;
import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileRegistryConfigDiagnostics;
import tech.kayys.wayang.readiness.WayangPlatformReadinessProfileRegistryConfigIssue;
import tech.kayys.wayang.client.WayangReadinessReports;

/**
 * Readiness assessor for platform readiness profile registry configuration.
 * Evaluates whether the registry configuration is valid and properly set up.
 */
public class PlatformRegistryConfigAssessor extends ComponentReadinessAssessor {

    public static final String READINESS_ID = "wayang.platform.readiness-profile-registry-config.readiness";

    @Override
    protected String getId() {
        return READINESS_ID;
    }

    @Override
    protected String getSource() {
        return READINESS_ID;
    }

    @Override
    protected String buildProbeName() {
        return "registry-config";
    }

    @Override
    protected List<Map<String, Object>> validate(Object input) {
        WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics =
                (WayangPlatformReadinessProfileRegistryConfigDiagnostics) input;
        WayangPlatformReadinessProfileRegistryConfigDiagnostics model = diagnostics == null
                ? WayangPlatformReadinessProfileRegistryConfig.builtin().diagnostics()
                : diagnostics;

        if (model.valid()) {
            return List.of();
        }

        return model.issues().stream()
                .map(this::issue)
                .toList();
    }

    @Override
    protected Map<String, Object> buildAttributes(Object input) {
        WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics =
                (WayangPlatformReadinessProfileRegistryConfigDiagnostics) input;
        WayangPlatformReadinessProfileRegistryConfigDiagnostics model = diagnostics == null
                ? WayangPlatformReadinessProfileRegistryConfig.builtin().diagnostics()
                : diagnostics;

        return model.toMap();
    }

    @Override
    protected List<Map<String, Object>> buildProbes(Object input, List<Map<String, Object>> issues) {
        WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics =
                (WayangPlatformReadinessProfileRegistryConfigDiagnostics) input;
        WayangPlatformReadinessProfileRegistryConfigDiagnostics model = diagnostics == null
                ? WayangPlatformReadinessProfileRegistryConfig.builtin().diagnostics()
                : diagnostics;

        boolean ready = issues.isEmpty();
        return List.of(WayangReadinessReports.probe(
                buildProbeName(),
                true,
                ready,
                model.issueCount(),
                model.toMap()));
    }

    private Map<String, Object> issue(
            WayangPlatformReadinessProfileRegistryConfigIssue issue) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("field", issue.field());
        return WayangReadinessReports.issue(
                issue.code(),
                getSource(),
                issue.message(),
                fields);
    }
}
