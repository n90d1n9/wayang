package tech.kayys.wayang.boundry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkText;

/**
 * Describes one SDK ownership boundary before the public package is physically
 * split into smaller domains.
 */
public record WayangSdkBoundary(
        String id,
        String name,
        String intendedPackage,
        String responsibility,
        List<String> classPrefixes,
        List<String> contractSchemas,
        List<String> dependsOn) {

    public WayangSdkBoundary {
        id = required("id", id);
        name = required("name", name);
        intendedPackage = required("intendedPackage", intendedPackage);
        responsibility = required("responsibility", responsibility);
        classPrefixes = copyTextValues(classPrefixes);
        contractSchemas = copyTextValues(contractSchemas);
        dependsOn = copyTextValues(dependsOn);
    }

    public boolean ownsClassName(String simpleClassName) {
        String normalized = SdkText.trimToEmpty(simpleClassName);
        if (normalized.isEmpty()) {
            return false;
        }
        return classPrefixes.stream()
                .anyMatch(prefix -> normalized.equals(prefix) || normalized.startsWith(prefix));
    }

    public boolean ownsContractSchema(String schema) {
        String normalized = SdkText.trimToEmpty(schema);
        return !normalized.isEmpty() && contractSchemas.contains(normalized);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", id);
        values.put("name", name);
        values.put("intendedPackage", intendedPackage);
        values.put("responsibility", responsibility);
        values.put("classPrefixes", classPrefixes);
        values.put("contractSchemas", contractSchemas);
        values.put("dependsOn", dependsOn);
        return values;
    }

    private static String required(String field, String value) {
        String normalized = SdkText.trimToEmpty(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Wayang SDK boundary " + field + " is required.");
        }
        return normalized;
    }

    private static List<String> copyTextValues(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(SdkText::trimToEmpty)
                .filter(value -> !value.isEmpty())
                .distinct()
                .toList();
    }
}
