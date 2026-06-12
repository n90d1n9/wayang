package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangRunSpecTest {

    @Test
    void mapsVersionedPropertiesToRunSpec() {
        Properties properties = new Properties();
        properties.setProperty("specVersion", "1");
        properties.setProperty("profileId", "assistant-agent");
        properties.setProperty("prompt", " answer from spec ");
        properties.setProperty("skills", "rag");
        properties.setProperty("requireReady", "true");

        WayangRunSpec spec = WayangRunSpec.fromProperties(properties);

        assertThat(spec.specVersion()).isEqualTo("1");
        assertThat(spec.profileId()).isEqualTo("assistant-agent");
        assertThat(spec.requireReady()).isTrue();
        assertThat(spec.request().prompt()).isEqualTo("answer from spec");
        assertThat(spec.request().surfaceId()).isEqualTo("assistant-agent");
        assertThat(spec.request().skills()).containsExactly("rag");
        assertThat(spec.request().context()).containsEntry("wayang.profile", "assistant-agent");
    }

    @Test
    void keepsLegacyPropertiesCompatible() {
        Properties properties = new Properties();
        properties.setProperty("prompt", "legacy");

        WayangRunSpec spec = WayangRunSpec.fromProperties(properties);

        assertThat(spec.specVersion()).isEqualTo(WayangRunSpec.CURRENT_VERSION);
        assertThat(spec.requireReady()).isFalse();
        assertThat(spec.request().prompt()).isEqualTo("legacy");
    }

    @Test
    void formatsRunSpecWithVersionAndLaunchPolicy() throws Exception {
        WayangRunSpec spec = WayangRunSpec.of("assistant-agent", AgentRunRequest.builder()
                .prompt("answer")
                .surfaceId("assistant-agent")
                .skill("rag")
                .build(), true);

        String propertiesText = WayangRunSpec.formatProperties(spec);
        Properties parsed = new Properties();

        assertThat(propertiesText)
                .startsWith("specVersion=1" + System.lineSeparator())
                .contains("profileId=assistant-agent" + System.lineSeparator())
                .contains("prompt=answer" + System.lineSeparator())
                .contains("surfaceId=assistant-agent" + System.lineSeparator())
                .contains("skills=rag" + System.lineSeparator())
                .contains("requireReady=true" + System.lineSeparator());
        assertThatCode(() -> parsed.load(new StringReader(propertiesText))).doesNotThrowAnyException();
        assertThat(WayangRunSpec.fromProperties(parsed).requireReady()).isTrue();
    }

    @Test
    void profileDefaultsApplyUnlessPropertiesOverrideThem() {
        Properties properties = new Properties();
        properties.setProperty("profileId", "low-code-agent");
        properties.setProperty("prompt", "automate this approval");
        properties.setProperty("workflowId", "custom-flow");

        WayangRunSpec spec = WayangRunSpec.fromProperties(properties);

        assertThat(spec.profileId()).isEqualTo("low-code-agent");
        assertThat(spec.requireReady()).isTrue();
        assertThat(spec.request().surfaceId()).isEqualTo("workflow-platform");
        assertThat(spec.request().workflowId()).isEqualTo("custom-flow");
        assertThat(spec.request().skills()).containsExactly("workflow", "hitl", "observability");
        assertThat(spec.request().context()).containsEntry("wayang.profile", "low-code-agent");
    }

    @Test
    void profileOverrideBeatsProfileIdInProperties() {
        Properties properties = new Properties();
        properties.setProperty("profileId", "assistant-agent");
        properties.setProperty("prompt", "wire workflow");

        WayangRunSpec spec = WayangRunSpec.fromProperties(properties, "low-code-agent");

        assertThat(spec.profileId()).isEqualTo("low-code-agent");
        assertThat(spec.request().surfaceId()).isEqualTo("workflow-platform");
        assertThat(spec.request().workflowId()).isEqualTo("gamelan-low-code-workflow");
        assertThat(spec.request().prompt()).isEqualTo("wire workflow");
    }

    @Test
    void rejectsUnsupportedVersionAndInvalidRequireReady() {
        Properties unsupported = new Properties();
        unsupported.setProperty("specVersion", "2");

        assertThatThrownBy(() -> WayangRunSpec.fromProperties(unsupported))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported Wayang run spec version '2'");

        Properties invalidBoolean = new Properties();
        invalidBoolean.setProperty("requireReady", "maybe");
        assertThatThrownBy(() -> WayangRunSpec.fromProperties(invalidBoolean))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requireReady");
    }
}
