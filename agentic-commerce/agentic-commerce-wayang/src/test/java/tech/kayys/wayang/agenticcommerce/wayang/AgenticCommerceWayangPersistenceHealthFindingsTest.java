package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangPersistenceHealthFindingsTest {

    @Test
    void catalogExposesKnownFindingDefinitions() {
        AgenticCommerceWayangPersistenceHealthFindingDefinition definition =
                AgenticCommerceWayangPersistenceHealthFindings.find("store_status_failed").orElseThrow();

        assertThat(definition.severity()).isEqualTo(AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_ERROR);
        assertThat(definition.source()).isEqualTo(AgenticCommerceWayangPersistenceHealthFinding.SOURCE_STORE);
        assertThat(definition.blocking()).isTrue();
        assertThat(definition.title()).isEqualTo("Persistence store status unavailable");
        assertThat(definition.remediation()).contains("configuration");
        assertThat(AgenticCommerceWayangPersistenceHealthFindings.toMapList())
                .hasSize(AgenticCommerceWayangPersistenceHealthFindings.ALL.size());
    }

    @Test
    void findingUsesCatalogMetadataAndFallbackDefinitions() {
        AgenticCommerceWayangPersistenceHealthFinding known =
                AgenticCommerceWayangPersistenceHealthFinding.documentWarning(
                        new AgenticCommerceWayangPersistenceDocumentStatus(
                                AgenticCommerceWayangPersistenceDocuments.RUNTIME_CONFIG,
                                false,
                                true,
                                null,
                                null,
                                Map.of("path", "/tmp/runtime-config.json")),
                        "runtime_config_missing");

        assertThat(known.title()).isEqualTo("Runtime config is missing");
        assertThat(known.blocking()).isFalse();
        assertThat(known.toMap())
                .containsEntry("title", "Runtime config is missing")
                .containsEntry("blocking", false)
                .containsEntry("documentId", "runtimeConfig");

        AgenticCommerceWayangPersistenceHealthFinding unknown =
                new AgenticCommerceWayangPersistenceHealthFinding(
                        "error",
                        "custom_store_failure",
                        "store",
                        "",
                        "",
                        Map.of());

        assertThat(unknown.title()).isEqualTo("Custom Store Failure");
        assertThat(unknown.blocking()).isTrue();
        assertThat(unknown.definition().attributes()).containsEntry("catalogFallback", true);
    }
}
