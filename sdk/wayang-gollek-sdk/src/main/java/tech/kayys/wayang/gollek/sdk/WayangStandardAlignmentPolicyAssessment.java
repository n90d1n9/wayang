package tech.kayys.wayang.gollek.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of applying a deployment/readiness policy to a standard-alignment portfolio.
 */
public record WayangStandardAlignmentPolicyAssessment(
        boolean ready,
        List<String> requiredStandardIds,
        List<String> presentStandardIds,
        List<String> missingStandardIds,
        List<String> failingStandardIds,
        List<String> warningStandardIds,
        Map<String, String> requiredVersions,
        Map<String, String> actualVersions,
        List<String> versionMismatchStandardIds,
        List<String> recommendations) {

    public WayangStandardAlignmentPolicyAssessment {
        requiredStandardIds = SdkLists.copy(requiredStandardIds);
        presentStandardIds = SdkLists.copy(presentStandardIds);
        missingStandardIds = SdkLists.copy(missingStandardIds);
        failingStandardIds = SdkLists.copy(failingStandardIds);
        warningStandardIds = SdkLists.copy(warningStandardIds);
        requiredVersions = stringMap(requiredVersions);
        actualVersions = stringMap(actualVersions);
        versionMismatchStandardIds = SdkLists.copy(versionMismatchStandardIds);
        recommendations = SdkLists.copy(recommendations);
    }

    public WayangStandardAlignmentPolicyAssessment(
            boolean ready,
            List<String> requiredStandardIds,
            List<String> presentStandardIds,
            List<String> missingStandardIds,
            List<String> failingStandardIds,
            List<String> warningStandardIds,
            List<String> recommendations) {
        this(
                ready,
                requiredStandardIds,
                presentStandardIds,
                missingStandardIds,
                failingStandardIds,
                warningStandardIds,
                Map.of(),
                Map.of(),
                List.of(),
                recommendations);
    }

    public boolean hasFailures() {
        return !missingStandardIds.isEmpty()
                || !failingStandardIds.isEmpty()
                || !versionMismatchStandardIds.isEmpty();
    }

    public boolean hasWarnings() {
        return !warningStandardIds.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready);
        values.put("requiredStandardIds", requiredStandardIds);
        values.put("presentStandardIds", presentStandardIds);
        values.put("missingStandardIds", missingStandardIds);
        values.put("failingStandardIds", failingStandardIds);
        values.put("warningStandardIds", warningStandardIds);
        values.put("requiredVersions", requiredVersions);
        values.put("actualVersions", actualVersions);
        values.put("versionMismatchStandardIds", versionMismatchStandardIds);
        values.put("recommendations", recommendations);
        return SdkMaps.orderedCopy(values);
    }

    private static Map<String, String> stringMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, String> copy = new LinkedHashMap<>();
        values.forEach((key, value) -> {
            String normalizedKey = SdkText.trimToEmpty(key);
            String normalizedValue = SdkText.trimToEmpty(value);
            if (!normalizedKey.isEmpty() && !normalizedValue.isEmpty()) {
                copy.put(normalizedKey, normalizedValue);
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }
}
