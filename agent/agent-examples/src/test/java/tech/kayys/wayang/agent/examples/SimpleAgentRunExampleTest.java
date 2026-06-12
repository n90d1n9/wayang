package tech.kayys.wayang.agent.examples;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleAgentRunExampleTest {

    @Test
    void runsAgainstDeterministicInferenceBackend() {
        var response = SimpleAgentRunExample.run("plan the release")
                .await().atMost(Duration.ofSeconds(2));

        assertThat(response.successful()).isTrue();
        assertThat(response.answer()).contains("plan the release");
        assertThat(response.strategy()).isEqualTo("react");
    }

    @Test
    void exposesDataDrivenAndRuntimeSkillExamples() {
        var definition = ExampleSkills.plannerDefinition();
        var result = ExampleSkills.echoSkill().execute(Map.of("input", "hello"))
                .await().atMost(Duration.ofSeconds(2));

        assertThat(definition.id()).isEqualTo("example-planner");
        assertThat(result)
                .containsEntry("success", true)
                .containsKey("payload");
    }
}
