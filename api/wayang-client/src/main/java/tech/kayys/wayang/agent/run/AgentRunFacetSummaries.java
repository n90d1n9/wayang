package tech.kayys.wayang.agent.run;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import tech.kayys.wayang.client.SdkText;

final class AgentRunFacetSummaries {

    private AgentRunFacetSummaries() {
    }

    static <T> List<T> fromCounts(Map<String, Integer> counts, BiFunction<String, Integer, T> mapper) {
        if (counts == null || counts.isEmpty()) {
            return List.of();
        }
        return counts.entrySet().stream()
                .map(entry -> new FacetCount(
                        SdkText.trimToEmpty(entry.getKey()),
                        entry.getValue() == null ? 0 : entry.getValue()))
                .filter(facet -> !facet.name().isEmpty() && facet.count() > 0)
                .sorted((left, right) -> {
                    int byCount = Integer.compare(right.count(), left.count());
                    return byCount == 0 ? left.name().compareTo(right.name()) : byCount;
                })
                .map(facet -> mapper.apply(facet.name(), facet.count()))
                .toList();
    }

    private record FacetCount(String name, int count) {
        private FacetCount {
            name = SdkText.trimToEmpty(name);
            count = Math.max(0, count);
        }
    }
}
