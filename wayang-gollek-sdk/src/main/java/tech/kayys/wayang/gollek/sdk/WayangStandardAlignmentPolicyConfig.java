package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deployment-facing configuration for building standard-alignment readiness policies.
 */
public record WayangStandardAlignmentPolicyConfig(
        Mode mode,
        List<String> standardIds,
        List<String> warningGapCategories,
        Map<String, String> requiredVersions,
        WayangStandardRegistryDriftMode registryDriftMode,
        List<String> requiredProviderIds,
        int minimumProviderCount,
        WayangStandardAlignmentProviderIssueMode providerIssueMode) {

    public enum Mode {
        NONE,
        STRICT,
        PINNED_REGISTRY,
        PINNED_KNOWN_STANDARDS;

        public static Mode fromId(String id) {
            return switch (modeKey(id)) {
                case "", "none", "off", "disabled", "disable" -> NONE;
                case "strict", "required", "require", "requiredstandards" -> STRICT;
                case "pinned", "registry", "pinnedregistry", "registryversions" -> PINNED_REGISTRY;
                case "all", "allknown", "known", "pinnedknown", "knownstandards", "pinnedall",
                        "pinnedknownstandards" ->
                        PINNED_KNOWN_STANDARDS;
                default -> NONE;
            };
        }
    }

    public WayangStandardAlignmentPolicyConfig {
        mode = mode == null ? Mode.NONE : mode;
        standardIds = normalizedStrings(standardIds);
        warningGapCategories = normalizedStrings(warningGapCategories);
        requiredVersions = normalizedVersions(requiredVersions);
        registryDriftMode = registryDriftMode == null
                ? WayangStandardRegistryDriftMode.IGNORE
                : registryDriftMode;
        requiredProviderIds = normalizedStrings(requiredProviderIds);
        minimumProviderCount = Math.max(0, minimumProviderCount);
        providerIssueMode = providerIssueMode == null
                ? WayangStandardAlignmentProviderIssueMode.WARN
                : providerIssueMode;
    }

    public WayangStandardAlignmentPolicyConfig(
            Mode mode,
            List<String> standardIds,
            List<String> warningGapCategories,
            Map<String, String> requiredVersions) {
        this(
                mode,
                standardIds,
                warningGapCategories,
                requiredVersions,
                WayangStandardRegistryDriftMode.IGNORE,
                List.of(),
                0,
                WayangStandardAlignmentProviderIssueMode.WARN);
    }

    public WayangStandardAlignmentPolicyConfig(
            Mode mode,
            List<String> standardIds,
            List<String> warningGapCategories,
            Map<String, String> requiredVersions,
            WayangStandardRegistryDriftMode registryDriftMode) {
        this(
                mode,
                standardIds,
                warningGapCategories,
                requiredVersions,
                registryDriftMode,
                List.of(),
                0,
                WayangStandardAlignmentProviderIssueMode.WARN);
    }

    public static WayangStandardAlignmentPolicyConfig none() {
        return builder().build();
    }

    public static WayangStandardAlignmentPolicyConfig strict(String... standardIds) {
        return builder()
                .mode(Mode.STRICT)
                .standardIds(standardIds)
                .build();
    }

    public static WayangStandardAlignmentPolicyConfig pinnedRegistry(String... standardIds) {
        return builder()
                .mode(Mode.PINNED_REGISTRY)
                .standardIds(standardIds)
                .build();
    }

    public static WayangStandardAlignmentPolicyConfig pinnedKnownStandards() {
        return builder()
                .mode(Mode.PINNED_KNOWN_STANDARDS)
                .build();
    }

    public static WayangStandardAlignmentPolicyConfig fromMap(Map<?, ?> values) {
        Map<String, Object> map = WayangStandardAlignmentMaps.copy(values);
        return builder()
                .mode(Mode.fromId(WayangStandardAlignmentMaps.firstText(map, "mode", "policyMode")))
                .standardIds(firstList(
                        map,
                        "standardIds",
                        "standard_ids",
                        "requiredStandardIds",
                        "required_standard_ids",
                        "standards"))
                .warningGapCategories(firstList(
                        map,
                        "warningGapCategories",
                        "warning_gap_categories",
                        "warningCategories",
                        "warning_categories",
                        "warningGaps",
                        "warning_gaps"))
                .requiredVersions(firstMap(
                        map,
                        "requiredVersions",
                        "required_versions",
                        "versionRequirements",
                        "version_requirements",
                        "versions"))
                .registryDriftMode(WayangStandardRegistryDriftMode.fromId(WayangStandardAlignmentMaps.firstText(
                        map,
                        "registryDriftMode",
                        "registry_drift_mode",
                        "driftMode",
                        "drift_mode")))
                .requiredProviderIds(firstList(
                        map,
                        "requiredProviderIds",
                        "required_provider_ids",
                        "providerIds",
                        "provider_ids",
                        "providers"))
                .minimumProviderCount(firstInt(
                        map,
                        "minimumProviderCount",
                        "minimum_provider_count",
                        "minProviderCount",
                        "min_provider_count",
                        "providerCount",
                        "provider_count"))
                .providerIssueMode(WayangStandardAlignmentProviderIssueMode.fromId(
                        WayangStandardAlignmentMaps.firstText(
                                map,
                                "providerIssueMode",
                                "provider_issue_mode",
                                "providerIssues",
                                "provider_issues",
                                "providerIssuePolicy",
                                "provider_issue_policy")))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public WayangStandardAlignmentPolicy toPolicy() {
        WayangStandardAlignmentPolicy.Builder builder = switch (mode) {
            case NONE -> WayangStandardAlignmentPolicies.builder();
            case STRICT -> WayangStandardAlignmentPolicies.builder()
                    .requiredStandards(standardIds);
            case PINNED_REGISTRY -> WayangStandardAlignmentPolicies.pinnedRegistryBuilder(standardIds);
            case PINNED_KNOWN_STANDARDS -> WayangStandardAlignmentPolicies.pinnedRegistryBuilder(List.of());
        };
        return builder
                .warningGapCategories(warningGapCategories)
                .requiredStandardVersions(requiredVersions)
                .build();
    }

    public WayangStandardAlignmentProviderPolicy toProviderPolicy() {
        return new WayangStandardAlignmentProviderPolicy(
                requiredProviderIds,
                minimumProviderCount,
                providerIssueMode);
    }

    public String modeId() {
        return switch (mode) {
            case NONE -> "none";
            case STRICT -> "strict";
            case PINNED_REGISTRY -> "pinnedRegistry";
            case PINNED_KNOWN_STANDARDS -> "pinnedKnownStandards";
        };
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", modeId());
        values.put("standardIds", standardIds);
        values.put("warningGapCategories", warningGapCategories);
        values.put("requiredVersions", requiredVersions);
        values.put("registryDriftMode", registryDriftMode.id());
        values.put("requiredProviderIds", requiredProviderIds);
        values.put("minimumProviderCount", minimumProviderCount);
        values.put("providerIssueMode", providerIssueMode.id());
        return SdkMaps.orderedCopy(values);
    }

    private static List<String> firstList(Map<?, ?> values, String... keys) {
        for (String key : keys) {
            List<String> list = textList(values.get(key));
            if (!list.isEmpty()) {
                return list;
            }
        }
        return List.of();
    }

    private static Map<String, String> firstMap(Map<?, ?> values, String... keys) {
        for (String key : keys) {
            Map<String, Object> map = WayangStandardAlignmentMaps.map(values.get(key));
            if (!map.isEmpty()) {
                Map<String, String> textMap = new LinkedHashMap<>();
                map.forEach((mapKey, value) -> {
                    String normalizedValue = SdkText.trimToEmpty(String.valueOf(value));
                    if (!normalizedValue.isEmpty()) {
                        textMap.put(mapKey, normalizedValue);
                    }
                });
                return textMap;
            }
        }
        return Map.of();
    }

    private static int firstInt(Map<?, ?> values, String... keys) {
        for (String key : keys) {
            Object value = values.get(key);
            if (value == null) {
                continue;
            }
            if (value instanceof Number number) {
                return Math.max(0, number.intValue());
            }
            String text = SdkText.trimToEmpty(String.valueOf(value));
            if (!text.isEmpty()) {
                try {
                    return Math.max(0, Integer.parseInt(text));
                } catch (NumberFormatException ignored) {
                    return 0;
                }
            }
        }
        return 0;
    }

    private static List<String> normalizedStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> copy = new LinkedHashSet<>();
        values.stream()
                .map(SdkText::trimToEmpty)
                .filter(value -> !value.isEmpty())
                .forEach(copy::add);
        return copy.isEmpty() ? List.of() : List.copyOf(copy);
    }

    private static Map<String, String> normalizedVersions(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        values.forEach((standardId, version) -> {
            String normalizedStandardId = WayangStandardRegistry.canonicalId(standardId);
            String normalizedVersion = SdkText.trimToEmpty(version);
            if (!normalizedStandardId.isBlank() && !normalizedVersion.isEmpty()) {
                copy.put(normalizedStandardId, normalizedVersion);
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }

    public static final class Builder {
        private Mode mode = Mode.NONE;
        private final List<String> standardIds = new ArrayList<>();
        private final List<String> warningGapCategories = new ArrayList<>();
        private final Map<String, String> requiredVersions = new LinkedHashMap<>();
        private WayangStandardRegistryDriftMode registryDriftMode = WayangStandardRegistryDriftMode.IGNORE;
        private final List<String> requiredProviderIds = new ArrayList<>();
        private int minimumProviderCount;
        private WayangStandardAlignmentProviderIssueMode providerIssueMode =
                WayangStandardAlignmentProviderIssueMode.WARN;

        private Builder() {
        }

        public Builder mode(Mode mode) {
            this.mode = mode == null ? Mode.NONE : mode;
            return this;
        }

        public Builder mode(String mode) {
            return mode(Mode.fromId(mode));
        }

        public Builder standardId(String standardId) {
            String normalized = SdkText.trimToEmpty(standardId);
            if (!normalized.isEmpty()) {
                standardIds.add(normalized);
            }
            return this;
        }

        public Builder standardIds(String... standardIds) {
            return standardIds(standardIds == null ? List.of() : Arrays.asList(standardIds));
        }

        public Builder standardIds(List<String> standardIds) {
            if (standardIds != null) {
                standardIds.forEach(this::standardId);
            }
            return this;
        }

        public Builder warningGapCategory(String category) {
            String normalized = SdkText.trimToEmpty(category);
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

        public Builder requiredStandardVersion(String standardId, String version) {
            String normalizedStandardId = WayangStandardRegistry.canonicalId(standardId);
            String normalizedVersion = SdkText.trimToEmpty(version);
            if (!normalizedStandardId.isBlank() && !normalizedVersion.isEmpty()) {
                requiredVersions.put(normalizedStandardId, normalizedVersion);
            }
            return this;
        }

        public Builder requiredVersions(Map<String, String> versions) {
            if (versions != null) {
                versions.forEach(this::requiredStandardVersion);
            }
            return this;
        }

        public Builder registryDriftMode(WayangStandardRegistryDriftMode mode) {
            this.registryDriftMode = mode == null ? WayangStandardRegistryDriftMode.IGNORE : mode;
            return this;
        }

        public Builder registryDriftMode(String mode) {
            return registryDriftMode(WayangStandardRegistryDriftMode.fromId(mode));
        }

        public Builder requiredProviderId(String providerId) {
            String normalized = SdkText.trimToEmpty(providerId);
            if (!normalized.isEmpty()) {
                requiredProviderIds.add(normalized);
            }
            return this;
        }

        public Builder requiredProviderIds(String... providerIds) {
            return requiredProviderIds(providerIds == null ? List.of() : Arrays.asList(providerIds));
        }

        public Builder requiredProviderIds(List<String> providerIds) {
            if (providerIds != null) {
                providerIds.forEach(this::requiredProviderId);
            }
            return this;
        }

        public Builder minimumProviderCount(int count) {
            this.minimumProviderCount = Math.max(0, count);
            return this;
        }

        public Builder providerIssueMode(WayangStandardAlignmentProviderIssueMode mode) {
            this.providerIssueMode = mode == null ? WayangStandardAlignmentProviderIssueMode.WARN : mode;
            return this;
        }

        public Builder providerIssueMode(String mode) {
            return providerIssueMode(WayangStandardAlignmentProviderIssueMode.fromId(mode));
        }

        public WayangStandardAlignmentPolicyConfig build() {
            return new WayangStandardAlignmentPolicyConfig(
                    mode,
                    standardIds,
                    warningGapCategories,
                    requiredVersions,
                    registryDriftMode,
                    requiredProviderIds,
                    minimumProviderCount,
                    providerIssueMode);
        }
    }

    private static String modeKey(String value) {
        return SdkText.trimToEmpty(value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "");
    }

    private static List<String> textList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream()
                    .flatMap(item -> splitText(item).stream())
                    .toList();
        }
        return splitText(value);
    }

    private static List<String> splitText(Object value) {
        String text = WayangStandardAlignmentMaps.text(value);
        if (text.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(text.split(","))
                .map(SdkText::trimToEmpty)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
