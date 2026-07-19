package tech.kayys.wayang.agent.skills.management;

/**
 * Filter for listing persisted skill artifacts.
 */
public record SkillArtifactQuery(
        String skillId,
        SkillArtifactKind kind,
        String name,
        String version,
        int limit) {

    public static final int DEFAULT_LIMIT = SkillManagementQueryLimits.DEFAULT_LIMIT;
    public static final int MAX_LIMIT = SkillManagementQueryLimits.MAX_LIMIT;

    public SkillArtifactQuery {
        skillId = normalizeFilter(skillId, "skill artifact query skill id");
        name = normalizeFilter(name, "skill artifact query name");
        version = normalizeFilter(version, "skill artifact query version");
        limit = normalizeLimit(limit);
    }

    public static SkillArtifactQuery all() {
        return new SkillArtifactQuery("", null, "", "", DEFAULT_LIMIT);
    }

    public static SkillArtifactQuery forSkill(String skillId) {
        return new SkillArtifactQuery(skillId, null, "", "", DEFAULT_LIMIT);
    }

    public static SkillArtifactQuery forSkill(String skillId, int limit) {
        return new SkillArtifactQuery(skillId, null, "", "", limit);
    }

    public static SkillArtifactQuery forKind(String skillId, SkillArtifactKind kind, int limit) {
        return new SkillArtifactQuery(skillId, kind, "", "", limit);
    }

    public boolean matches(SkillArtifactReference reference) {
        if (reference == null) {
            return false;
        }
        if (!skillId.isBlank() && !skillId.equals(reference.skillId())) {
            return false;
        }
        if (kind != null && kind != reference.kind()) {
            return false;
        }
        if (!name.isBlank() && !name.equals(reference.name())) {
            return false;
        }
        return version.isBlank() || version.equals(reference.version());
    }

    private static String normalizeFilter(String value, String context) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return SkillManagementSkillIds.normalizeForStorage(value, context);
    }

    private static int normalizeLimit(int value) {
        return SkillManagementQueryLimits.normalize(value);
    }
}
