package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

record WayangA2aSpecAlignmentCategorySummaries(
        List<WayangA2aSpecAlignmentCategorySummary> summaries) {

    WayangA2aSpecAlignmentCategorySummaries {
        summaries = summaries == null
                ? List.of()
                : summaries.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    static WayangA2aSpecAlignmentCategorySummaries fromRequirements(
            List<WayangA2aSpecAlignmentRequirement> requirements) {
        Map<String, ArrayList<WayangA2aSpecAlignmentRequirement>> categories = new LinkedHashMap<>();
        if (requirements != null) {
            requirements.stream()
                    .filter(Objects::nonNull)
                    .forEach(requirement -> categories
                            .computeIfAbsent(requirement.category(), ignored -> new ArrayList<>())
                            .add(requirement));
        }
        return new WayangA2aSpecAlignmentCategorySummaries(categories.entrySet().stream()
                .map(entry -> WayangA2aSpecAlignmentCategorySummary.from(entry.getKey(), entry.getValue()))
                .toList());
    }

    static WayangA2aSpecAlignmentCategorySummaries fromSummaries(
            List<WayangA2aSpecAlignmentCategorySummary> summaries) {
        return new WayangA2aSpecAlignmentCategorySummaries(summaries);
    }

    Optional<WayangA2aSpecAlignmentCategorySummary> find(String category) {
        String normalized = WayangA2aMaps.optional(category);
        if (normalized == null) {
            return Optional.empty();
        }
        return summaries.stream()
                .filter(summary -> summary.category().equals(normalized))
                .findFirst();
    }

    List<WayangA2aSpecAlignmentCategorySummary> gaps() {
        return summaries.stream()
                .filter(WayangA2aSpecAlignmentCategorySummary::hasGaps)
                .toList();
    }

    List<String> gapCategories() {
        return gaps().stream()
                .map(WayangA2aSpecAlignmentCategorySummary::category)
                .toList();
    }

    List<Map<String, Object>> maps() {
        return summaries.stream()
                .map(WayangA2aSpecAlignmentCategorySummary::toMap)
                .toList();
    }
}
