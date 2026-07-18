package tech.kayys.wayang.code;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;

/**
 * Immutable session context shared with coding-agent extensions.
 *
 * <p>The SDK owns this contract so every Wayang surface can pass the same
 * information to OSS, pro, and enterprise extensions without coupling those
 * extensions to a particular CLI, API, or server implementation.</p>
 */
public record WayangCodeAgentContext(
        String surfaceId,
        String profileId,
        Path workspacePath,
        String projectId,
        String sessionId,
        String modelId,
        String providerId,
        boolean memoryEnabled,
        boolean harnessEnabled,
        int maxSteps,
        Map<String, Object> metadata) {

    public WayangCodeAgentContext {
        surfaceId = normalizeIdentifier(surfaceId, "coding-agent");
        profileId = normalizeIdentifier(profileId, "coding-agent");
        workspacePath = workspacePath == null
                ? Path.of(".").toAbsolutePath().normalize()
                : workspacePath.toAbsolutePath().normalize();
        projectId = SdkText.trimToEmpty(projectId);
        sessionId = SdkText.trimToEmpty(sessionId);
        modelId = SdkText.trimToDefault(modelId, "default");
        providerId = SdkText.trimToEmpty(providerId);
        maxSteps = Math.max(1, maxSteps);
        metadata = SdkMaps.copy(metadata);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("surfaceId", surfaceId);
        values.put("profileId", profileId);
        values.put("workspacePath", workspacePath.toString());
        values.put("projectId", projectId);
        values.put("sessionId", sessionId);
        values.put("modelId", modelId);
        values.put("providerId", providerId);
        values.put("memoryEnabled", memoryEnabled);
        values.put("harnessEnabled", harnessEnabled);
        values.put("maxSteps", maxSteps);
        values.put("metadata", metadata);
        return SdkMaps.orderedCopy(values);
    }

    private static String normalizeIdentifier(String value, String defaultValue) {
        String normalized = SdkText.trimToEmpty(value).toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? defaultValue : normalized;
    }

    /**
     * Builder for {@link WayangCodeAgentContext}.
     */
    public static final class Builder {
        private String surfaceId;
        private String profileId;
        private Path workspacePath;
        private String projectId;
        private String sessionId;
        private String modelId;
        private String providerId;
        private boolean memoryEnabled = true;
        private boolean harnessEnabled;
        private int maxSteps = 12;
        private final Map<String, Object> metadata = new LinkedHashMap<>();

        public Builder surfaceId(String surfaceId) {
            this.surfaceId = surfaceId;
            return this;
        }

        public Builder profileId(String profileId) {
            this.profileId = profileId;
            return this;
        }

        public Builder workspacePath(Path workspacePath) {
            this.workspacePath = workspacePath;
            return this;
        }

        public Builder projectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }

        public Builder providerId(String providerId) {
            this.providerId = providerId;
            return this;
        }

        public Builder memoryEnabled(boolean memoryEnabled) {
            this.memoryEnabled = memoryEnabled;
            return this;
        }

        public Builder harnessEnabled(boolean harnessEnabled) {
            this.harnessEnabled = harnessEnabled;
            return this;
        }

        public Builder maxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
            return this;
        }

        public Builder metadata(String key, Object value) {
            String normalizedKey = SdkText.trimToEmpty(key);
            if (!normalizedKey.isEmpty() && value != null) {
                metadata.put(normalizedKey, value);
            }
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            if (metadata != null) {
                metadata.forEach(this::metadata);
            }
            return this;
        }

        public WayangCodeAgentContext build() {
            return new WayangCodeAgentContext(
                    surfaceId,
                    profileId,
                    workspacePath,
                    projectId,
                    sessionId,
                    modelId,
                    providerId,
                    memoryEnabled,
                    harnessEnabled,
                    maxSteps,
                    metadata);
        }
    }
}
