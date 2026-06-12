package tech.kayys.wayang.gollek.sdk;

import java.util.Arrays;
import java.util.List;

/**
 * Reusable policy presets for standard-alignment readiness gates.
 */
public final class WayangStandardAlignmentPolicies {

    private WayangStandardAlignmentPolicies() {
    }

    public static WayangStandardAlignmentPolicy.Builder builder() {
        return WayangStandardAlignmentPolicy.builder();
    }

    public static WayangStandardAlignmentPolicy none() {
        return builder().build();
    }

    public static WayangStandardAlignmentPolicy strict(String... requiredStandardIds) {
        return WayangStandardAlignmentPolicy.strict(requiredStandardIds);
    }

    public static WayangStandardAlignmentPolicy pinnedKnownStandards() {
        return pinnedRegistryBuilder(List.of()).build();
    }

    public static WayangStandardAlignmentPolicy pinnedRegistry(String... standardIds) {
        return pinnedRegistry(standardIds == null ? List.of() : Arrays.asList(standardIds));
    }

    public static WayangStandardAlignmentPolicy pinnedRegistry(List<String> standardIds) {
        return pinnedRegistryBuilder(standardIds).build();
    }

    static WayangStandardAlignmentPolicy.Builder pinnedRegistryBuilder(List<String> standardIds) {
        WayangStandardAlignmentPolicy.Builder builder = builder();
        if (standardIds == null || standardIds.isEmpty()) {
            WayangStandardRegistry.knownStandards()
                    .forEach(definition -> requireRegistryVersion(builder, definition));
            return builder;
        }
        standardIds.forEach(standardId -> requirePinnedStandard(builder, standardId));
        return builder;
    }

    private static void requirePinnedStandard(
            WayangStandardAlignmentPolicy.Builder builder,
            String standardId) {
        WayangStandardRegistry.find(standardId)
                .ifPresentOrElse(
                        definition -> requireRegistryVersion(builder, definition),
                        () -> builder.requiredStandard(standardId));
    }

    private static void requireRegistryVersion(
            WayangStandardAlignmentPolicy.Builder builder,
            WayangStandardDefinition definition) {
        if (definition.version().isEmpty()) {
            builder.requiredStandard(definition.standardId());
            return;
        }
        builder.requiredStandardVersion(definition.standardId(), definition.version());
    }
}
