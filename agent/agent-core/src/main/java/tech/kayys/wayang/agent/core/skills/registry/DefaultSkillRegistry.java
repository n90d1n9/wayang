package tech.kayys.wayang.agent.core.skills.registry;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Default implementation of skill registry.
 *
 * <p>
 * Thread-safe registry for skill registration, lookup, and filtering.
 * Supports skill discovery by name, category, tags, and custom predicates.
 */
public class DefaultSkillRegistry implements SkillRegistry {

    private final Map<String, SkillDefinition> skillsByName = new ConcurrentHashMap<>();
    private final Map<SkillCategory, List<SkillDefinition>> skillsByCategory = new ConcurrentHashMap<>();
    private final Map<String, Set<SkillDefinition>> skillsByTag = new ConcurrentHashMap<>();

    @Override
    public void register(SkillDefinition skill) {
        Objects.requireNonNull(skill, "Skill cannot be null");
        Objects.requireNonNull(skill.name(), "Skill name cannot be null");

        skillsByName.put(skill.name(), skill);

        // Index by category
        if (skill.category() != null) {
            skillsByCategory.computeIfAbsent(skill.category(), k -> new ArrayList<>()).add(skill);
        }

        // Index by tags
        if (skill.tags() != null) {
            for (String tag : skill.tags()) {
                skillsByTag.computeIfAbsent(tag, k -> ConcurrentHashMap.newKeySet()).add(skill);
            }
        }
    }

    @Override
    public void unregister(String skillName) {
        Objects.requireNonNull(skillName);

        SkillDefinition skill = skillsByName.remove(skillName);
        if (skill != null) {
            // Remove from category index
            if (skill.category() != null) {
                List<SkillDefinition> categorySkills = skillsByCategory.get(skill.category());
                if (categorySkills != null) {
                    categorySkills.remove(skill);
                    if (categorySkills.isEmpty()) {
                        skillsByCategory.remove(skill.category());
                    }
                }
            }

            // Remove from tag index
            if (skill.tags() != null) {
                for (String tag : skill.tags()) {
                    Set<SkillDefinition> tagSkills = skillsByTag.get(tag);
                    if (tagSkills != null) {
                        tagSkills.remove(skill);
                        if (tagSkills.isEmpty()) {
                            skillsByTag.remove(tag);
                        }
                    }
                }
            }
        }
    }

    @Override
    public Optional<SkillDefinition> get(String skillName) {
        Objects.requireNonNull(skillName);
        return Optional.ofNullable(skillsByName.get(skillName));
    }

    @Override
    public Collection<SkillDefinition> getAll() {
        return Collections.unmodifiableCollection(skillsByName.values());
    }

    @Override
    public List<SkillDefinition> getByCategory(SkillCategory category) {
        Objects.requireNonNull(category);
        List<SkillDefinition> skills = skillsByCategory.get(category);
        return skills != null ? Collections.unmodifiableList(skills) : Collections.emptyList();
    }

    @Override
    public List<SkillDefinition> getByTag(String tag) {
        Objects.requireNonNull(tag);
        Set<SkillDefinition> skills = skillsByTag.get(tag);
        return skills != null ? Collections.unmodifiableList(new ArrayList<>(skills)) : Collections.emptyList();
    }

    @Override
    public List<SkillDefinition> filter(java.util.function.Predicate<SkillDefinition> predicate) {
        Objects.requireNonNull(predicate);
        return skillsByName.values().stream()
                .filter(predicate)
                .toList();
    }

    @Override
    public Stream<SkillDefinition> stream() {
        return skillsByName.values().stream();
    }

    @Override
    public boolean exists(String skillName) {
        Objects.requireNonNull(skillName);
        return skillsByName.containsKey(skillName);
    }

    @Override
    public int size() {
        return skillsByName.size();
    }

    @Override
    public void clear() {
        skillsByName.clear();
        skillsByCategory.clear();
        skillsByTag.clear();
    }

    /**
     * Bulk register multiple skills.
     *
     * @param skills skills to register
     */
    public void registerAll(Collection<SkillDefinition> skills) {
        Objects.requireNonNull(skills);
        skills.forEach(this::register);
    }

    /**
     * Get registry statistics.
     *
     * @return map with counts by category and total
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", size());
        stats.put("byCategory", new HashMap<>(skillsByCategory.keySet().stream()
                .collect(HashMap::new, (m, k) -> m.put(k.toString(), skillsByCategory.get(k).size()), HashMap::putAll)));
        stats.put("tags", skillsByTag.size());
        return stats;
    }
}
