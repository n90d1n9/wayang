package tech.kayys.wayang.readiness;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;

public record WayangPlatformReadinessProfileRegistryConfigIssue(
        String code,
        String field,
        String message) {

    public WayangPlatformReadinessProfileRegistryConfigIssue {
        code = SdkText.trimToDefault(code, "readiness_profile_registry_config_issue");
        field = SdkText.trimToEmpty(field);
        message = SdkText.trimToDefault(message, code.replace('_', ' '));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("code", code);
        values.put("field", field);
        values.put("message", message);
        return SdkMaps.copy(values);
    }
}
