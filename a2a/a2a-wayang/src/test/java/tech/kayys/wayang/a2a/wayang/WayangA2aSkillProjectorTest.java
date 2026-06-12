package tech.kayys.wayang.a2a.wayang;

import io.smallrye.mutiny.Uni;
import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aAgentSkill;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WayangA2aSkillProjectorTest {

    private final WayangA2aSkillProjector projector = new WayangA2aSkillProjector();

    @Test
    void projectsSkillDefinitionMetadataIntoA2aSkill() {
        SkillDefinition skill = SkillDefinition.builder()
                .id("rag-search")
                .name("RAG Search")
                .description("Answer with retrieved context")
                .category("retrieval")
                .systemPrompt("Use retrieved context.")
                .tools(List.of("mcp.search"))
                .metadata(Map.of(
                        SkillMetadataKeys.KEY_TAGS, List.of("rag", "knowledge"),
                        SkillMetadataKeys.KEY_DOMAINS, "docs support",
                        SkillMetadataKeys.KEY_OUTPUT_FORMAT, "json",
                        WayangA2a.METADATA_EXAMPLES, List.of("Find docs about S3 skill storage"),
                        WayangA2a.METADATA_INPUT_MODES, List.of("text/plain"),
                        WayangA2a.METADATA_SECURITY_REQUIREMENTS, List.of(Map.of("bearer", List.of()))))
                .build();

        A2aAgentSkill projected = projector.fromSkillDefinition(skill);

        assertThat(projected.id()).isEqualTo("rag-search");
        assertThat(projected.tags()).contains("rag", "knowledge", "docs", "support", "retrieval", "mcp.search");
        assertThat(projected.examples()).containsExactly("Find docs about S3 skill storage");
        assertThat(projected.inputModes()).containsExactly("text/plain");
        assertThat(projected.outputModes()).containsExactly("application/json");
        assertThat(projected.securityRequirements()).singleElement()
                .satisfies(requirement -> assertThat(requirement).containsKey("bearer"));
    }

    @Test
    void projectsLegacyAgentSkillWithSafeFallbackTags() {
        AgentSkill skill = new AgentSkill() {
            @Override
            public String id() {
                return "planner";
            }

            @Override
            public String name() {
                return "Planner";
            }

            @Override
            public List<String> aliases() {
                return List.of("plan", "tasks");
            }

            @Override
            public String description() {
                return "Plan agent work";
            }

            @Override
            public String category() {
                return "REASONING";
            }

            @Override
            public Uni<Map<String, Object>> execute(Map<String, Object> context) {
                return Uni.createFrom().item(Map.of());
            }
        };

        A2aAgentSkill projected = projector.fromAgentSkill(skill);

        assertThat(projected.tags()).containsExactly("plan", "tasks", "REASONING", "planner");
    }
}
