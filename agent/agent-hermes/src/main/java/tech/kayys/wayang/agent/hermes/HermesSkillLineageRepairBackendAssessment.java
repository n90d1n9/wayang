package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Backend readiness assessment for one repair intent.
 */
public record HermesSkillLineageRepairBackendAssessment(
        String intentId,
        String command,
        String targetType,
        String target,
        String selectedBackend,
        boolean commandSupported,
        boolean mutationSupported,
        String status,
        List<HermesSkillLineageRepairBackendProbe> probes) {

    public HermesSkillLineageRepairBackendAssessment {
        intentId = HermesText.oneLineOr(intentId, "intent");
        command = HermesText.oneLineOr(command, "review-learned-skill-lineage");
        targetType = HermesText.oneLineOr(targetType, "catalog");
        target = HermesText.oneLineOr(target, "");
        probes = HermesCollections.copyNonNull(probes);
        commandSupported = commandSupported || probes.stream()
                .anyMatch(HermesSkillLineageRepairBackendProbe::commandSupported);
        mutationSupported = mutationSupported || probes.stream()
                .anyMatch(HermesSkillLineageRepairBackendProbe::mutationSupported);
        selectedBackend = HermesText.oneLineOr(selectedBackend, selectedBackend(probes));
        status = HermesText.oneLineOr(status, status(commandSupported, mutationSupported));
    }

    public static HermesSkillLineageRepairBackendAssessment from(
            HermesSkillLineageRepairIntent intent,
            List<HermesSkillLineageRepairBackend> backends) {
        HermesSkillLineageRepairIntent resolved = intent == null
                ? HermesSkillLineageRepairIntent.from(
                        1,
                        new HermesSkillLineageRemediationAction(
                                "review-lineage",
                                "info",
                                false,
                                false,
                                "catalog",
                                "learned-skills",
                                "learned-skill lineage review recommended",
                                Map.of()),
                        HermesSkillLineageRemediationPolicy.dryRun())
                : intent;
        List<HermesSkillLineageRepairBackendProbe> probes = HermesCollections.copyNonNull(backends).stream()
                .map(backend -> backend.probe(resolved))
                .toList();
        return new HermesSkillLineageRepairBackendAssessment(
                resolved.intentId(),
                resolved.command(),
                resolved.targetType(),
                resolved.target(),
                "",
                false,
                false,
                "",
                probes);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("intentId", intentId);
        values.put("command", command);
        values.put("targetType", targetType);
        values.put("target", target);
        values.put("selectedBackend", selectedBackend);
        values.put("commandSupported", commandSupported);
        values.put("mutationSupported", mutationSupported);
        values.put("status", status);
        values.put("probes", probes.stream()
                .map(HermesSkillLineageRepairBackendProbe::toMetadata)
                .toList());
        return Map.copyOf(values);
    }

    private static String selectedBackend(List<HermesSkillLineageRepairBackendProbe> probes) {
        return probes.stream()
                .filter(HermesSkillLineageRepairBackendProbe::mutationSupported)
                .map(HermesSkillLineageRepairBackendProbe::backendId)
                .findFirst()
                .orElseGet(() -> probes.stream()
                        .filter(HermesSkillLineageRepairBackendProbe::commandSupported)
                        .map(HermesSkillLineageRepairBackendProbe::backendId)
                        .findFirst()
                        .orElse(""));
    }

    private static String status(boolean commandSupported, boolean mutationSupported) {
        if (mutationSupported) {
            return "mutation-ready";
        }
        return commandSupported ? "preview-only" : "unsupported";
    }
}
