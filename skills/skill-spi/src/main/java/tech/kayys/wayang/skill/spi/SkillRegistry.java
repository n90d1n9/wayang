package tech.kayys.wayang.skill.spi;

import java.util.List;
import java.util.Optional;

/**
 * Registry for managing {@link SkillDefinition} instances at runtime.
 *
 * <p>
 * The registry holds both built-in skills (loaded from classpath resources on
 * startup)
 * and custom skills (registered dynamically via the UI or API).
 *
 * <p>
 * Implementations must be thread-safe since skills can be
 * registered/unregistered
 * concurrently during workflow execution.
 */
public interface SkillRegistry {

    List<Skill> listAll();

    Optional<Skill> find(String id);

    Skill findOrThrow(String id);

    void register(Skill skill);

    void unregister(String skillId);

    List<Skill> findByCategory(SkillCategory category);

    List<Skill> listAllowed(String tenantId, java.util.Set<String> allowedIds);

    boolean isRegistered(String skillId);

    java.util.Map<String, SkillHealth> checkHealth();

    int size();

    /**
     * Look up a skill by its unique ID.
     *
     * @param skillId the skill identifier
     * @return the skill definition, or empty if not found
     */
    Optional<SkillDefinition> getSkill(String skillId);

    /**
     * List all registered skills.
     *
     * @return unmodifiable list of all skills
     */
    List<SkillDefinition> listSkills();

    /**
     * List skills filtered by category.
     *
     * @param category the category to filter by ("built-in", "template", "custom")
     * @return unmodifiable list of matching skills
     */
    List<SkillDefinition> listByCategory(String category);

    /**
     * Register a new skill or update an existing one.
     *
     * @param skill the skill definition to register
     */
    void registerSkill(SkillDefinition skill);

    /**
     * Remove a skill from the registry.
     *
     * @param skillId the skill identifier to remove
     * @return true if the skill was found and removed, false otherwise
     */
    boolean unregisterSkill(String skillId);

    /**
     * Check whether a skill with the given ID exists.
     *
     * @param skillId the skill identifier
     * @return true if registered
     */
    default boolean hasSkill(String skillId) {
        return getSkill(skillId).isPresent();
    }
}
