package tech.kayys.wayang.agent.skills.management;

/**
 * Shared object-storage provider vocabulary for samples and readiness checks.
 */
enum SkillManagementObjectStorageProviderKind {
    S3_RUSTFS(
            "s3-rustfs",
            "S3/RustFS",
            "S3/RustFS",
            "S3/RustFS-compatible object storage"),
    GCS(
            "gcs",
            "GCS",
            "GCS",
            "Google Cloud Storage"),
    AZURE(
            "azure",
            "Azure Blob Storage",
            "Azure",
            "Azure Blob Storage");

    private final String configName;
    private final String readinessLabel;
    private final String summaryLabel;
    private final String sampleLabel;

    SkillManagementObjectStorageProviderKind(
            String configName,
            String readinessLabel,
            String summaryLabel,
            String sampleLabel) {
        this.configName = configName;
        this.readinessLabel = readinessLabel;
        this.summaryLabel = summaryLabel;
        this.sampleLabel = sampleLabel;
    }

    String configName() {
        return configName;
    }

    String readinessLabel() {
        return readinessLabel;
    }

    String summaryLabel() {
        return summaryLabel;
    }

    String sampleLabel() {
        return sampleLabel;
    }
}
