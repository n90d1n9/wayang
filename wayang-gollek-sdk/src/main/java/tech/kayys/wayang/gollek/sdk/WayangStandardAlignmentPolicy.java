package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deployment/readiness policy for a standard-alignment portfolio.
 */
public record WayangStandardAlignmentPolicy(
        List<String> requiredStandardIds,
        List<String> warningGapCategories,
        Map<String, String> requiredVersions) {

    public WayangStandardAlignmentPolicy {
        requiredVersions = canonicalVersions(requiredVersions);
        requiredStandardIds = canonicalStandardIds(requiredStandardIds, requiredVersions.keySet());
        warningGapCategories = normalizedCategories(warningGapCategories);
    }

    public WayangStandardAlignmentPolicy(
            List<String> requiredStandardIds,
            List<String> warningGapCategories) {
        this(requiredStandardIds, warningGapCategories, Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static WayangStandardAlignmentPolicy strict(String... requiredStandardIds) {
        return builder()
                .requiredStandards(requiredStandardIds)
                .build();
    }

    public WayangStandardAlignmentPolicyAssessment assess(WayangStandardAlignmentPortfolio portfolio) {
        WayangStandardAlignmentPortfolio resolved = portfolio == null
                ? WayangStandardAlignmentPortfolio.builder().build()
                : portfolio;
        List<String> present = resolved.standardIds();
        List<String> missing = requiredStandardIds.stream()
                .filter(required -> !present.contains(required))
                .toList();
        Map<String, WayangStandardAlignmentSummary> byStandardId = summariesByStandardId(resolved);
        Map<String, String> actualVersions = actualVersions(byStandardId);
        List<String> versionMismatches = versionMismatchStandardIds(byStandardId);
        List<String> failing = resolved.standards().stream()
                .filter(this::blocking)
                .map(WayangStandardAlignmentSummary::standardId)
                .toList();
        List<String> warnings = resolved.standards().stream()
                .filter(this::warningOnly)
                .map(WayangStandardAlignmentSummary::standardId)
                .toList();
        List<String> recommendations = recommendations(missing, versionMismatches, failing, warnings);
        return new WayangStandardAlignmentPolicyAssessment(
                missing.isEmpty() && versionMismatches.isEmpty() && failing.isEmpty(),
                requiredStandardIds,
                present,
                missing,
                failing,
                warnings,
                requiredVersions,
                actualVersions,
                versionMismatches,
                recommendations);
    }

    private boolean blocking(WayangStandardAlignmentSummary summary) {
        if (summary == null || summary.aligned() && !summary.hasGaps()) {
            return false;
        }
        return !warningOnly(summary);
    }

    private boolean warningOnly(WayangStandardAlignmentSummary summary) {
        if (summary == null || !summary.hasGaps() || warningGapCategories.isEmpty()) {
            return false;
        }
        List<String> categories = normalizedCategories(summary.gapCategories());
        return !categories.isEmpty() && warningGapCategories.containsAll(categories);
    }

    private List<String> recommendations(
            List<String> missing,
            List<String> versionMismatches,
            List<String> failing,
            List<String> warnings) {
        List<String> recommendations = new ArrayList<>();
        missing.forEach(standard -> recommendations.add("Add alignment report for required standard: " + standard + "."));
        versionMismatches.forEach(standard -> recommendations.add("Update alignment report for standard "
                + standard
                + " to required version "
                + requiredVersions.get(standard)
                + "."));
        failing.forEach(standard -> recommendations.add("Resolve blocking alignment gaps for standard: " + standard + "."));
        warnings.forEach(standard -> recommendations.add("Review warning-only alignment gaps for standard: " + standard + "."));
        return List.copyOf(recommendations);
    }

    private List<String> versionMismatchStandardIds(Map<String, WayangStandardAlignmentSummary> byStandardId) {
        return requiredVersions.entrySet().stream()
                .filter(entry -> byStandardId.containsKey(entry.getKey()))
                .filter(entry -> !entry.getValue().equals(actualVersion(byStandardId.get(entry.getKey()))))
                .map(Map.Entry::getKey)
                .toList();
    }

    private Map<String, String> actualVersions(Map<String, WayangStandardAlignmentSummary> byStandardId) {
        Map<String, String> values = new LinkedHashMap<>();
        requiredVersions.keySet().stream()
                .filter(byStandardId::containsKey)
                .forEach(standardId -> values.put(standardId, actualVersion(byStandardId.get(standardId))));
        return values.isEmpty() ? Map.of() : Collections.unmodifiableMap(values);
    }

    private static String actualVersion(WayangStandardAlignmentSummary summary) {
        return summary == null ? "" : SdkText.trimToEmpty(summary.standard().version());
    }

    private static Map<String, WayangStandardAlignmentSummary> summariesByStandardId(
            WayangStandardAlignmentPortfolio portfolio) {
        Map<String, WayangStandardAlignmentSummary> values = new LinkedHashMap<>();
        portfolio.standards().forEach(summary -> values.putIfAbsent(summary.standardId(), summary));
        return values;
    }

    private static List<String> canonicalStandardIds(
            List<String> standardIds,
            Set<String> versionRequiredStandardIds) {
        Set<String> values = new LinkedHashSet<>();
        if (standardIds != null) {
            standardIds.stream()
                    .map(WayangStandardRegistry::canonicalId)
                    .map(SdkText::trimToEmpty)
                    .filter(value -> !value.isEmpty())
                    .forEach(values::add);
        }
        values.addAll(versionRequiredStandardIds);
        return List.copyOf(values);
    }

    private static List<String> normalizedCategories(List<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return List.of();
        }
        Set<String> values = new LinkedHashSet<>();
        categories.stream()
                .map(SdkText::trimToEmpty)
                .map(String::toLowerCase)
                .filter(value -> !value.isEmpty())
                .forEach(values::add);
        return List.copyOf(values);
    }

    private static Map<String, String> canonicalVersions(Map<String, String> versions) {
        if (versions == null || versions.isEmpty()) {
            return Map.of();
        }
        Map<String, String> values = new LinkedHashMap<>();
        versions.forEach((standardId, version) -> {
            String normalizedStandardId = WayangStandardRegistry.canonicalId(standardId);
            String normalizedVersion = SdkText.trimToEmpty(version);
            if (!normalizedStandardId.isBlank() && !normalizedVersion.isEmpty()) {
                values.put(normalizedStandardId, normalizedVersion);
            }
        });
        return values.isEmpty() ? Map.of() : Collections.unmodifiableMap(values);
    }

    public static final class Builder {
        private final List<String> requiredStandardIds = new ArrayList<>();
        private final List<String> warningGapCategories = new ArrayList<>();
        private final Map<String, String> requiredVersions = new LinkedHashMap<>();

        private Builder() {
        }

        public Builder requiredStandard(String standardId) {
            String normalized = WayangStandardRegistry.canonicalId(standardId);
            if (!normalized.isBlank()) {
                requiredStandardIds.add(normalized);
            }
            return this;
        }

        public Builder requiredStandards(String... standardIds) {
            return requiredStandards(standardIds == null ? List.of() : Arrays.asList(standardIds));
        }

        public Builder requiredStandards(List<String> standardIds) {
            if (standardIds != null) {
                standardIds.forEach(this::requiredStandard);
            }
            return this;
        }

        public Builder requiredStandardVersion(String standardId, String version) {
            String normalizedStandardId = WayangStandardRegistry.canonicalId(standardId);
            String normalizedVersion = SdkText.trimToEmpty(version);
            if (!normalizedStandardId.isBlank() && !normalizedVersion.isEmpty()) {
                requiredStandard(normalizedStandardId);
                requiredVersions.put(normalizedStandardId, normalizedVersion);
            }
            return this;
        }

        public Builder requiredStandardVersions(Map<String, String> versions) {
            if (versions != null) {
                versions.forEach(this::requiredStandardVersion);
            }
            return this;
        }

        public Builder warningGapCategory(String category) {
            String normalized = SdkText.trimToEmpty(category).toLowerCase();
            if (!normalized.isEmpty()) {
                warningGapCategories.add(normalized);
            }
            return this;
        }

        public Builder warningGapCategories(String... categories) {
            return warningGapCategories(categories == null ? List.of() : Arrays.asList(categories));
        }

        public Builder warningGapCategories(List<String> categories) {
            if (categories != null) {
                categories.forEach(this::warningGapCategory);
            }
            return this;
        }

        public WayangStandardAlignmentPolicy build() {
            return new WayangStandardAlignmentPolicy(requiredStandardIds, warningGapCategories, requiredVersions);
        }
    }
}
