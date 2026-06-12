package tech.kayys.wayang.agent.skills.management;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Immutable skill artifact payload addressed by a {@link SkillArtifactReference}.
 */
public record SkillArtifact(
        SkillArtifactReference reference,
        byte[] content,
        String contentType,
        Map<String, String> metadata) {

    public static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";
    public static final String SHA256_ALGORITHM = "SHA-256";

    public SkillArtifact {
        reference = Objects.requireNonNull(reference, "reference");
        content = content == null ? new byte[0] : content.clone();
        contentType = contentType == null || contentType.isBlank()
                ? DEFAULT_CONTENT_TYPE
                : contentType.trim();
        metadata = sanitize(metadata);
    }

    public static SkillArtifact of(SkillArtifactReference reference, byte[] content) {
        return new SkillArtifact(reference, content, DEFAULT_CONTENT_TYPE, Map.of());
    }

    public static SkillArtifact text(SkillArtifactReference reference, String content) {
        byte[] bytes = content == null
                ? new byte[0]
                : content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return new SkillArtifact(reference, bytes, "text/plain; charset=utf-8", Map.of());
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public int sizeBytes() {
        return content.length;
    }

    public String sha256() {
        return sha256(content);
    }

    public SkillArtifact withMetadata(Map<String, String> metadata) {
        return new SkillArtifact(reference, content, contentType, metadata);
    }

    static String sha256(byte[] content) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance(SHA256_ALGORITHM);
            return java.util.HexFormat.of().formatHex(digest.digest(content == null ? new byte[0] : content));
        } catch (java.security.NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 digest algorithm is unavailable", error);
        }
    }

    private static Map<String, String> sanitize(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        TreeMap<String, String> copy = new TreeMap<>();
        values.forEach((key, value) -> {
            if (key != null && value != null) {
                copy.put(key, value);
            }
        });
        return copy.isEmpty() ? Map.of() : Collections.unmodifiableMap(copy);
    }
}
