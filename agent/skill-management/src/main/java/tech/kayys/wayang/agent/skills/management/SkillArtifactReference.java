package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Storage-neutral identity for skill artifacts such as packages, resources,
 * RAG indexes, and MCP descriptors.
 */
public record SkillArtifactReference(
        String skillId,
        SkillArtifactKind kind,
        String name,
        String version) {

    public static final String DEFAULT_VERSION = "current";
    public static final String DEFAULT_STORAGE_PREFIX = "skill-management/artifacts";

    public SkillArtifactReference {
        if (kind == null) {
            throw new IllegalArgumentException("Skill artifact kind is required");
        }
        skillId = normalizeSegment(skillId, "skill artifact reference skill id");
        name = normalizeSegment(defaultIfBlank(name, kind.label()), "skill artifact reference name");
        version = normalizeSegment(defaultIfBlank(version, DEFAULT_VERSION), "skill artifact reference version");
    }

    public static SkillArtifactReference definition(String skillId) {
        return new SkillArtifactReference(skillId, SkillArtifactKind.DEFINITION, null, null);
    }

    public static SkillArtifactReference lifecycleState(String skillId) {
        return new SkillArtifactReference(skillId, SkillArtifactKind.LIFECYCLE_STATE, null, null);
    }

    public static SkillArtifactReference eventHistory(String skillId) {
        return new SkillArtifactReference(skillId, SkillArtifactKind.EVENT_HISTORY, null, null);
    }

    public static SkillArtifactReference packageArtifact(String skillId, String version) {
        return new SkillArtifactReference(skillId, SkillArtifactKind.PACKAGE, null, version);
    }

    public static SkillArtifactReference resource(String skillId, String name, String version) {
        return new SkillArtifactReference(skillId, SkillArtifactKind.RESOURCE, name, version);
    }

    public static SkillArtifactReference ragIndex(String skillId, String name, String version) {
        return new SkillArtifactReference(skillId, SkillArtifactKind.RAG_INDEX, name, version);
    }

    public static SkillArtifactReference mcpDescriptor(String skillId, String name, String version) {
        return new SkillArtifactReference(skillId, SkillArtifactKind.MCP_DESCRIPTOR, name, version);
    }

    public List<String> pathSegments() {
        return List.of(skillId, kind.label(), name, version);
    }

    public String relativePath() {
        return String.join("/", pathSegments());
    }

    public String storageKey() {
        return storageKey(null);
    }

    public String storageKey(String prefix) {
        return storageKey(prefix, DEFAULT_STORAGE_PREFIX);
    }

    public String storageKey(String prefix, String defaultPrefix) {
        return SkillManagementObjectKeys.normalizePrefix(prefix, defaultPrefix) + relativePath();
    }

    public String qualifiedName() {
        return String.join(":", pathSegments());
    }

    private static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String normalizeSegment(String value, String context) {
        return SkillManagementSkillIds.normalizeForStorage(value, context);
    }
}
