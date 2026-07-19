package tech.kayys.wayang.agent.skills.management;

import java.util.List;

/**
 * Runtime configuration hints for platform object-storage providers.
 */
final class SkillManagementObjectStorageProviderConfigHints {

    private static final String DEFAULT_ENDPOINT = "http://localhost:9000";
    private static final String DEFAULT_BUCKET = "wayang";
    private static final String DEFAULT_REGION = "us-east-1";
    private static final String DEFAULT_ACCESS_KEY_ID = "CHANGE_ME";
    private static final String DEFAULT_SECRET_ACCESS_KEY = "CHANGE_ME";
    private static final String DEFAULT_PATH_STYLE_ACCESS = "true";
    private static final String DEFAULT_PATH_PREFIX = "tenants/acme";
    private static final String DEFAULT_GCS_PROJECT_ID = "CHANGE_ME_PROJECT";
    private static final String DEFAULT_AZURE_CONNECTION_STRING = "CHANGE_ME";

    private static final SkillManagementObjectStorageProviderConfigKey ENDPOINT_KEY = optional(
            "wayang.storage.s3.endpoint",
            "WAYANG_STORAGE_S3_ENDPOINT",
            DEFAULT_ENDPOINT,
            "S3/RustFS endpoint.");
    private static final SkillManagementObjectStorageProviderConfigKey BUCKET_KEY = required(
            "wayang.storage.s3.bucket",
            "WAYANG_STORAGE_S3_BUCKET",
            DEFAULT_BUCKET,
            "S3/RustFS bucket.");
    private static final SkillManagementObjectStorageProviderConfigKey REGION_KEY = required(
            "wayang.storage.s3.region",
            "WAYANG_STORAGE_S3_REGION",
            DEFAULT_REGION,
            "S3/RustFS region.");
    private static final SkillManagementObjectStorageProviderConfigKey ACCESS_KEY_ID_KEY = required(
            "wayang.storage.s3.access-key-id",
            "WAYANG_STORAGE_S3_ACCESS_KEY_ID",
            DEFAULT_ACCESS_KEY_ID,
            "S3/RustFS access key id.");
    private static final SkillManagementObjectStorageProviderConfigKey SECRET_ACCESS_KEY_KEY = required(
            "wayang.storage.s3.secret-access-key",
            "WAYANG_STORAGE_S3_SECRET_ACCESS_KEY",
            DEFAULT_SECRET_ACCESS_KEY,
            "S3/RustFS secret access key.");
    private static final SkillManagementObjectStorageProviderConfigKey PATH_STYLE_ACCESS_KEY = optional(
            "wayang.storage.s3.path-style-access",
            "WAYANG_STORAGE_S3_PATH_STYLE_ACCESS",
            DEFAULT_PATH_STYLE_ACCESS,
            "Enable path-style access for RustFS or MinIO.");
    private static final SkillManagementObjectStorageProviderConfigKey PATH_PREFIX_KEY = optional(
            "wayang.storage.s3.path-prefix",
            "WAYANG_STORAGE_S3_PATH_PREFIX",
            DEFAULT_PATH_PREFIX,
            "Bucket-wide object prefix before skill paths.");

    private static final SkillManagementObjectStorageProviderConfigKey GCS_BUCKET_KEY = required(
            "wayang.storage.gcs.bucket",
            "WAYANG_STORAGE_GCS_BUCKET",
            DEFAULT_BUCKET,
            "GCS bucket.");
    private static final SkillManagementObjectStorageProviderConfigKey GCS_PROJECT_ID_KEY = optional(
            "wayang.storage.gcs.project-id",
            "WAYANG_STORAGE_GCS_PROJECT_ID",
            DEFAULT_GCS_PROJECT_ID,
            "GCS project id.");
    private static final SkillManagementObjectStorageProviderConfigKey GCS_PATH_PREFIX_KEY = optional(
            "wayang.storage.gcs.path-prefix",
            "WAYANG_STORAGE_GCS_PATH_PREFIX",
            DEFAULT_PATH_PREFIX,
            "Bucket-wide object prefix before skill paths.");

    private static final SkillManagementObjectStorageProviderConfigKey AZURE_CONNECTION_STRING_KEY = required(
            "wayang.storage.azure.connection-string",
            "WAYANG_STORAGE_AZURE_CONNECTION_STRING",
            DEFAULT_AZURE_CONNECTION_STRING,
            "Azure Blob Storage connection string.");
    private static final SkillManagementObjectStorageProviderConfigKey AZURE_CONTAINER_KEY = required(
            "wayang.storage.azure.container",
            "WAYANG_STORAGE_AZURE_CONTAINER",
            DEFAULT_BUCKET,
            "Azure Blob Storage container.");
    private static final SkillManagementObjectStorageProviderConfigKey AZURE_PATH_PREFIX_KEY = optional(
            "wayang.storage.azure.path-prefix",
            "WAYANG_STORAGE_AZURE_PATH_PREFIX",
            DEFAULT_PATH_PREFIX,
            "Container-wide object prefix before skill paths.");

    private static final SkillManagementObjectStorageProviderConfigKeySet S3_KEYS = keys(
            List.of(
                    ENDPOINT_KEY,
                    BUCKET_KEY,
                    REGION_KEY,
                    ACCESS_KEY_ID_KEY,
                    SECRET_ACCESS_KEY_KEY,
                    PATH_STYLE_ACCESS_KEY,
                    PATH_PREFIX_KEY),
            List.of(
                    ACCESS_KEY_ID_KEY,
                    SECRET_ACCESS_KEY_KEY,
                    BUCKET_KEY,
                    REGION_KEY,
                    ENDPOINT_KEY,
                    PATH_STYLE_ACCESS_KEY,
                    PATH_PREFIX_KEY));
    private static final SkillManagementObjectStorageProviderConfigKeySet GCS_KEYS = keys(
            List.of(
                    GCS_BUCKET_KEY,
                    GCS_PROJECT_ID_KEY,
                    GCS_PATH_PREFIX_KEY),
            List.of(
                    GCS_BUCKET_KEY,
                    GCS_PROJECT_ID_KEY,
                    GCS_PATH_PREFIX_KEY));
    private static final SkillManagementObjectStorageProviderConfigKeySet AZURE_KEYS = keys(
            List.of(
                    AZURE_CONNECTION_STRING_KEY,
                    AZURE_CONTAINER_KEY,
                    AZURE_PATH_PREFIX_KEY),
            List.of(
                    AZURE_CONNECTION_STRING_KEY,
                    AZURE_CONTAINER_KEY,
                    AZURE_PATH_PREFIX_KEY));

    private SkillManagementObjectStorageProviderConfigHints() {
    }

    static List<SkillManagementRuntimeConfigHint> hints() {
        return List.of(
                hint(
                        "endpoint",
                        "S3-compatible endpoint for object storage. Use a RustFS/MinIO URL for local or private clouds.",
                        ENDPOINT_KEY,
                        List.of("Leave blank to use the default AWS S3 endpoint for the configured region.")),
                hint(
                        "bucket",
                        "Bucket that stores skill definitions, lifecycle state, event history, and artifacts.",
                        BUCKET_KEY,
                        List.of()),
                hint(
                        "region",
                        "S3 region used by the object-storage provider.",
                        REGION_KEY,
                        List.of("RustFS and MinIO still require a region value; us-east-1 is usually accepted.")),
                hint(
                        "access-key-id",
                        "Access key id for the S3-compatible object-storage provider.",
                        ACCESS_KEY_ID_KEY,
                        List.of("Prefer environment variables or secret injection for production deployments.")),
                hint(
                        "secret-access-key",
                        "Secret access key for the S3-compatible object-storage provider.",
                        SECRET_ACCESS_KEY_KEY,
                        List.of("Prefer environment variables or secret injection for production deployments.")),
                hint(
                        "path-style-access",
                        "Enables path-style S3 access for RustFS, MinIO, and many private S3-compatible endpoints.",
                        PATH_STYLE_ACCESS_KEY,
                        List.of("Set false for AWS virtual-hosted style access when appropriate.")),
                hint(
                        "path-prefix",
                        "Optional bucket-wide object prefix applied before skill-management object prefixes.",
                        PATH_PREFIX_KEY,
                        List.of("This is separate from wayang.skills.profile.object-prefix.")),
                hint(
                        "gcs.bucket",
                        "GCS bucket used when the object-storage provider is Google Cloud Storage.",
                        GCS_BUCKET_KEY,
                        List.of("Use this instead of S3 bucket settings when a GCS provider module is active.")),
                hint(
                        "gcs.project-id",
                        "Optional GCS project id used to create provider clients.",
                        GCS_PROJECT_ID_KEY,
                        List.of("Leave blank to rely on Google Cloud default project resolution.")),
                hint(
                        "gcs.path-prefix",
                        "Optional GCS bucket-wide object prefix applied before skill-management object prefixes.",
                        GCS_PATH_PREFIX_KEY,
                        List.of("This is separate from wayang.skills.profile.object-prefix.")),
                hint(
                        "azure.connection-string",
                        "Azure Blob Storage connection string used by the Azure object-storage provider.",
                        AZURE_CONNECTION_STRING_KEY,
                        List.of("Prefer environment variables or secret injection for production deployments.")),
                hint(
                        "azure.container",
                        "Azure Blob Storage container used for skill-management object storage.",
                        AZURE_CONTAINER_KEY,
                        List.of("Use this instead of S3 bucket settings when an Azure provider module is active.")),
                hint(
                        "azure.path-prefix",
                        "Optional Azure container-wide object prefix applied before skill-management object prefixes.",
                        AZURE_PATH_PREFIX_KEY,
                        List.of("This is separate from wayang.skills.profile.object-prefix.")));
    }

    static List<SkillManagementRuntimeConfigSampleEntry> sampleEntries(
            SkillManagementObjectStorageProviderKind kind,
            boolean environment) {
        return keySet(kind).sampleEntries(environment);
    }

    static List<String> requiredProperties(SkillManagementObjectStorageProviderKind kind) {
        return keySet(kind).requiredProperties();
    }

    static List<String> optionalProperties(SkillManagementObjectStorageProviderKind kind) {
        return keySet(kind).optionalProperties();
    }

    private static SkillManagementRuntimeConfigHint hint(
            String name,
            String description,
            SkillManagementObjectStorageProviderConfigKey key,
            List<String> notes) {
        return new SkillManagementRuntimeConfigHint(
                name,
                description,
                List.of(key.property()),
                List.of(key.environment()),
                key.defaultValue(),
                notes);
    }

    private static SkillManagementObjectStorageProviderConfigKeySet keySet(
            SkillManagementObjectStorageProviderKind kind) {
        if (kind == null) {
            return keys(List.of(), List.of());
        }
        return switch (kind) {
            case S3_RUSTFS -> S3_KEYS;
            case GCS -> GCS_KEYS;
            case AZURE -> AZURE_KEYS;
        };
    }

    private static SkillManagementObjectStorageProviderConfigKeySet keys(
            List<SkillManagementObjectStorageProviderConfigKey> sampleKeys,
            List<SkillManagementObjectStorageProviderConfigKey> readinessKeys) {
        return new SkillManagementObjectStorageProviderConfigKeySet(
                sampleKeys,
                readinessKeys);
    }

    private static SkillManagementObjectStorageProviderConfigKey required(
            String property,
            String environmentKey,
            String defaultValue,
            String description) {
        return new SkillManagementObjectStorageProviderConfigKey(
                property,
                environmentKey,
                defaultValue,
                description,
                true);
    }

    private static SkillManagementObjectStorageProviderConfigKey optional(
            String property,
            String environmentKey,
            String defaultValue,
            String description) {
        return new SkillManagementObjectStorageProviderConfigKey(
                property,
                environmentKey,
                defaultValue,
                description,
                false);
    }
}
