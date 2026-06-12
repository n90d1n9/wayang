package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesPersistenceHintParserTest {

    @Test
    void parsesInlineAndDottedPersistenceHints() {
        HermesConfigValues values = HermesConfigValues.from(Map.of(
                "wayang.agent.hermes.persistence-hints", "definitions=database;artifacts:s3",
                "wayang.agent.hermes.persistence-hints.fallback", "file-system"),
                HermesAgentModeConfigs.PROPERTY_PREFIX);

        Map<String, String> hints = HermesPersistenceHintParser.parse(
                values,
                HermesAgentModeConfigs.PROPERTY_PREFIX);

        assertThat(hints)
                .containsEntry("definitions", "database")
                .containsEntry("artifacts", "s3")
                .containsEntry("fallback", "file-system");
    }

    @Test
    void rejectsMalformedInlinePersistenceHints() {
        HermesConfigValues values = HermesConfigValues.from(Map.of(
                "wayang.agent.hermes.persistence-hints", "definitions"),
                HermesAgentModeConfigs.PROPERTY_PREFIX);

        assertThatThrownBy(() -> HermesPersistenceHintParser.parse(
                values,
                HermesAgentModeConfigs.PROPERTY_PREFIX))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("persistence hint");
    }
}
