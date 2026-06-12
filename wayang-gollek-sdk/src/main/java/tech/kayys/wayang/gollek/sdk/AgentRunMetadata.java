package tech.kayys.wayang.gollek.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class AgentRunMetadata {

    static final String TENANT = "tenant";
    static final String TENANT_ID = "tenantId";
    static final String SESSION = "session";
    static final String SESSION_ID = "sessionId";
    static final String SURFACE = "surface";
    static final String SURFACE_ID = "surfaceId";
    static final String PROFILE = "profile";
    static final String PROFILE_ID = "profileId";
    static final String WAYANG_PROFILE = "wayang.profile";

    private AgentRunMetadata() {
    }

    static String tenant(AgentRunStatus status) {
        return value(status, TENANT, TENANT_ID);
    }

    static String session(AgentRunStatus status) {
        return value(status, SESSION, SESSION_ID);
    }

    static String surface(AgentRunStatus status) {
        return value(status, SURFACE, SURFACE_ID);
    }

    static String profile(AgentRunStatus status) {
        return value(status, PROFILE, PROFILE_ID, WAYANG_PROFILE);
    }

    static String profile(Map<String, Object> metadata) {
        return value(metadata, PROFILE, PROFILE_ID, WAYANG_PROFILE);
    }

    static String profileContext(Map<String, Object> context) {
        return value(context, WAYANG_PROFILE, PROFILE, PROFILE_ID);
    }

    static void putProfileAliases(Map<String, Object> metadata, String profileId) {
        if (metadata == null) {
            return;
        }
        String normalized = SdkText.trimToEmpty(profileId);
        if (!normalized.isEmpty()) {
            metadata.put(PROFILE, normalized);
            metadata.put(PROFILE_ID, normalized);
        }
    }

    static boolean matches(AgentRunStatus status, String expected, String... keys) {
        String normalizedExpected = SdkText.trimToEmpty(expected);
        if (normalizedExpected.isEmpty()) {
            return true;
        }
        if (status == null) {
            return false;
        }
        Map<String, Object> metadata = status.metadata();
        for (String key : keys) {
            if (normalizedExpected.equals(value(metadata, key))) {
                return true;
            }
        }
        return false;
    }

    static Map<String, Integer> count(List<AgentRunStatus> statuses, Function<AgentRunStatus, String> classifier) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (AgentRunStatus status : SdkLists.copy(statuses)) {
            if (status == null) {
                continue;
            }
            String key = SdkText.trimToEmpty(classifier.apply(status));
            if (!key.isEmpty()) {
                counts.put(key, counts.getOrDefault(key, 0) + 1);
            }
        }
        return counts.isEmpty() ? Map.of() : Collections.unmodifiableMap(counts);
    }

    static String value(AgentRunStatus status, String... keys) {
        return status == null ? "" : value(status.metadata(), keys);
    }

    static String value(Map<String, Object> metadata, String... keys) {
        if (metadata == null || metadata.isEmpty()) {
            return "";
        }
        for (String key : keys) {
            Object value = metadata.get(key);
            String normalized = SdkText.trimToEmpty(value == null ? "" : String.valueOf(value));
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        return "";
    }
}
