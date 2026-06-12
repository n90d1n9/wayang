package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangPersistenceHealthReportTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void emptyDurableStoreIsReadyButIncomplete() {
        FileAgenticCommerceWayangPersistenceStore store =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("empty"));

        AgenticCommerceWayangPersistenceHealthReport report =
                AgenticCommerceWayangPersistenceHealthReport.from(store);

        assertThat(report.ready()).isTrue();
        assertThat(report.complete()).isFalse();
        assertThat(report.healthStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceHealthSummary.STATUS_INCOMPLETE);
        assertThat(report.summary().incomplete()).isTrue();
        assertThat(report.availableDocumentCount()).isZero();
        assertThat(report.requiredDocumentCount()).isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(report.missingDocuments()).hasSize(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(report.failedDocuments()).isEmpty();
        assertThat(report.documentIndex().missingIds())
                .containsExactlyElementsOf(AgenticCommerceWayangPersistenceDocuments.ids());
        assertThat(report.documentStatus("runtimeConfig")).isPresent();
        assertThat(report.documentStatus(AgenticCommerceWayangPersistenceDocuments.MANIFEST))
                .get()
                .extracting(AgenticCommerceWayangPersistenceDocumentStatus::id)
                .isEqualTo("manifest");
        assertThat(report.documents())
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count())
                .allSatisfy(status -> {
                    assertThat(status.available()).isFalse();
                    assertThat(status.loadable()).isTrue();
                    assertThat(status.issues()).isEmpty();
                    assertThat(status.warningCount()).isEqualTo(1);
                    assertThat(status.attributes()).containsEntry("statusAvailable", false);
                });
        AgenticCommerceWayangPersistenceDocumentStatus runtimeStatus = report.documents().get(0);
        assertThat(runtimeStatus.id()).isEqualTo("runtimeConfig");
        assertThat(runtimeStatus.fileName()).isEqualTo("runtime-config.json");
        assertThat(runtimeStatus.warnings()).containsExactly("runtime_config_missing");
        assertThat(runtimeStatus.attributes()).containsKey("path");
        assertThat(report.issues()).isEmpty();
        assertThat(report.warnings())
                .containsExactly(
                        "runtime_config_missing",
                        "bootstrap_config_missing",
                        "bootstrap_report_missing",
                        "manifest_missing");
        assertThat(report.findingCount()).isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(report.errorFindingCount()).isZero();
        assertThat(report.warningFindingCount()).isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(report.findings().get(0).documentScoped()).isTrue();
        assertThat(report.findings().get(0).toMap())
                .containsEntry("severity", AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_WARNING)
                .containsEntry("source", AgenticCommerceWayangPersistenceHealthFinding.SOURCE_DOCUMENT)
                .containsEntry("code", "runtime_config_missing")
                .containsEntry("title", "Runtime config is missing")
                .containsEntry("blocking", false)
                .containsEntry("documentId", "runtimeConfig");
        assertThat(report.capabilities().durable()).isTrue();
        assertThat(map(report.storeStatus().get("target")))
                .containsEntry("targetKind", "file")
                .containsEntry("durable", true);
        assertThat(report.persistenceTarget())
                .containsEntry("targetKind", "file")
                .containsEntry("durable", true);
        assertThat(report.toMap())
                .containsEntry("ready", true)
                .containsEntry("complete", false)
                .containsEntry(
                        "healthStatus",
                        AgenticCommerceWayangPersistenceHealthSummary.STATUS_INCOMPLETE)
                .containsEntry("findingCount", AgenticCommerceWayangPersistenceDocuments.count())
                .containsEntry("errorFindingCount", 0)
                .containsEntry("warningFindingCount", AgenticCommerceWayangPersistenceDocuments.count())
                .containsEntry("availableDocumentCount", 0);
        assertThat(map(report.toMap().get("persistenceTarget")))
                .containsEntry("targetKind", "file")
                .containsEntry("durable", true);
        List<Object> findings = list(report.toMap().get("findings"));
        assertThat(findings).hasSize(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(map(findings.get(0)))
                .containsEntry("severity", AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_WARNING)
                .containsEntry("source", AgenticCommerceWayangPersistenceHealthFinding.SOURCE_DOCUMENT)
                .containsEntry("code", "runtime_config_missing")
                .containsEntry("title", "Runtime config is missing")
                .containsEntry("blocking", false)
                .containsEntry("documentId", "runtimeConfig");
        assertThat(map(report.toMap().get("summary")))
                .containsEntry(
                        "healthStatus",
                        AgenticCommerceWayangPersistenceHealthSummary.STATUS_INCOMPLETE)
                .containsEntry("missingDocumentCount", AgenticCommerceWayangPersistenceDocuments.count())
                .containsEntry("failedDocumentCount", 0);
        Map<String, Object> summaryAttributes = map(map(report.toMap().get("summary")).get("attributes"));
        assertThat(map(summaryAttributes.get("persistenceTarget"))).containsEntry("targetKind", "file");
        Map<String, Object> documentIndex = map(report.toMap().get("documentIndex"));
        assertThat(documentIndex)
                .containsEntry("documentCount", AgenticCommerceWayangPersistenceDocuments.count())
                .containsEntry("missingDocumentCount", AgenticCommerceWayangPersistenceDocuments.count())
                .containsEntry("failedDocumentCount", 0);
        assertThat(map(documentIndex.get("documentsById"))).containsKey("runtimeConfig");
        List<Object> documentMaps = list(report.toMap().get("documents"));
        assertThat(documentMaps).hasSize(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(map(documentMaps.get(0)))
                .containsEntry("id", "runtimeConfig")
                .containsEntry("fileName", "runtime-config.json")
                .containsEntry("available", false)
                .containsEntry("loadable", true);
    }

    @Test
    void completeEphemeralStoreReportsEphemeralWarnings() {
        InMemoryAgenticCommerceWayangPersistenceStore store =
                InMemoryAgenticCommerceWayangPersistenceStore.create();
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(store);

        AgenticCommerceWayangPersistenceHealthReport report =
                AgenticCommerceWayangPersistenceHealthReport.from(store);

        assertThat(report.ready()).isTrue();
        assertThat(report.complete()).isTrue();
        assertThat(report.healthStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceHealthSummary.STATUS_DEGRADED);
        assertThat(report.summary().degraded()).isTrue();
        assertThat(report.availableDocumentCount()).isEqualTo(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(report.missingDocuments()).isEmpty();
        assertThat(report.failedDocuments()).isEmpty();
        assertThat(report.documentIndex().failedIds()).isEmpty();
        assertThat(report.documentIndex().available("bootstrapReport")).isTrue();
        assertThat(report.documents())
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count())
                .allSatisfy(status -> {
                    assertThat(status.available()).isTrue();
                    assertThat(status.loadable()).isTrue();
                    assertThat(status.issues()).isEmpty();
                    assertThat(status.warnings()).isEmpty();
                    assertThat(status.attributes()).containsEntry("statusAvailable", true);
                });
        assertThat(report.warnings())
                .containsExactly(
                        "persistence_store_ephemeral",
                        "persistence_store_not_durable");
        assertThat(report.findingCount()).isEqualTo(2);
        assertThat(report.errorFindingCount()).isZero();
        assertThat(report.warningFindingCount()).isEqualTo(2);
        assertThat(report.findings().get(0).toMap())
                .containsEntry("severity", AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_WARNING)
                .containsEntry("source", AgenticCommerceWayangPersistenceHealthFinding.SOURCE_STORE)
                .containsEntry("code", "persistence_store_ephemeral")
                .doesNotContainKey("documentId");
        assertThat(report.capabilities().ephemeral()).isTrue();
        assertThat(map(report.toMap().get("capabilities")))
                .containsEntry("ephemeral", true)
                .containsEntry("durable", false);
        assertThat(map(report.toMap().get("summary")))
                .containsEntry(
                        "healthStatus",
                        AgenticCommerceWayangPersistenceHealthSummary.STATUS_DEGRADED)
                .containsEntry("missingDocumentCount", 0)
                .containsEntry("failedDocumentCount", 0);
    }

    @Test
    void failingStoreReportsLoadAndStatusIssues() {
        AgenticCommerceWayangPersistenceHealthReport report =
                AgenticCommerceWayangPersistenceHealthReport.from(new FailingPersistenceStore());

        assertThat(report.ready()).isFalse();
        assertThat(report.complete()).isFalse();
        assertThat(report.healthStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceHealthSummary.STATUS_UNAVAILABLE);
        assertThat(report.summary().unavailable()).isTrue();
        assertThat(report.statusReadable()).isFalse();
        assertThat(report.issueCount()).isEqualTo(5);
        assertThat(report.missingDocuments()).hasSize(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(report.failedDocuments()).hasSize(AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(report.documentIndex().failedIds())
                .containsExactlyElementsOf(AgenticCommerceWayangPersistenceDocuments.ids());
        assertThat(report.documentStatus("unknown")).isEmpty();
        assertThat(report.documents())
                .hasSize(AgenticCommerceWayangPersistenceDocuments.count())
                .allSatisfy(status -> {
                    assertThat(status.available()).isFalse();
                    assertThat(status.loadable()).isFalse();
                    assertThat(status.issueCount()).isEqualTo(1);
                    assertThat(status.warningCount()).isEqualTo(1);
                    assertThat(status.attributes()).isEmpty();
                });
        AgenticCommerceWayangPersistenceDocumentStatus runtimeStatus = report.documents().get(0);
        assertThat(runtimeStatus.issues()).containsExactly("runtime_config_load_failed");
        assertThat(runtimeStatus.warnings()).containsExactly("runtime_config_missing");
        assertThat(report.issues())
                .containsExactly(
                        "store_status_failed",
                        "runtime_config_load_failed",
                        "bootstrap_config_load_failed",
                        "bootstrap_report_load_failed",
                        "manifest_load_failed");
        assertThat(report.findingCount()).isEqualTo(9);
        assertThat(report.errorFindingCount()).isEqualTo(5);
        assertThat(report.warningFindingCount()).isEqualTo(4);
        assertThat(report.errorFindings().get(0).toMap())
                .containsEntry("severity", AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_ERROR)
                .containsEntry("source", AgenticCommerceWayangPersistenceHealthFinding.SOURCE_STORE)
                .containsEntry("code", "store_status_failed")
                .containsEntry("title", "Persistence store status unavailable")
                .containsEntry("blocking", true);
        assertThat(report.errorFindings().get(1).toMap())
                .containsEntry("severity", AgenticCommerceWayangPersistenceHealthFinding.SEVERITY_ERROR)
                .containsEntry("source", AgenticCommerceWayangPersistenceHealthFinding.SOURCE_DOCUMENT)
                .containsEntry("code", "runtime_config_load_failed")
                .containsEntry("documentId", "runtimeConfig");
        assertThat(report.warnings())
                .contains(
                        "runtime_config_missing",
                        "bootstrap_config_missing",
                        "bootstrap_report_missing",
                        "manifest_missing");
        assertThat(report.persistenceTarget())
                .containsEntry("storageKind", "failing")
                .containsEntry("targetKind", "failing");
        assertThat(map(report.toMap().get("summary")))
                .containsEntry(
                        "healthStatus",
                        AgenticCommerceWayangPersistenceHealthSummary.STATUS_UNAVAILABLE)
                .containsEntry("missingDocumentCount", AgenticCommerceWayangPersistenceDocuments.count())
                .containsEntry("failedDocumentCount", AgenticCommerceWayangPersistenceDocuments.count());
        assertThat(map(report.toMap().get("persistenceTarget")))
                .containsEntry("storageKind", "failing")
                .containsEntry("targetKind", "failing");
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<Object>) value;
    }

    private static final class FailingPersistenceStore implements AgenticCommerceWayangPersistenceStore {

        @Override
        public String storageKind() {
            return "failing";
        }

        @Override
        public Optional<AgenticCommerceWayangRuntimeConfig> loadRuntimeConfig() {
            throw failure();
        }

        @Override
        public void saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig runtimeConfig) {
            throw failure();
        }

        @Override
        public Optional<AgenticCommerceWayangBootstrapConfig> loadBootstrapConfig() {
            throw failure();
        }

        @Override
        public void saveBootstrapConfig(AgenticCommerceWayangBootstrapConfig bootstrapConfig) {
            throw failure();
        }

        @Override
        public Optional<Map<String, Object>> loadBootstrapReport() {
            throw failure();
        }

        @Override
        public void saveBootstrapReport(AgenticCommerceWayangBootstrapReport bootstrapReport) {
            throw failure();
        }

        @Override
        public Optional<Map<String, Object>> loadManifest() {
            throw failure();
        }

        @Override
        public void saveManifest(AgenticCommerceWayangManifest manifest) {
            throw failure();
        }

        @Override
        public Map<String, Object> toMap() {
            throw failure();
        }

        private static IllegalStateException failure() {
            return new IllegalStateException("store unavailable");
        }
    }
}
