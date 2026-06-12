package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Reads skill definitions with lifecycle-aware catalog filtering.
 */
final class SkillManagementCatalogReader {

    private final SkillDefinitionStore definitionStore;
    private final SkillManagementLifecycleRunner lifecycleRunner;

    SkillManagementCatalogReader(
            SkillDefinitionStore definitionStore,
            SkillManagementLifecycleRunner lifecycleRunner) {
        this.definitionStore = Objects.requireNonNull(definitionStore, "definitionStore");
        this.lifecycleRunner = Objects.requireNonNull(lifecycleRunner, "lifecycleRunner");
    }

    Optional<SkillDefinition> get(String skillId) {
        return definitionStore.getSkill(skillId);
    }

    List<SkillDefinition> list() {
        return sorted(definitionStore.listSkills());
    }

    List<SkillDefinition> listActive() {
        return sorted(definitionStore.listSkills().stream()
                .filter(skill -> lifecycleRunner.viewStateFor(skill.id()).status() == SkillLifecycleStatus.ACTIVE)
                .toList());
    }

    List<SkillDefinition> listByCategory(String category) {
        return sorted(definitionStore.listByCategory(category));
    }

    List<SkillDefinition> search(String query, String category, boolean includeDisabled) {
        String normalizedQuery = normalize(query);
        String normalizedCategory = normalize(category);
        return sorted(definitionStore.listSkills().stream()
                .filter(skill -> includeDisabled
                        || lifecycleRunner.viewStateFor(skill.id()).status() == SkillLifecycleStatus.ACTIVE)
                .filter(skill -> normalizedCategory.isBlank()
                        || normalize(skill.category()).equals(normalizedCategory))
                .filter(skill -> normalizedQuery.isBlank() || matches(skill, normalizedQuery))
                .toList());
    }

    private List<SkillDefinition> sorted(List<SkillDefinition> skills) {
        return skills.stream()
                .sorted(Comparator.comparing(SkillDefinition::id))
                .toList();
    }

    private boolean matches(SkillDefinition skill, String normalizedQuery) {
        return normalize(skill.id()).contains(normalizedQuery)
                || normalize(skill.name()).contains(normalizedQuery)
                || normalize(skill.description()).contains(normalizedQuery)
                || skill.tools().stream().map(this::normalize).anyMatch(tool -> tool.contains(normalizedQuery));
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
