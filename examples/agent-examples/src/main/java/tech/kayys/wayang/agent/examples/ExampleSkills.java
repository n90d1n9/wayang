package tech.kayys.wayang.agent.examples;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Map;

public final class ExampleSkills {

    private ExampleSkills() {
    }

    public static SkillDefinition plannerDefinition() {
        return SkillDefinition.builder()
                .id("example-planner")
                .name("Example Planner")
                .description("Plans a task before handing it to an inference backend.")
                .category("REASONING")
                .systemPrompt("Break the request into clear execution steps.")
                .build();
    }

    public static AgentSkill echoSkill() {
        return new AgentSkill() {
            @Override
            public String id() {
                return "example-echo";
            }

            @Override
            public String name() {
                return "Example Echo";
            }

            @Override
            public String description() {
                return "Returns the input payload for harness smoke tests.";
            }

            @Override
            public String category() {
                return "UTILITY";
            }

            @Override
            public Uni<Map<String, Object>> execute(Map<String, Object> context) {
                return Uni.createFrom().item(Map.of(
                        "success", true,
                        "payload", context == null ? Map.of() : context));
            }
        };
    }
}
