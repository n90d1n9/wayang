package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangPersistenceDocumentsTest {

    @Test
    void catalogListsStableDocumentIdsAndFileNames() {
        assertThat(AgenticCommerceWayangPersistenceDocuments.count()).isEqualTo(4);
        assertThat(AgenticCommerceWayangPersistenceDocuments.ids())
                .containsExactly("runtimeConfig", "bootstrapConfig", "bootstrapReport", "manifest");
        assertThat(AgenticCommerceWayangPersistenceDocuments.fileNames())
                .containsExactly(
                        "runtime-config.json",
                        "bootstrap-config.json",
                        "bootstrap-report.json",
                        "manifest.json");
    }

    @Test
    void catalogResolvesDocumentById() {
        AgenticCommerceWayangPersistenceDocument document =
                AgenticCommerceWayangPersistenceDocuments.find("bootstrapReport").orElseThrow();

        assertThat(document.fileName()).isEqualTo("bootstrap-report.json");
        assertThat(document.availabilityStatusKey()).isEqualTo("bootstrapReportAvailable");
        assertThat(document.missingWarning()).isEqualTo("bootstrap_report_missing");
        assertThat(document.loadFailureIssue()).isEqualTo("bootstrap_report_load_failed");
        assertThat(AgenticCommerceWayangPersistenceDocuments.find("unknown")).isEmpty();
    }

    @Test
    void catalogExportsJsonReadyMetadata() {
        List<Map<String, Object>> values = AgenticCommerceWayangPersistenceDocuments.toMapList();

        assertThat(values).hasSize(4);
        assertThat(values.get(0))
                .containsEntry("id", "runtimeConfig")
                .containsEntry("fileName", "runtime-config.json")
                .containsEntry("pathStatusKey", "runtimeConfigPath")
                .containsEntry("objectKeyStatusKey", "runtimeConfigKey")
                .containsEntry("availabilityStatusKey", "runtimeConfigAvailable");
    }
}
