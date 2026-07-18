package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementPropertiesCodecSupportTest {

    @Test
    void storesAndLoadsPropertiesThroughUtf8Bytes() {
        Properties properties = new Properties();
        properties.setProperty("skillId", "planner");
        properties.setProperty("prompt", "rancang jalur");

        byte[] content = SkillManagementPropertiesCodecSupport.storeToBytes(
                properties,
                "test",
                "encode failed");

        assertThat(SkillManagementPropertiesCodecSupport.loadFromBytes(content, "decode failed"))
                .containsEntry("skillId", "planner")
                .containsEntry("prompt", "rancang jalur");
    }

    @Test
    void convertsUtf8StringsAndBytes() {
        byte[] content = SkillManagementPropertiesCodecSupport.toUtf8Bytes("wayang");

        assertThat(SkillManagementPropertiesCodecSupport.fromUtf8Bytes(content)).isEqualTo("wayang");
    }

    @Test
    void rejectsMissingRequiredProperties() {
        Properties properties = new Properties();

        assertThatThrownBy(() -> SkillManagementPropertiesCodecSupport.requiredProperty(
                properties,
                "skillId",
                "test source"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing required property 'skillId' in test source");
    }

    @Test
    void writesBlankAwareProperties() {
        Properties properties = new Properties();
        Map<String, String> prompts = new LinkedHashMap<>();
        prompts.put("REVIEW", "Review carefully.");
        prompts.put(" ", "ignored");
        prompts.put("empty", null);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("enabled", true);
        metadata.put("version", 2);
        metadata.put("nested", Map.of("ignored", true));

        SkillManagementPropertiesCodecSupport.putProperty(properties, "name", "Planner");
        SkillManagementPropertiesCodecSupport.putProperty(properties, "missing", null);
        SkillManagementPropertiesCodecSupport.putLineTokens(properties, "tools", List.of("search", "rag"));
        SkillManagementPropertiesCodecSupport.putPrefixedStringProperties(properties, "prompt.", prompts);
        SkillManagementPropertiesCodecSupport.putPrefixedScalarProperties(properties, "metadata.", metadata);

        assertThat(properties)
                .containsEntry("name", "Planner")
                .containsEntry("tools", "search\nrag")
                .containsEntry("prompt.REVIEW", "Review carefully.")
                .containsEntry("metadata.enabled", "true")
                .containsEntry("metadata.version", "2")
                .doesNotContainKeys("missing", "prompt. ", "prompt.empty", "metadata.nested");
    }

    @Test
    void readsPrefixedStringPropertiesInSortedOrder() {
        Properties properties = new Properties();
        properties.setProperty("prompt.Z", "zed");
        properties.setProperty("prompt.A", "alpha");
        properties.setProperty("other.value", "ignored");

        Map<String, String> values =
                SkillManagementPropertiesCodecSupport.prefixedStringProperties(properties, "prompt.");

        assertThat(values.keySet()).containsExactly("A", "Z");
        assertThat(values)
                .containsEntry("A", "alpha")
                .containsEntry("Z", "zed")
                .doesNotContainKey("other.value");
    }

    @Test
    void parsesBlankAwareScalarProperties() {
        assertThat(SkillManagementPropertiesCodecSupport.isBlank(" ")).isTrue();
        assertThat(SkillManagementPropertiesCodecSupport.lineTokens(" search\nrag \n\n"))
                .containsExactly("search", "rag");
        assertThat(SkillManagementPropertiesCodecSupport.doubleOrNull("0.7")).isEqualTo(0.7D);
        assertThat(SkillManagementPropertiesCodecSupport.doubleOrNull(" ")).isNull();
        assertThat(SkillManagementPropertiesCodecSupport.integerOrNull("128")).isEqualTo(128);
        assertThat(SkillManagementPropertiesCodecSupport.integerOrDefault(" ", 1)).isEqualTo(1);
        assertThat(SkillManagementPropertiesCodecSupport.instantOrNull("2026-01-01T00:00:00Z"))
                .isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }
}
