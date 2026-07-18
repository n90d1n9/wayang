package tech.kayys.wayang.boundry;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;

/**
 * Describes one SDK boundary catalog validation issue with the affected
 * boundary, field, and value when those details are known.
 */
public record WayangSdkBoundaryCatalogValidationIssue(
        String kind,
        String message,
        String boundaryId,
        String field,
        String value) {

    public WayangSdkBoundaryCatalogValidationIssue {
        kind = normalizeRequired("Issue kind", kind);
        message = normalizeRequired("Issue message", message);
        boundaryId = SdkText.trimToEmpty(boundaryId);
        field = SdkText.trimToEmpty(field);
        value = SdkText.trimToEmpty(value);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("kind", kind);
        values.put("message", message);
        values.put("boundaryId", boundaryId);
        values.put("field", field);
        values.put("value", value);
        return SdkMaps.orderedCopy(values);
    }

    private static String normalizeRequired(String label, String value) {
        String normalized = SdkText.trimToEmpty(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return normalized;
    }
}
