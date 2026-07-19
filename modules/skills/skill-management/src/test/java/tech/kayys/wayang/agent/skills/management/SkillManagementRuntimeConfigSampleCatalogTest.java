package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementRuntimeConfigSampleCatalogTest {

    @Test
    void selectsCanonicalObjectStorageSampleWithDefaultS3Provider() {
        SkillManagementRuntimeConfigSampleSelection selection =
                SkillManagementRuntimeConfigSampleCatalog.selection(" rustfs ");

        assertThat(selection.descriptor().profile()).isEqualTo(SkillManagementServiceProfile.OBJECT_STORAGE);
        assertThat(selection.descriptor().label()).isEqualTo("object-storage");
        assertThat(selection.objectStorageProvider())
                .isEqualTo(SkillManagementRuntimeConfigSampleProvider.S3_RUSTFS);
        assertThat(selection.description()).contains("S3/RustFS-compatible");
    }

    @Test
    void selectsProviderSamplesThroughNormalizedAliases() {
        SkillManagementRuntimeConfigSampleSelection gcs =
                SkillManagementRuntimeConfigSampleCatalog.selection("HYBRID_GCS");
        SkillManagementRuntimeConfigSampleSelection azure =
                SkillManagementRuntimeConfigSampleCatalog.selection("azure.blob.storage");

        assertThat(gcs.descriptor().profile()).isEqualTo(SkillManagementServiceProfile.HYBRID_OBJECT_FILE);
        assertThat(gcs.objectStorageProvider()).isEqualTo(SkillManagementRuntimeConfigSampleProvider.GCS);
        assertThat(gcs.description()).contains("Google Cloud Storage");
        assertThat(azure.descriptor().profile()).isEqualTo(SkillManagementServiceProfile.OBJECT_STORAGE);
        assertThat(azure.objectStorageProvider()).isEqualTo(SkillManagementRuntimeConfigSampleProvider.AZURE);
        assertThat(azure.description()).contains("Azure Blob Storage");
    }

    @Test
    void exposesCanonicalAndProviderSpecificDescriptors() {
        assertThat(SkillManagementRuntimeConfigSampleCatalog.samples())
                .filteredOn(sample -> sample.name().equals("mirrored-azure"))
                .singleElement()
                .satisfies(sample -> {
                    assertThat(sample.profile()).isEqualTo("mirrored-object-file");
                    assertThat(sample.objectStorageProvider()).isEqualTo("azure");
                    assertThat(sample.aliases()).containsExactly(
                            "azure-mirrored",
                            "mirror-azure",
                            "azure-mirror");
                    assertThat(sample.description()).contains("Mirrored file/object");
                });
    }

    @Test
    void defaultsProviderOnlyForCloudBackedProfiles() {
        assertThat(SkillManagementRuntimeConfigSampleProvider.defaultForProfile(null))
                .isEqualTo(SkillManagementRuntimeConfigSampleProvider.NONE);
        assertThat(SkillManagementRuntimeConfigSampleProvider.defaultForProfile(
                SkillManagementServiceProfile.LOCAL_FILESYSTEM))
                .isEqualTo(SkillManagementRuntimeConfigSampleProvider.NONE);
        assertThat(SkillManagementRuntimeConfigSampleProvider.defaultForProfile(
                SkillManagementServiceProfile.MIRRORED_OBJECT_FILE))
                .isEqualTo(SkillManagementRuntimeConfigSampleProvider.S3_RUSTFS);
    }
}
