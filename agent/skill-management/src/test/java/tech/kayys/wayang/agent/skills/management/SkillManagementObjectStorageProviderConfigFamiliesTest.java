package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementObjectStorageProviderConfigFamiliesTest {

    @Test
    void exposesStableProviderFamilyOrder() {
        assertThat(SkillManagementObjectStorageProviderConfigFamilies.all())
                .extracting(SkillManagementObjectStorageProviderConfigFamily::id)
                .containsExactly("s3-rustfs", "gcs", "azure");
        assertThat(SkillManagementObjectStorageProviderConfigFamilies.summaryLabel())
                .isEqualTo("S3/RustFS, GCS, or Azure");
    }

    @Test
    void keepsProviderRequiredAndOptionalKeysSeparated() {
        assertThat(family("s3-rustfs"))
                .satisfies(family -> {
                    assertThat(family.requiredKeys()).containsExactly(
                            "wayang.storage.s3.access-key-id",
                            "wayang.storage.s3.secret-access-key",
                            "wayang.storage.s3.bucket",
                            "wayang.storage.s3.region");
                    assertThat(family.optionalKeys()).containsExactly(
                            "wayang.storage.s3.endpoint",
                            "wayang.storage.s3.path-style-access",
                            "wayang.storage.s3.path-prefix");
                });
        assertThat(family("azure"))
                .satisfies(family -> {
                    assertThat(family.label()).isEqualTo("Azure Blob Storage");
                    assertThat(family.summaryLabel()).isEqualTo("Azure");
                    assertThat(family.requiredKeys()).containsExactly(
                            "wayang.storage.azure.connection-string",
                            "wayang.storage.azure.container");
                });
    }

    @Test
    void detectsConfiguredFamilyFromNormalizedEnvironmentKeys() {
        SkillManagementObjectStorageProviderConfigFamily family = family("gcs");
        Map<String, String> normalized = SkillStoreConfigValues.flattenAndNormalize(Map.of(
                "WAYANG_STORAGE_GCS_BUCKET", "wayang-skills"));

        assertThat(family.configured(normalized)).isTrue();
        assertThat(family.missingRequiredKeys(normalized)).isEmpty();
        assertThat(family.warnings(normalized)).isEmpty();
    }

    @Test
    void treatsOptionalOnlySettingsAsIncompleteProviderFamily() {
        SkillManagementObjectStorageProviderConfigFamily family = family("s3-rustfs");
        Map<String, String> normalized = SkillStoreConfigValues.flattenAndNormalize(Map.of(
                "wayang.storage.s3.endpoint", "http://localhost:9000"));

        assertThat(family.configured(normalized)).isTrue();
        assertThat(family.missingRequiredKeys(normalized)).containsExactly(
                "wayang.storage.s3.access-key-id",
                "wayang.storage.s3.secret-access-key",
                "wayang.storage.s3.bucket",
                "wayang.storage.s3.region");
        assertThat(family.warnings(normalized)).containsExactly(
                "S3/RustFS object-storage provider settings are incomplete: missing "
                        + "wayang.storage.s3.access-key-id, "
                        + "wayang.storage.s3.secret-access-key, "
                        + "wayang.storage.s3.bucket, "
                        + "wayang.storage.s3.region.");
    }

    private static SkillManagementObjectStorageProviderConfigFamily family(String id) {
        return SkillManagementObjectStorageProviderConfigFamilies.all().stream()
                .filter(family -> family.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
