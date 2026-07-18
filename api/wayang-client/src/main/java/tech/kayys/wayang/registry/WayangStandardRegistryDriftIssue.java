package tech.kayys.wayang.registry;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;

/**
 * Field-level mismatch between a reported standard descriptor and the SDK registry.
 */
public record WayangStandardRegistryDriftIssue(
        String standardId,
        String field,
        String expected,
        String actual) {

    public WayangStandardRegistryDriftIssue {
        standardId = SdkText.trimToDefault(standardId, "unknown");
        field = SdkText.trimToDefault(field, "unknown");
        expected = SdkText.trimToEmpty(expected);
        actual = SdkText.trimToEmpty(actual);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("standardId", standardId);
        values.put("field", field);
        values.put("expected", expected);
        values.put("actual", actual);
        return SdkMaps.orderedCopy(values);
    }
}
