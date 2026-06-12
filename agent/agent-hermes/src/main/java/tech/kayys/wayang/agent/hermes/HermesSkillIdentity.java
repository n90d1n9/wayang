package tech.kayys.wayang.agent.hermes;

/**
 * Stable user-facing identity for one Hermes-learned skill.
 */
public record HermesSkillIdentity(
        String id,
        String name,
        String description) {

    public HermesSkillIdentity {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Hermes skill identity id is required");
        }
        name = name == null || name.isBlank() ? id : name.trim();
        description = description == null ? "" : description.trim();
    }
}
