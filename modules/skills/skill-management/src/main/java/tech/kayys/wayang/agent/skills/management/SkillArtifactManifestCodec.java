package tech.kayys.wayang.agent.skills.management;

import java.util.Map;
import java.util.Objects;
import java.util.Properties;

/**
 * Java-properties manifest codec for artifact metadata stored beside payloads.
 */
final class SkillArtifactManifestCodec {

    static final String FILE_NAME = "artifact.properties";

    private static final String SKILL_ID = "skillId";
    private static final String KIND = "kind";
    private static final String NAME = "name";
    private static final String VERSION = "version";
    private static final String CONTENT_TYPE = "contentType";
    private static final String SIZE_BYTES = "sizeBytes";
    private static final String SHA256 = "sha256";
    private static final String METADATA_PREFIX = "metadata.";

    byte[] toBytes(SkillArtifact artifact) {
        Objects.requireNonNull(artifact, "artifact");
        Properties properties = new Properties();
        SkillArtifactReference reference = artifact.reference();
        SkillManagementPropertiesCodecSupport.putProperty(properties, SKILL_ID, reference.skillId());
        SkillManagementPropertiesCodecSupport.putProperty(properties, KIND, reference.kind().label());
        SkillManagementPropertiesCodecSupport.putProperty(properties, NAME, reference.name());
        SkillManagementPropertiesCodecSupport.putProperty(properties, VERSION, reference.version());
        SkillManagementPropertiesCodecSupport.putProperty(properties, CONTENT_TYPE, artifact.contentType());
        SkillManagementPropertiesCodecSupport.putProperty(properties, SIZE_BYTES, artifact.sizeBytes());
        SkillManagementPropertiesCodecSupport.putProperty(properties, SHA256, artifact.sha256());
        SkillManagementPropertiesCodecSupport.putPrefixedStringProperties(
                properties,
                METADATA_PREFIX,
                artifact.metadata());
        return SkillManagementPropertiesCodecSupport.storeToBytes(
                properties,
                "Wayang skill artifact manifest",
                "Failed to serialize skill artifact manifest");
    }

    SkillArtifactManifest fromBytes(byte[] content, String sourceDescription) {
        String source = sourceDescription == null ? "skill artifact manifest" : sourceDescription;
        Properties properties = SkillManagementPropertiesCodecSupport.loadFromBytes(
                content,
                "Failed to parse " + source);
        SkillArtifactReference reference = new SkillArtifactReference(
                SkillManagementPropertiesCodecSupport.requiredProperty(properties, SKILL_ID, source),
                SkillArtifactKind.fromLabel(
                        SkillManagementPropertiesCodecSupport.requiredProperty(properties, KIND, source)),
                SkillManagementPropertiesCodecSupport.requiredProperty(properties, NAME, source),
                SkillManagementPropertiesCodecSupport.requiredProperty(properties, VERSION, source));
        String contentType = properties.getProperty(CONTENT_TYPE, SkillArtifact.DEFAULT_CONTENT_TYPE);
        int sizeBytes = SkillManagementPropertiesCodecSupport.integerOrDefault(
                properties.getProperty(SIZE_BYTES),
                -1);
        String sha256 = properties.getProperty(SHA256, "");
        Map<String, String> metadata =
                SkillManagementPropertiesCodecSupport.prefixedStringProperties(properties, METADATA_PREFIX);
        return new SkillArtifactManifest(reference, contentType, metadata, sizeBytes, sha256);
    }
}

record SkillArtifactManifest(
        SkillArtifactReference reference,
        String contentType,
        Map<String, String> metadata,
        int sizeBytes,
        String sha256) {

    SkillArtifactManifest {
        reference = Objects.requireNonNull(reference, "reference");
        contentType = contentType == null || contentType.isBlank()
                ? SkillArtifact.DEFAULT_CONTENT_TYPE
                : contentType.trim();
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        sizeBytes = sizeBytes < 0 ? -1 : sizeBytes;
        sha256 = sha256 == null ? "" : sha256.trim().toLowerCase(java.util.Locale.ROOT);
    }

    SkillArtifact toArtifact(byte[] content) {
        byte[] resolvedContent = content == null ? new byte[0] : content;
        validateSize(resolvedContent);
        validateDigest(resolvedContent);
        return new SkillArtifact(reference, resolvedContent, contentType, metadata);
    }

    private void validateSize(byte[] content) {
        if (sizeBytes >= 0 && content.length != sizeBytes) {
            throw new IllegalStateException("Skill artifact content size mismatch for "
                    + reference.qualifiedName() + ": expected " + sizeBytes + " bytes but found "
                    + content.length);
        }
    }

    private void validateDigest(byte[] content) {
        if (sha256.isBlank()) {
            return;
        }
        String actual = SkillArtifact.sha256(content);
        if (!sha256.equals(actual)) {
            throw new IllegalStateException("Skill artifact SHA-256 mismatch for "
                    + reference.qualifiedName() + ": expected " + sha256 + " but found " + actual);
        }
    }
}
