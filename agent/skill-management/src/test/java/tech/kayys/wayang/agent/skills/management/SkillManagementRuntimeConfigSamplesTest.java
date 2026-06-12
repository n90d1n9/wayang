package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillManagementRuntimeConfigSamplesTest {

    @Test
    void exposesDiscoverableSampleDescriptors() {
        assertThat(SkillManagementRuntimeConfigSamples.samples())
                .extracting(SkillManagementRuntimeConfigSampleDescriptor::name)
                .containsExactly(
                        "default",
                        "local-filesystem",
                        "object-storage",
                        "jdbc",
                        "hybrid-object-file",
                        "mirrored-object-file",
                        "gcs",
                        "azure",
                        "hybrid-gcs",
                        "hybrid-azure",
                        "mirrored-gcs",
                        "mirrored-azure");

        assertThat(SkillManagementRuntimeConfigSamples.samples())
                .filteredOn(sample -> sample.name().equals("gcs"))
                .singleElement()
                .satisfies(sample -> {
                    assertThat(sample.profile()).isEqualTo("object-storage");
                    assertThat(sample.objectStorageProvider()).isEqualTo("gcs");
                    assertThat(sample.aliases()).containsExactly(
                            "google-cloud-storage",
                            "gcs-object-storage");
                });
    }

    @Test
    void rendersObjectStorageSampleFromAlias() {
        SkillManagementRuntimeConfigSample sample =
                SkillManagementRuntimeConfigSamples.forProfile("rustfs");

        assertThat(sample.profile()).isEqualTo("object-storage");
        assertThat(sample.description()).contains("S3/RustFS-compatible");
        assertThat(lines(sample.properties()))
                .containsExactly(
                        "wayang.skills.profile=object-storage",
                        "wayang.skills.profile.object-prefix=wayang/skills",
                        "wayang.storage.s3.endpoint=http://localhost:9000",
                        "wayang.storage.s3.bucket=wayang",
                        "wayang.storage.s3.region=us-east-1",
                        "wayang.storage.s3.access-key-id=CHANGE_ME",
                        "wayang.storage.s3.secret-access-key=CHANGE_ME",
                        "wayang.storage.s3.path-style-access=true",
                        "wayang.storage.s3.path-prefix=tenants/acme",
                        "wayang.skills.profile.max-events=10000",
                        "wayang.skills.lifecycle.reconcile.mode=inspect-only");
        assertThat(lines(sample.environment()))
                .containsExactly(
                        "WAYANG_SKILLS_PROFILE=object-storage",
                        "WAYANG_SKILLS_PROFILE_OBJECT_PREFIX=wayang/skills",
                        "WAYANG_STORAGE_S3_ENDPOINT=http://localhost:9000",
                        "WAYANG_STORAGE_S3_BUCKET=wayang",
                        "WAYANG_STORAGE_S3_REGION=us-east-1",
                        "WAYANG_STORAGE_S3_ACCESS_KEY_ID=CHANGE_ME",
                        "WAYANG_STORAGE_S3_SECRET_ACCESS_KEY=CHANGE_ME",
                        "WAYANG_STORAGE_S3_PATH_STYLE_ACCESS=true",
                        "WAYANG_STORAGE_S3_PATH_PREFIX=tenants/acme",
                        "WAYANG_SKILLS_PROFILE_MAX_EVENTS=10000",
                        "WAYANG_SKILLS_LIFECYCLE_RECONCILE_MODE=inspect-only");
    }

    @Test
    void rendersGcsObjectStorageSampleFromProviderAlias() {
        SkillManagementRuntimeConfigSample sample =
                SkillManagementRuntimeConfigSamples.forProfile("gcs");

        assertThat(sample.profile()).isEqualTo("object-storage");
        assertThat(sample.description()).contains("Google Cloud Storage");
        assertThat(lines(sample.properties()))
                .containsExactly(
                        "wayang.skills.profile=object-storage",
                        "wayang.skills.profile.object-prefix=wayang/skills",
                        "wayang.storage.gcs.bucket=wayang",
                        "wayang.storage.gcs.project-id=CHANGE_ME_PROJECT",
                        "wayang.storage.gcs.path-prefix=tenants/acme",
                        "wayang.skills.profile.max-events=10000",
                        "wayang.skills.lifecycle.reconcile.mode=inspect-only");
        assertThat(lines(sample.environment()))
                .containsExactly(
                        "WAYANG_SKILLS_PROFILE=object-storage",
                        "WAYANG_SKILLS_PROFILE_OBJECT_PREFIX=wayang/skills",
                        "WAYANG_STORAGE_GCS_BUCKET=wayang",
                        "WAYANG_STORAGE_GCS_PROJECT_ID=CHANGE_ME_PROJECT",
                        "WAYANG_STORAGE_GCS_PATH_PREFIX=tenants/acme",
                        "WAYANG_SKILLS_PROFILE_MAX_EVENTS=10000",
                        "WAYANG_SKILLS_LIFECYCLE_RECONCILE_MODE=inspect-only");
    }

    @Test
    void rendersHybridSampleWithFileAndObjectDefaults() {
        SkillManagementRuntimeConfigSample sample =
                SkillManagementRuntimeConfigSamples.forProfile("hybrid");

        assertThat(sample.profile()).isEqualTo("hybrid-object-file");
        assertThat(lines(sample.properties()))
                .containsExactly(
                        "wayang.skills.profile=hybrid-object-file",
                        "wayang.skills.profile.base-directory=.wayang/skills",
                        "wayang.skills.profile.object-prefix=wayang/skills",
                        "wayang.storage.s3.endpoint=http://localhost:9000",
                        "wayang.storage.s3.bucket=wayang",
                        "wayang.storage.s3.region=us-east-1",
                        "wayang.storage.s3.access-key-id=CHANGE_ME",
                        "wayang.storage.s3.secret-access-key=CHANGE_ME",
                        "wayang.storage.s3.path-style-access=true",
                        "wayang.storage.s3.path-prefix=tenants/acme",
                        "wayang.skills.profile.max-events=10000",
                        "wayang.skills.lifecycle.reconcile.mode=inspect-only");
    }

    @Test
    void rendersAzureHybridSampleFromProviderAlias() {
        SkillManagementRuntimeConfigSample sample =
                SkillManagementRuntimeConfigSamples.forProfile("hybrid-azure");

        assertThat(sample.profile()).isEqualTo("hybrid-object-file");
        assertThat(sample.description()).contains("Azure Blob Storage");
        assertThat(lines(sample.properties()))
                .containsExactly(
                        "wayang.skills.profile=hybrid-object-file",
                        "wayang.skills.profile.base-directory=.wayang/skills",
                        "wayang.skills.profile.object-prefix=wayang/skills",
                        "wayang.storage.azure.connection-string=CHANGE_ME",
                        "wayang.storage.azure.container=wayang",
                        "wayang.storage.azure.path-prefix=tenants/acme",
                        "wayang.skills.profile.max-events=10000",
                        "wayang.skills.lifecycle.reconcile.mode=inspect-only");
        assertThat(lines(sample.environment()))
                .containsExactly(
                        "WAYANG_SKILLS_PROFILE=hybrid-object-file",
                        "WAYANG_SKILLS_PROFILE_BASE_DIRECTORY=.wayang/skills",
                        "WAYANG_SKILLS_PROFILE_OBJECT_PREFIX=wayang/skills",
                        "WAYANG_STORAGE_AZURE_CONNECTION_STRING=CHANGE_ME",
                        "WAYANG_STORAGE_AZURE_CONTAINER=wayang",
                        "WAYANG_STORAGE_AZURE_PATH_PREFIX=tenants/acme",
                        "WAYANG_SKILLS_PROFILE_MAX_EVENTS=10000",
                        "WAYANG_SKILLS_LIFECYCLE_RECONCILE_MODE=inspect-only");
    }

    @Test
    void rendersJdbcSampleWithSchemaInitialization() {
        SkillManagementRuntimeConfigSample sample =
                SkillManagementRuntimeConfigSamples.forProfile("db");

        assertThat(sample.profile()).isEqualTo("jdbc");
        assertThat(lines(sample.properties()))
                .containsExactly(
                        "wayang.skills.profile=jdbc",
                        "wayang.skills.profile.max-events=10000",
                        "wayang.skills.profile.initialize-jdbc-schema=true",
                        "wayang.skills.lifecycle.reconcile.mode=inspect-only");
    }

    @Test
    void rejectsUnknownProfile() {
        assertThatThrownBy(() -> SkillManagementRuntimeConfigSamples.forProfile("missing-profile"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Unknown skill-management service profile: missing-profile");
    }

    private static java.util.List<String> lines(
            java.util.List<SkillManagementRuntimeConfigSampleEntry> entries) {
        return entries.stream()
                .map(entry -> entry.key() + "=" + entry.value())
                .toList();
    }
}
