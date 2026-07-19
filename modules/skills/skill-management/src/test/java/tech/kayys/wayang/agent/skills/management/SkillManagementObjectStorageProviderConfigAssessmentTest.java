package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementObjectStorageProviderConfigAssessmentTest {

    @Test
    void warnsWhenObjectStorageProviderSettingsAreMissing() {
        SkillManagementObjectStorageProviderConfigAssessment assessment =
                SkillManagementObjectStorageProviderConfigAssessment.fromMap(Map.of());

        assertThat(assessment.configuredProviders()).isEmpty();
        assertThat(assessment.warningCount()).isEqualTo(1);
        assertThat(assessment.warnings()).containsExactly(
                "Object-storage persistence is selected but no S3/RustFS, GCS, or Azure provider settings were detected.");
    }

    @Test
    void acceptsCompleteGcsProviderSettings() {
        SkillManagementObjectStorageProviderConfigAssessment assessment =
                SkillManagementObjectStorageProviderConfigAssessment.fromMap(Map.of(
                        "wayang.storage.gcs.bucket", "wayang-skills"));

        assertThat(assessment.configuredProviders()).containsExactly("gcs");
        assertThat(assessment.hasConfiguredProvider()).isTrue();
        assertThat(assessment.warnings()).isEmpty();
    }

    @Test
    void detectsEnvironmentStyleGcsProviderSettings() {
        SkillManagementObjectStorageProviderConfigAssessment assessment =
                SkillManagementObjectStorageProviderConfigAssessment.fromEnvironment(Map.of(
                        "WAYANG_STORAGE_GCS_BUCKET", "wayang-skills"));

        assertThat(assessment.configuredProviders()).containsExactly("gcs");
        assertThat(assessment.warnings()).isEmpty();
    }

    @Test
    void warnsWhenAzureProviderSettingsAreIncomplete() {
        SkillManagementObjectStorageProviderConfigAssessment assessment =
                SkillManagementObjectStorageProviderConfigAssessment.fromMap(Map.of(
                        "wayang.storage.azure.container", "wayang"));

        assertThat(assessment.configuredProviders()).containsExactly("azure");
        assertThat(assessment.warnings()).containsExactly(
                "Azure Blob Storage object-storage provider settings are incomplete: missing "
                        + "wayang.storage.azure.connection-string.");
    }

    @Test
    void warnsWhenMultipleProviderFamiliesAreConfigured() {
        SkillManagementObjectStorageProviderConfigAssessment assessment =
                SkillManagementObjectStorageProviderConfigAssessment.fromMap(Map.of(
                        "wayang.storage.s3.access-key-id", "ak",
                        "wayang.storage.s3.secret-access-key", "sk",
                        "wayang.storage.s3.bucket", "wayang",
                        "wayang.storage.s3.region", "us-east-1",
                        "wayang.storage.gcs.bucket", "wayang"));

        assertThat(assessment.configuredProviders()).containsExactly("s3-rustfs", "gcs");
        assertThat(assessment.warnings()).containsExactly(
                "Multiple object-storage provider setting families detected: s3-rustfs, gcs. "
                        + "Keep only the active provider family configured.");
    }
}
