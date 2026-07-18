package tech.kayys.wayang.agent.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate REST projection over structured Hermes operational actions.
 */
public record HermesOperationalActionSummaryResponse(
        int totalActions,
        int safeActions,
        int unsafeActions,
        int dryRunSupportedActions,
        boolean requiresOperatorApproval,
        boolean requiresConfiguration,
        List<String> requiredConfig,
        Map<String, Long> riskLevelCounts) {

    public HermesOperationalActionSummaryResponse {
        totalActions = Math.max(totalActions, 0);
        safeActions = Math.max(safeActions, 0);
        unsafeActions = Math.max(unsafeActions, 0);
        dryRunSupportedActions = Math.max(dryRunSupportedActions, 0);
        requiredConfig = requiredConfig == null ? List.of() : List.copyOf(requiredConfig);
        requiresOperatorApproval = requiresOperatorApproval || unsafeActions > 0;
        requiresConfiguration = requiresConfiguration || !requiredConfig.isEmpty();
        riskLevelCounts = riskLevelCounts == null ? Map.of() : Map.copyOf(riskLevelCounts);
    }

    static HermesOperationalActionSummaryResponse empty() {
        return from(List.of());
    }

    static HermesOperationalActionSummaryResponse from(List<HermesOperationalAction> actions) {
        List<HermesOperationalAction> values = actions == null
                ? List.of()
                : actions.stream()
                        .filter(action -> action != null)
                        .toList();
        List<String> requiredConfig = values.stream()
                .flatMap(action -> action.requiredConfig().stream())
                .map(value -> HermesResponseMetadata.text(value, ""))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        int safeActions = (int) values.stream().filter(HermesOperationalAction::safe).count();
        int unsafeActions = values.size() - safeActions;
        return new HermesOperationalActionSummaryResponse(
                values.size(),
                safeActions,
                unsafeActions,
                (int) values.stream().filter(HermesOperationalAction::dryRunSupported).count(),
                unsafeActions > 0,
                !requiredConfig.isEmpty(),
                requiredConfig,
                riskLevelCounts(values));
    }

    private static Map<String, Long> riskLevelCounts(List<HermesOperationalAction> actions) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (HermesOperationalAction action : actions) {
            String riskLevel = HermesResponseMetadata.text(action.riskLevel(), "unknown");
            counts.merge(riskLevel, 1L, Long::sum);
        }
        return Map.copyOf(counts);
    }
}
