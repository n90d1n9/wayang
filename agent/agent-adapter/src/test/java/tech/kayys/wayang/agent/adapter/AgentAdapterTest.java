package tech.kayys.wayang.agent.adapter;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.audit.AgentArtifact;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillDescriptor;
import tech.kayys.wayang.agent.spi.skills.SkillHealth;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AgentAdapterTest {

    @Test
    void auditAdapterStoresArtifactsByTenantAndRun() {
        AgentAuditServiceAdapter adapter = new AgentAuditServiceAdapter();
        AgentArtifact first = AgentArtifact.create("run-1", "tenant-a", "plan", "Plan");
        AgentArtifact second = AgentArtifact.create("run-1", "tenant-a", "task", "Task");
        AgentArtifact otherTenant = AgentArtifact.create("run-1", "tenant-b", "plan", "Other");

        adapter.saveArtifact(first).await().indefinitely();
        adapter.saveArtifact(second).await().indefinitely();
        adapter.saveArtifact(otherTenant).await().indefinitely();

        assertThat(adapter.getArtifact(first.id(), "tenant-a").await().indefinitely()).isEqualTo(first);
        assertThat(adapter.getArtifactsByRun("run-1", "tenant-a").await().indefinitely())
                .containsExactly(first, second);
        assertThat(adapter.size()).isEqualTo(3);
    }

    @Test
    void memoryAdapterStoresAndFiltersContext() {
        AgentMemoryManagerAdapter adapter = new AgentMemoryManagerAdapter();

        String firstId = adapter.storeMemory("agent-1", "alpha planning note", Map.of("phase", "plan"))
                .await().indefinitely();
        adapter.storeMemory("agent-1", "beta implementation detail", Map.of()).await().indefinitely();
        adapter.storeObservation("agent-1", "shell", "alpha command output").await().indefinitely();

        String context = adapter.retrieveContext("agent-1", "alpha", 5).await().indefinitely();

        assertThat(firstId).isNotBlank();
        assertThat(context)
                .contains("alpha planning note")
                .contains("alpha command output")
                .doesNotContain("beta implementation detail");
        assertThat(adapter.memoriesFor("agent-1")).hasSize(3);
    }

    @Test
    void skillRegistryAdapterRegistersRuntimeSkillAndDerivedDefinition() {
        SimpleSkillRegistry delegate = new SimpleSkillRegistry();
        SkillRegistryAdapter adapter = SkillRegistryAdapter.wrap(delegate);

        adapter.register(new EchoSkill());

        assertThat(adapter.find("echo").map(AgentSkill::name)).contains("Echo");
        assertThat(adapter.getSkill("echo").map(SkillDefinition::category)).contains("REASONING");
        assertThat(adapter.getSkill("echo").orElseThrow().metadata())
                .containsEntry("runtimeSkill", true)
                .containsEntry("priority", 42);

        SkillResult result = adapter.executeSkill("echo", Map.of("text", "hello")).await().indefinitely();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.observation()).isEqualTo("hello");
        assertThat(result.getOutput("echo", String.class)).isEqualTo("hello");
    }

    @SkillDescriptor(
            id = "echo",
            name = "Echo",
            description = "Echoes input text.",
            category = SkillCategory.REASONING,
            priority = 42,
            inputs = @SkillDescriptor.Input(name = "text"),
            outputs = @SkillDescriptor.Output(name = "echo"),
            aliases = { "repeat" },
            triggers = { "echo" })
    private static final class EchoSkill implements AgentSkill {
        @Override
        public String id() {
            return "echo";
        }

        @Override
        public String name() {
            return "Echo";
        }

        @Override
        public String description() {
            return "Echoes input text.";
        }

        @Override
        public Uni<Map<String, Object>> execute(Map<String, Object> context) {
            String text = String.valueOf(context.getOrDefault("text", ""));
            return Uni.createFrom().item(Map.of(
                    "success", true,
                    "status", "SUCCESS",
                    "observation", text,
                    "echo", text));
        }
    }

    private static final class SimpleSkillRegistry implements SkillRegistry {
        private final Map<String, AgentSkill> runtimeSkills = new LinkedHashMap<>();
        private final Map<String, SkillDefinition> definitions = new LinkedHashMap<>();

        @Override
        public List<AgentSkill> listAll() {
            return List.copyOf(runtimeSkills.values());
        }

        @Override
        public Optional<AgentSkill> find(String id) {
            return Optional.ofNullable(runtimeSkills.get(id));
        }

        @Override
        public AgentSkill findOrThrow(String id) {
            return find(id).orElseThrow();
        }

        @Override
        public void register(AgentSkill skill) {
            runtimeSkills.put(skill.id(), skill);
        }

        @Override
        public void unregister(String skillId) {
            runtimeSkills.remove(skillId);
            definitions.remove(skillId);
        }

        @Override
        public List<AgentSkill> findByCategory(SkillCategory category) {
            return runtimeSkills.values().stream()
                    .filter(skill -> category.name().equals(skill.category()))
                    .toList();
        }

        @Override
        public List<AgentSkill> listAllowed(String tenantId, Set<String> allowedIds) {
            return runtimeSkills.values().stream()
                    .filter(skill -> allowedIds == null || allowedIds.isEmpty() || allowedIds.contains(skill.id()))
                    .toList();
        }

        @Override
        public boolean isRegistered(String skillId) {
            return runtimeSkills.containsKey(skillId) || definitions.containsKey(skillId);
        }

        @Override
        public Map<String, SkillHealth> checkHealth() {
            return Map.of();
        }

        @Override
        public int size() {
            return runtimeSkills.size() + definitions.size();
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
                    .filter(definition -> category.equals(definition.category()))
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
