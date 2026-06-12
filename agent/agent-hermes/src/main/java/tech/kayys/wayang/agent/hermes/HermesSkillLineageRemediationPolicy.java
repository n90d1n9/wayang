package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Safety policy for future learned-skill lineage remediation mutations.
 */
public record HermesSkillLineageRemediationPolicy(
        String mode,
        int maxActionsPerRun,
        List<String> allowedActions,
        List<String> allowedTargetTypes) {

    public static final String DRY_RUN = "dry-run";
    public static final String MANUAL = "manual";
    public static final String AUTOMATIC = "automatic";

    public HermesSkillLineageRemediationPolicy {
        mode = normalizeMode(mode);
        maxActionsPerRun = Math.max(maxActionsPerRun, 0);
        allowedActions = normalizeList(allowedActions);
        allowedTargetTypes = normalizeList(allowedTargetTypes);
    }

    public static HermesSkillLineageRemediationPolicy dryRun() {
        return new HermesSkillLineageRemediationPolicy(DRY_RUN, 0, List.of(), List.of());
    }

    public static HermesSkillLineageRemediationPolicy manual(
            int maxActionsPerRun,
            List<String> allowedActions,
            List<String> allowedTargetTypes) {
        return new HermesSkillLineageRemediationPolicy(
                MANUAL,
                maxActionsPerRun,
                allowedActions,
                allowedTargetTypes);
    }

    public boolean dryRunOnly() {
        return DRY_RUN.equals(mode);
    }

    public boolean mutationAllowed() {
        return !dryRunOnly();
    }

    public boolean automaticMutationAllowed() {
        return AUTOMATIC.equals(mode);
    }

    public boolean approvalRequired() {
        return MANUAL.equals(mode);
    }

    public int permittedActionCount(HermesSkillLineageRemediationPlan plan) {
        if (!mutationAllowed() || plan == null || maxActionsPerRun == 0) {
            return 0;
        }
        return Math.min(maxActionsPerRun, (int) plan.actions().stream()
                .filter(this::permits)
                .count());
    }

    public boolean permits(HermesSkillLineageRemediationAction action) {
        if (!mutationAllowed() || action == null) {
            return false;
        }
        if (automaticMutationAllowed() && !action.automatic()) {
            return false;
        }
        return allowed(allowedActions, action.action()) && allowed(allowedTargetTypes, action.targetType());
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", mode);
        values.put("dryRunOnly", dryRunOnly());
        values.put("mutationAllowed", mutationAllowed());
        values.put("automaticMutationAllowed", automaticMutationAllowed());
        values.put("approvalRequired", approvalRequired());
        values.put("maxActionsPerRun", maxActionsPerRun);
        values.put("allowedActions", allowedActions);
        values.put("allowedTargetTypes", allowedTargetTypes);
        return Map.copyOf(values);
    }

    private static boolean allowed(List<String> allowlist, String value) {
        String normalized = normalize(value);
        return allowlist.stream().anyMatch(candidate -> "all".equals(candidate) || candidate.equals(normalized));
    }

    private static List<String> normalizeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values.stream()
                .map(HermesSkillLineageRemediationPolicy::normalize)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
    }

    private static String normalizeMode(String value) {
        String normalized = normalize(value);
        return switch (normalized) {
            case "", DRY_RUN -> DRY_RUN;
            case MANUAL, "manual-approval", "approval" -> MANUAL;
            case AUTOMATIC, "auto" -> AUTOMATIC;
            default -> throw new IllegalArgumentException(
                    "skillLineageRemediationPolicy mode must be dry-run, manual, or automatic");
        };
    }

    private static String normalize(String value) {
        return HermesText.trimToEmpty(value)
                .toLowerCase(Locale.ROOT)
                .replace('_', '-');
    }
}
