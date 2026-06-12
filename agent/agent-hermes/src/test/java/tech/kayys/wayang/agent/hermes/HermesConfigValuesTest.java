package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesConfigValuesTest {

    @Test
    void flattensNestedMapsAndReadsTypedValues() {
        HermesConfigValues values = HermesConfigValues.from(Map.of(
                "wayang", Map.of(
                        "agent", Map.of(
                                "hermes", Map.of(
                                        "require-tool-calling", "off",
                                        "memory-entry-limit", "4",
                                        "max-sub-agents", "3",
                                        "default-toolsets", "skills;rag")))),
                HermesAgentModeConfigs.PROPERTY_PREFIX);

        assertThat(values.booleanValue("require-tool-calling")).contains(false);
        assertThat(values.intValue("memory-entry-limit")).contains(4);
        assertThat(values.intValue("maxSubAgents")).contains(3);
        assertThat(values.listValue("default-toolsets")).contains(List.of("skills", "rag"));
    }

    @Test
    void normalizesCamelCaseAndHyphenatedAliases() {
        HermesConfigValues values = HermesConfigValues.from(Map.of(
                "wayang.agent.hermes.skillLearningEnabled", "true"),
                HermesAgentModeConfigs.PROPERTY_PREFIX);

        assertThat(values.booleanValue("skill-learning-enabled")).contains(true);
    }

    @Test
    void rejectsInvalidTypedValues() {
        HermesConfigValues values = HermesConfigValues.from(Map.of(
                "wayang.agent.hermes.cron-enabled", "maybe",
                "wayang.agent.hermes.memory-entry-limit", "several"),
                HermesAgentModeConfigs.PROPERTY_PREFIX);

        assertThatThrownBy(() -> values.booleanValue("cron-enabled"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("boolean");
        assertThatThrownBy(() -> values.intValue("memory-entry-limit"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("integer");
    }
}
