package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangPersistenceConfigValidationReportTest {

    @Test
    void fileConfigIsValidWithoutFindings() {
        AgenticCommerceWayangPersistenceConfig config =
                AgenticCommerceWayangPersistenceConfig.file("agentic-commerce");

        AgenticCommerceWayangPersistenceConfigValidationReport report = config.validationReport();

        assertThat(report.valid()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.errorCodes()).isEmpty();
        assertThat(report.warningCodes()).isEmpty();
        assertThat(map(config.toMap().get("validation")))
                .containsEntry("valid", true)
                .containsEntry("issueCount", 0);
    }

    @Test
    void inMemoryConfigWarnsAboutEphemeralPersistence() {
        AgenticCommerceWayangPersistenceConfigValidationReport report =
                AgenticCommerceWayangPersistenceConfig.memory().validationReport();

        assertThat(report.valid()).isTrue();
        assertThat(report.warningCodes()).containsExactly("in_memory_persistence_ephemeral");
        assertThat(report.errorCodes()).isEmpty();
    }

    @Test
    void unsupportedStorageKindReportsErrorUnderDefaultProviders() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "hosted-database"));

        AgenticCommerceWayangPersistenceConfigValidationReport report = config.validationReport();

        assertThat(report.valid()).isFalse();
        assertThat(report.errorCodes()).containsExactly("unsupported_storage_kind");
        assertThat(map(report.toMap().get("target"))).containsEntry("storageKind", "hosted-database");
    }

    @Test
    void hybridConfigReportsMirrorAndEphemeralFallbackRisks() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.hybrid(
                AgenticCommerceWayangPersistenceConfig.file("primary"),
                AgenticCommerceWayangPersistenceConfig.memory(),
                false);

        AgenticCommerceWayangPersistenceConfigValidationReport report = config.validationReport();

        assertThat(report.valid()).isTrue();
        assertThat(report.warningCodes())
                .contains(
                        "in_memory_persistence_ephemeral",
                        "hybrid_writes_not_mirrored",
                        "hybrid_fallback_ephemeral");
        assertThat(report.errorCodes()).isEmpty();
    }

    @Test
    void hybridConfigReportsSameTargetRisk() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.hybrid(
                AgenticCommerceWayangPersistenceConfig.file("same-state"),
                AgenticCommerceWayangPersistenceConfig.file("same-state"),
                true);

        AgenticCommerceWayangPersistenceConfigValidationReport report = config.validationReport();

        assertThat(report.valid()).isTrue();
        assertThat(report.warningCodes()).contains("hybrid_primary_fallback_same_target");
    }

    @Test
    void rustFsConfigWarnsWhenEndpointIsMissing() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.fromMap(Map.of(
                "storageKind",
                "rustfs",
                "bucket",
                "wayang-state",
                "keyPrefix",
                "prod"));

        AgenticCommerceWayangPersistenceConfigValidationReport report = config.validationReport();

        assertThat(report.valid()).isTrue();
        assertThat(report.warningCodes()).containsExactly("object_store_endpoint_missing");
    }

    @Test
    void genericDatabaseConfigWarnsAboutProviderAmbiguity() {
        AgenticCommerceWayangPersistenceConfig config = AgenticCommerceWayangPersistenceConfig.database(
                AgenticCommerceDatabasePersistenceConfig.defaults());

        AgenticCommerceWayangPersistenceConfigValidationReport report = config.validationReport();

        assertThat(report.valid()).isTrue();
        assertThat(report.warningCodes()).containsExactly("database_provider_generic");
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
