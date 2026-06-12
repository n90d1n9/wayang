package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.skills.management.FileSystemSkillDefinitionStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillArtifactStore;
import tech.kayys.wayang.agent.skills.management.InMemorySkillLifecycleStateStore;
import tech.kayys.wayang.agent.skills.management.SkillDefinitionStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillLifecycleStateStoreInspector;
import tech.kayys.wayang.agent.skills.management.SkillManagementEventSink;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillHealth;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesSkillManagementServiceResolverTest {

    @Test
    void prefersExistingSkillManagementService(@TempDir Path tempDir) {
        SkillManagementService existingService = service(tempDir);

        SkillManagementService resolved = HermesSkillManagementServiceResolver.resolve(
                Optional.of(existingService),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());

        assertThat(resolved).isSameAs(existingService);
    }

    @Test
    void createsRegistryBackedServiceWhenNoServiceBeanExists() {
        SimpleSkillRegistry registry = new SimpleSkillRegistry();
        SkillDefinition skill = skill("hermes-audit");
        registry.registerSkill(skill);

        SkillManagementService resolved = HermesSkillManagementServiceResolver.resolve(
                Optional.empty(),
                Optional.of(registry),
                Optional.of(SkillManagementServiceConfig.defaults()),
                Optional.empty(),
                Optional.empty());

        assertThat(resolved.getSkill("hermes-audit").await().indefinitely())
                .contains(skill);
    }

    @Test
    void requiresServiceOrRegistry() {
        assertThatThrownBy(() -> HermesSkillManagementServiceResolver.resolve(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Hermes mode requires a SkillRegistry or SkillManagementService bean");
    }

    private static SkillManagementService service(Path tempDir) {
        return new SkillManagementService(
                new FileSystemSkillDefinitionStore(tempDir.resolve("definitions")),
                new SkillDefinitionStoreInspector(),
                new InMemorySkillLifecycleStateStore(),
                new SkillLifecycleStateStoreInspector(),
                new InMemorySkillArtifactStore(),
                SkillManagementEventSink.noop());
    }

    private static SkillDefinition skill(String id) {
        return SkillDefinition.builder()
                .id(id)
                .name("Hermes Audit")
                .description("Audit Hermes runtime readiness")
                .category(HermesAgentMode.LEARNED_SKILL_CATEGORY)
                .systemPrompt("Audit the runtime.")
                .userPromptTemplate("{{instruction}}")
                .tools(List.of("rag"))
                .build();
    }

    private static final class SimpleSkillRegistry implements SkillRegistry {
        private final Map<String, SkillDefinition> definitions = new LinkedHashMap<>();

        @Override
        public List<AgentSkill> listAll() {
            return List.of();
        }

        @Override
        public Optional<AgentSkill> find(String id) {
            return Optional.empty();
        }

        @Override
        public AgentSkill findOrThrow(String id) {
            return find(id).orElseThrow();
        }

        @Override
        public void register(AgentSkill skill) {
        }

        @Override
        public void unregister(String skillId) {
            definitions.remove(skillId);
        }

        @Override
        public List<AgentSkill> findByCategory(SkillCategory category) {
            return List.of();
        }

        @Override
        public List<AgentSkill> listAllowed(String tenantId, Set<String> allowedIds) {
            return List.of();
        }

        @Override
        public boolean isRegistered(String skillId) {
            return definitions.containsKey(skillId);
        }

        @Override
        public Map<String, SkillHealth> checkHealth() {
            return Map.of();
        }

        @Override
        public int size() {
            return definitions.size();
        }

        @Override
        public Optional<SkillDefinition> getSkill(String skillId) {
            return Optional.ofNullable(definitions.get(skillId));
        }

        @Override
        public List<SkillDefinition> listSkills() {
            return List.copyOf(definitions.values());
        }

        @Override
        public List<SkillDefinition> listByCategory(String category) {
            return definitions.values().stream()
                    .filter(skill -> category == null || category.equalsIgnoreCase(skill.category()))
                    .toList();
        }

        @Override
        public void registerSkill(SkillDefinition skill) {
            definitions.put(skill.id(), skill);
        }

        @Override
        public boolean unregisterSkill(String skillId) {
            return definitions.remove(skillId) != null;
        }
    }
}
