package tech.kayys.wayang.agent.api;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentSkillCatalogServiceTest {

    private final AgentSkillCatalogService catalog = new AgentSkillCatalogService();

    @Test
    void listsRuntimeAndDefinitionSkillsInStableOrder() {
        AgentApiTestSkillRegistry registry = new AgentApiTestSkillRegistry();
        registry.register(new RuntimeSkill("runtime-z", "Runtime Z", SkillCategory.EXECUTION.name(), false));
        registry.registerSkill(definition("definition-a", "Definition A", "REASONING", Map.of("version", "2.0.0", "priority", "7")));

        List<AgentResource.SkillSummary> summaries = catalog.listSkills(registry, null);

        assertThat(summaries).extracting(AgentResource.SkillSummary::id)
                .containsExactly("definition-a", "runtime-z");
        assertThat(summaries.get(0))
                .returns("2.0.0", AgentResource.SkillSummary::version)
                .returns(7, AgentResource.SkillSummary::priority)
                .returns(false, AgentResource.SkillSummary::runtime);
        assertThat(summaries.get(1))
                .returns(false, AgentResource.SkillSummary::healthy)
                .returns(true, AgentResource.SkillSummary::runtime);
    }

    @Test
    void filtersByCategoryCaseInsensitively() {
        AgentApiTestSkillRegistry registry = new AgentApiTestSkillRegistry();
        registry.register(new RuntimeSkill("runtime-execution", "Runtime Execution", "EXECUTION", true));
        registry.registerSkill(definition("definition-reasoning", "Definition Reasoning", "REASONING", Map.of()));

        assertThat(catalog.listSkills(registry, new AgentSkillsRequest("reasoning")))
                .extracting(AgentResource.SkillSummary::id)
                .containsExactly("definition-reasoning");
    }

    @Test
    void getsRuntimeSkillBeforeDefinitionSkill() {
        AgentApiTestSkillRegistry registry = new AgentApiTestSkillRegistry();
        registry.register(new RuntimeSkill("shared", "Runtime Shared", "EXECUTION", true));
        registry.registerSkill(definition("shared", "Definition Shared", "EXECUTION", Map.of()));

        assertThat(catalog.getSkill(registry, "shared")).hasValueSatisfying(summary -> assertThat(summary)
                .returns("Runtime Shared", AgentResource.SkillSummary::name)
                .returns(true, AgentResource.SkillSummary::runtime));
        assertThat(catalog.getSkill(registry, "missing")).isEmpty();
    }

    @Test
    void reportsHealthAcrossRuntimeAndDefinitionSkillsWithoutDoubleCountingIds() {
        AgentApiTestSkillRegistry registry = new AgentApiTestSkillRegistry();
        registry.register(new RuntimeSkill("shared", "Runtime Shared", "EXECUTION", false));
        registry.registerSkill(definition("shared", "Definition Shared", "EXECUTION", Map.of()));
        registry.registerSkill(definition("definition-only", "Definition Only", "REASONING", Map.of()));

        assertThat(catalog.health(registry))
                .isEqualTo(new AgentResource.AgentHealthResponse("UP", 2, 1, 1));
    }

    private SkillDefinition definition(String id, String name, String category, Map<String, Object> metadata) {
        return SkillDefinition.builder()
                .id(id)
                .name(name)
                .description(name + " description")
                .category(category)
                .systemPrompt("Use " + name)
                .metadata(metadata)
                .build();
    }

    private record RuntimeSkill(String id, String name, String category, boolean healthy) implements AgentSkill {
        @Override
        public String description() {
            return name + " description";
        }

        @Override
        public boolean isHealthy() {
            return healthy;
        }

        @Override
        public Uni<Map<String, Object>> execute(Map<String, Object> context) {
            return Uni.createFrom().item(Map.of("success", true));
        }
    }
}
