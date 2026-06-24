package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Optional contribution from a coding-agent extension for one session.
 *
 * <p>Contributions are intentionally declarative. Extensions can add prompt
 * guidance, advertise slash commands, and attach metadata while the active
 * surface decides how to render or enforce those additions.</p>
 */
public record WayangCodeAgentContribution(
        String extensionId,
        List<String> systemPromptAdditions,
        List<String> slashCommandHints,
        Map<String, Object> metadata) {

    public WayangCodeAgentContribution {
        extensionId = SdkText.trimToDefault(extensionId, "unknown");
        systemPromptAdditions = normalizeTextList(systemPromptAdditions);
        slashCommandHints = normalizeTextList(slashCommandHints);
        metadata = SdkMaps.copy(metadata);
    }

    public static WayangCodeAgentContribution empty(String extensionId) {
        return new WayangCodeAgentContribution(extensionId, List.of(), List.of(), Map.of());
    }

    public static Builder builder(String extensionId) {
        return new Builder(extensionId);
    }

    public boolean empty() {
        return systemPromptAdditions.isEmpty() && slashCommandHints.isEmpty() && metadata.isEmpty();
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("extensionId", extensionId);
        values.put("systemPromptAdditions", systemPromptAdditions);
        values.put("slashCommandHints", slashCommandHints);
        values.put("metadata", metadata);
        return SdkMaps.orderedCopy(values);
    }

    private static List<String> normalizeTextList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        values.forEach(value -> {
            String text = SdkText.trimToEmpty(value);
            if (!text.isEmpty()) {
                normalized.add(text);
            }
        });
        return normalized.isEmpty() ? List.of() : List.copyOf(normalized);
    }

    /**
     * Builder for {@link WayangCodeAgentContribution}.
     */
    public static final class Builder {
        private final String extensionId;
        private final Set<String> systemPromptAdditions = new LinkedHashSet<>();
        private final Set<String> slashCommandHints = new LinkedHashSet<>();
        private final Map<String, Object> metadata = new LinkedHashMap<>();

        private Builder(String extensionId) {
            this.extensionId = extensionId;
        }

        public Builder systemPromptAddition(String text) {
            String normalized = SdkText.trimToEmpty(text);
            if (!normalized.isEmpty()) {
                systemPromptAdditions.add(normalized);
            }
            return this;
        }

        public Builder systemPromptAdditions(List<String> values) {
            if (values != null) {
                values.forEach(this::systemPromptAddition);
            }
            return this;
        }

        public Builder slashCommandHint(String text) {
            String normalized = SdkText.trimToEmpty(text);
            if (!normalized.isEmpty()) {
                slashCommandHints.add(normalized);
            }
            return this;
        }

        public Builder slashCommandHints(List<String> values) {
            if (values != null) {
                values.forEach(this::slashCommandHint);
            }
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

        public WayangCodeAgentContribution build() {
            return new WayangCodeAgentContribution(
                    extensionId,
                    List.copyOf(systemPromptAdditions),
                    List.copyOf(slashCommandHints),
                    metadata);
        }
    }
}
