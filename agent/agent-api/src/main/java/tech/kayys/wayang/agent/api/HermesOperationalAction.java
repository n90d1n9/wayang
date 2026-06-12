package tech.kayys.wayang.agent.api;

import java.util.List;
import java.util.Map;

/**
 * Structured operator-facing remediation action emitted by Hermes operational APIs.
 */
public record HermesOperationalAction(
        String source,
        String actionId,
        String severity,
        int priority,
        String riskLevel,
        boolean safe,
        boolean dryRunSupported,
        List<String> requiredConfig,
        String message,
        Map<String, Object> metadata) {

    public HermesOperationalAction {
        source = HermesResponseMetadata.text(source, "hermes");
        actionId = HermesResponseMetadata.text(actionId, "");
        severity = HermesResponseMetadata.text(severity, "info");
        priority = Math.max(priority, 0);
        riskLevel = HermesResponseMetadata.text(riskLevel, safe ? "low" : "medium");
        requiredConfig = requiredConfig == null ? List.of() : List.copyOf(requiredConfig);
        message = HermesResponseMetadata.text(message, actionId);
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }

    static List<HermesOperationalAction> retentionActions(
            String severity,
            int priority,
            List<String> actionIds) {
        if (actionIds == null || actionIds.isEmpty()) {
            return List.of();
        }
        return actionIds.stream()
                .map(actionId -> HermesResponseMetadata.text(actionId, ""))
                .filter(actionId -> !actionId.isEmpty())
                .distinct()
                .map(actionId -> retentionAction(severity, priority, actionId))
                .toList();
    }

    private static HermesOperationalAction retentionAction(
            String severity,
            int priority,
            String actionId) {
        return switch (actionId) {
            case "monitor-learning-audit-retention" -> retentionAction(
                    actionId,
                    "low",
                    true,
                    false,
                    List.of(),
                    "Monitor learning-audit retention pressure before it reaches capacity.",
                    severity,
                    priority);
            case "plan-learning-audit-retention-capacity" -> retentionAction(
                    actionId,
                    "low",
                    true,
                    false,
                    List.of(),
                    "Plan retention capacity based on current learning-audit write volume.",
                    severity,
                    priority);
            case "verify-learning-audit-ledger-pruning" -> retentionAction(
                    actionId,
                    "low",
                    true,
                    true,
                    List.of("learning-audit-ledger-access"),
                    "Verify that learning-audit receipt pruning is configured and running.",
                    severity,
                    priority);
            case "increase-learning-audit-retention-limit" -> retentionAction(
                    actionId,
                    "medium",
                    false,
                    true,
                    List.of("learning-audit-retention-limit"),
                    "Increase the learning-audit retention limit after validating storage capacity.",
                    severity,
                    priority);
            case "archive-learning-audit-receipts" -> retentionAction(
                    actionId,
                    "medium",
                    false,
                    true,
                    List.of("learning-audit-archive-target"),
                    "Archive older learning-audit receipts before they are evicted.",
                    severity,
                    priority);
            default -> retentionAction(
                    actionId,
                    "medium",
                    false,
                    true,
                    List.of(),
                    "Review and apply the recommended Hermes operational action.",
                    severity,
                    priority);
        };
    }

    private static HermesOperationalAction retentionAction(
            String actionId,
            String riskLevel,
            boolean safe,
            boolean dryRunSupported,
            List<String> requiredConfig,
            String message,
            String severity,
            int priority) {
        return new HermesOperationalAction(
                "learning-audit-retention",
                actionId,
                severity,
                priority,
                riskLevel,
                safe,
                dryRunSupported,
                requiredConfig,
                message,
                Map.of());
    }
}
