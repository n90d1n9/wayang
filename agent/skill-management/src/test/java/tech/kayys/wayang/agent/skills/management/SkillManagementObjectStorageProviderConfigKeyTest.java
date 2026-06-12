package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementObjectStorageProviderConfigKeyTest {

    @Test
    void createsPropertyAndEnvironmentSampleEntries() {
        SkillManagementObjectStorageProviderConfigKey key =
                new SkillManagementObjectStorageProviderConfigKey(
                        " wayang.storage.example.bucket ",
                        " WAYANG_STORAGE_EXAMPLE_BUCKET ",
                        " wayang ",
                        " Example bucket. ",
                        true);

        assertThat(key.property()).isEqualTo("wayang.storage.example.bucket");
        assertThat(key.environment()).isEqualTo("WAYANG_STORAGE_EXAMPLE_BUCKET");
        assertThat(key.defaultValue()).isEqualTo("wayang");
        assertThat(key.sampleDescription()).isEqualTo("Example bucket.");
        assertThat(key.required()).isTrue();
        assertThat(key.sampleEntry(false))
                .isEqualTo(new SkillManagementRuntimeConfigSampleEntry(
                        "wayang.storage.example.bucket",
                        "wayang",
                        "Example bucket."));
        assertThat(key.sampleEntry(true))
                .isEqualTo(new SkillManagementRuntimeConfigSampleEntry(
                        "WAYANG_STORAGE_EXAMPLE_BUCKET",
                        "wayang",
                        "Example bucket."));
    }

    @Test
    void rejectsMissingPropertyAndEnvironmentKeys() {
        assertThatThrownBy(() -> new SkillManagementObjectStorageProviderConfigKey(
                "",
                "WAYANG_STORAGE_EXAMPLE_BUCKET",
                "",
                "",
                false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("property is required");
        assertThatThrownBy(() -> new SkillManagementObjectStorageProviderConfigKey(
                "wayang.storage.example.bucket",
                "",
                "",
                "",
                false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("environment is required");
    }
}
