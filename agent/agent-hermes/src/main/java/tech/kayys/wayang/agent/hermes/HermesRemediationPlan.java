package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordered remediation plan for dispatch attention emitted by Hermes runtime ports.
 */
public record HermesRemediationPlan(
        boolean required,
        String strategy,
        int actionCount,
        int criticalCount,
        int retryableCount,
        List<HermesRemediationAction> actions) {

    public HermesRemediationPlan {
        actions = actions == null ? List.of() : List.copyOf(actions);
        required = required || !actions.isEmpty();
        actionCount = actions.size();
        criticalCount = (int) actions.stream()
                .filter(action -> action.severity().equalsIgnoreCase("critical"))
                .count();
        retryableCount = (int) actions.stream()
                .filter(HermesRemediationAction::retryable)
                .count();
        strategy = HermesDirectiveSupport.clean(strategy, required ? "manual-review" : "none");
    }

    public static HermesRemediationPlan none() {
        return new HermesRemediationPlan(false, "none", 0, 0, 0, List.of());
    }

    public static HermesRemediationPlan from(List<HermesDirectiveDispatchAttention> attention) {
        List<HermesRemediationAction> actions = HermesCollections.copyNonNull(attention).stream()
                .map(HermesRemediationAction::from)
                .toList();
        if (actions.isEmpty()) {
            return none();
        }
        return new HermesRemediationPlan(
                true,
                strategyFor(actions),
                0,
                0,
                0,
                actions);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("required", required);
        values.put("strategy", strategy);
        values.put("actionCount", actionCount);
        values.put("criticalCount", criticalCount);
        values.put("retryableCount", retryableCount);
        values.put("actions", actions.stream()
                .map(HermesRemediationAction::toMetadata)
                .toList());
        return Map.copyOf(values);
    }

    private static String strategyFor(List<HermesRemediationAction> actions) {
        if (hasAction(actions, "configure-runtime-port")) {
            return "configure-runtime-port";
        }
        if (hasAction(actions, "restore-runtime-port")) {
            return "restore-runtime-port";
        }
        if (actions.stream().anyMatch(HermesRemediationAction::retryable)) {
            return "retry-runtime-adapter";
        }
        return "manual-review";
    }

    private static boolean hasAction(List<HermesRemediationAction> actions, String actionType) {
        return actions.stream().anyMatch(action -> action.action().equalsIgnoreCase(actionType));
    }
}
