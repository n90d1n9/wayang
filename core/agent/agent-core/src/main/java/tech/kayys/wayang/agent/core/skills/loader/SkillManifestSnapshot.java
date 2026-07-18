package tech.kayys.wayang.agent.core.skills.loader;

import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable snapshot of loaded skill manifests with stable content fingerprints.
 */
public record SkillManifestSnapshot(
        Map<String, SkillManifest> manifests,
        Map<String, String> fingerprints) {

    public SkillManifestSnapshot {
        manifests = immutableMap(manifests);
        fingerprints = immutableMap(fingerprints);
    }

    public static SkillManifestSnapshot empty() {
        return new SkillManifestSnapshot(Map.of(), Map.of());
    }

    public static SkillManifestSnapshot from(Map<String, SkillManifest> manifests) {
        Map<String, SkillManifest> copied = immutableMap(manifests);
        Map<String, String> fingerprints = new LinkedHashMap<>();
        copied.forEach((name, manifest) -> fingerprints.put(name, fingerprint(manifest)));
        return new SkillManifestSnapshot(copied, fingerprints);
    }

    public boolean contains(String skillName) {
        return manifests.containsKey(skillName);
    }

    public Optional<SkillManifest> get(String skillName) {
        return Optional.ofNullable(manifests.get(skillName));
    }

    public Set<String> names() {
        return manifests.keySet();
    }

    public Optional<String> fingerprint(String skillName) {
        return Optional.ofNullable(fingerprints.get(skillName));
    }

    private static String fingerprint(SkillManifest manifest) {
        if (manifest == null) {
            return "";
        }
        String content = String.join("\n",
                field("name", manifest.getName()),
                field("description", manifest.getDescription()),
                field("version", manifest.getVersion()),
                field("license", manifest.getLicense()),
                field("compatibility", manifest.getCompatibility()),
                field("allowedToolsString", manifest.getAllowedToolsString()),
                field("allowedTools", manifest.getAllowedTools()),
                field("metadata", manifest.getMetadata()),
                field("bodyContent", manifest.getBodyContent()),
                field("references", manifest.getReferences()));
        return sha256(content);
    }

    private static String field(String name, Object value) {
        String normalized = stableValue(value);
        return name + ":" + normalized.length() + ":" + normalized;
    }

    private static String stableValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Map<?, ?> map) {
            return map.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> String.valueOf(entry.getKey())))
                    .map(entry -> stableValue(entry.getKey()) + "=" + stableValue(entry.getValue()))
                    .collect(Collectors.joining(",", "map{", "}"));
        }
        if (value instanceof Set<?> set) {
            return set.stream()
                    .map(SkillManifestSnapshot::stableValue)
                    .sorted()
                    .collect(Collectors.joining(",", "set[", "]"));
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(SkillManifestSnapshot::stableValue)
                    .collect(Collectors.joining(",", "list[", "]"));
        }
        String scalar = String.valueOf(value);
        return value.getClass().getName() + ":" + scalar.length() + ":" + scalar;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            try (Formatter formatter = new Formatter()) {
                for (byte current : bytes) {
                    formatter.format("%02x", current);
                }
                return formatter.toString();
            }
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 digest is unavailable", error);
        }
    }

    private static <T> Map<String, T> immutableMap(Map<String, T> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
