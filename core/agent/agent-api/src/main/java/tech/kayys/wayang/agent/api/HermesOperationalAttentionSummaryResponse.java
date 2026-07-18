package tech.kayys.wayang.agent.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate REST projection over structured Hermes operational attention items.
 */
public record HermesOperationalAttentionSummaryResponse(
        int totalItems,
        int retryableItems,
        int highestPriority,
        boolean requiresAttention,
        List<String> actions,
        Map<String, Long> sourceCounts,
        Map<String, Long> severityCounts) {

    public HermesOperationalAttentionSummaryResponse {
        totalItems = Math.max(totalItems, 0);
        retryableItems = Math.max(retryableItems, 0);
        highestPriority = Math.max(highestPriority, 0);
        requiresAttention = requiresAttention || totalItems > 0;
        actions = actions == null ? List.of() : List.copyOf(actions);
        sourceCounts = sourceCounts == null ? Map.of() : Map.copyOf(sourceCounts);
        severityCounts = severityCounts == null ? Map.of() : Map.copyOf(severityCounts);
    }

    static HermesOperationalAttentionSummaryResponse empty() {
        return from(List.of());
    }

    static HermesOperationalAttentionSummaryResponse from(List<HermesOperationalAttention> items) {
        List<HermesOperationalAttention> values = items == null
                ? List.of()
                : items.stream()
                        .filter(item -> item != null)
                        .toList();
        return new HermesOperationalAttentionSummaryResponse(
                values.size(),
                (int) values.stream().filter(HermesOperationalAttention::retryable).count(),
                values.stream()
                        .mapToInt(HermesOperationalAttention::priority)
                        .max()
                        .orElse(0),
                !values.isEmpty(),
                actions(values),
                counts(values, true),
                counts(values, false));
    }

    private static List<String> actions(List<HermesOperationalAttention> items) {
        return items.stream()
                .map(HermesOperationalAttention::action)
                .map(value -> HermesResponseMetadata.text(value, ""))
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    private static Map<String, Long> counts(
            List<HermesOperationalAttention> items,
            boolean sourceCounts) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (HermesOperationalAttention item : items) {
            String value = sourceCounts ? item.source() : item.severity();
            counts.merge(HermesResponseMetadata.text(value, "unknown"), 1L, Long::sum);
        }
        return Map.copyOf(counts);
    }
}
