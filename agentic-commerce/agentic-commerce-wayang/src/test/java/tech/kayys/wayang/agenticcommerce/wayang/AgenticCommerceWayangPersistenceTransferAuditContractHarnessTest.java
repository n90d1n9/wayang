package tech.kayys.wayang.agenticcommerce.wayang;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static tech.kayys.wayang.agenticcommerce.wayang.AgenticCommerceWayangPersistenceTransferAuditContractIssues.SINK_BUILD_FAILED;

class AgenticCommerceWayangPersistenceTransferAuditContractHarnessTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void inMemoryAuditStorePassesContract() {
        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.retainedLatestTwo()
                        .run(() -> new InMemoryAgenticCommerceWayangPersistenceTransferAuditSink(2));

        assertThat(report.passed()).isTrue();
        assertThat(report.issueCount()).isZero();
        assertThat(report.retainedTrailCount()).isEqualTo(2);
        assertThat(report.reloadAttempted()).isFalse();
        assertThat(report.toMap())
                .containsEntry("passed", true)
                .containsEntry("retainedTrailCount", 2);
        assertThat(map(report.retainedPage()).get("trailTypes").toString())
                .contains(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
    }

    @Test
    void fileSystemAuditStorePassesReloadContract() throws Exception {
        Path journal = temporaryDirectory.resolve("contract/file/audit.jsonl");

        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.retainedLatestTwo()
                        .run(() -> new FileSystemAgenticCommerceWayangPersistenceTransferAuditStore(journal, 2), true);

        assertThat(report.passed()).isTrue();
        assertThat(report.reloadAttempted()).isTrue();
        assertThat(report.reloadTrailCount()).isEqualTo(2);
        assertRetainedJsonl(Files.readAllLines(journal, StandardCharsets.UTF_8));
    }

    @Test
    void objectStoreAuditStorePassesReloadContract() {
        InMemoryAgenticCommerceObjectStoreClient client =
                InMemoryAgenticCommerceObjectStoreClient.create();
        AgenticCommerceObjectStoreConfig config = AgenticCommerceObjectStoreConfig.fromMap(Map.of(
                "provider",
                "s3",
                "bucket",
                "wayang-audit",
                "keyPrefix",
                "contract"));

        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.retainedLatestTwo()
                        .run(() -> ObjectStoreAgenticCommerceWayangPersistenceTransferAuditStore.configured(
                                config,
                                client,
                                "audit.jsonl",
                                2), true);

        assertThat(report.passed()).isTrue();
        assertThat(report.reloadTrailCount()).isEqualTo(2);
        String auditObjectKey = config.objectKey("audit.jsonl");
        assertThat(client.contains("wayang-audit", auditObjectKey)).isTrue();
        List<String> auditObjectLines = AgenticCommerceWayangPersistenceTransferAuditJsonl.linesFromBody(
                client.readText("wayang-audit", auditObjectKey).orElseThrow());
        assertRetainedJsonl(auditObjectLines);
    }

    @Test
    void databaseAuditStorePassesReloadContract() {
        InMemoryAgenticCommerceDatabasePersistenceClient client =
                InMemoryAgenticCommerceDatabasePersistenceClient.create();
        AgenticCommerceDatabasePersistenceConfig config = AgenticCommerceDatabasePersistenceConfig.fromMap(Map.of(
                "provider",
                "postgres",
                "table",
                "wayang_audit",
                "namespace",
                "contract"));

        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.retainedLatestTwo()
                        .run(() -> DatabaseAgenticCommerceWayangPersistenceTransferAuditStore.configured(
                                config,
                                client,
                                "audit.jsonl",
                                2), true);

        assertThat(report.passed()).isTrue();
        assertThat(report.reloadTrailCount()).isEqualTo(2);
        String auditDocumentKey = config.documentKey("audit.jsonl");
        assertThat(client.contains("wayang_audit", auditDocumentKey)).isTrue();
        List<String> auditDocumentLines = AgenticCommerceWayangPersistenceTransferAuditJsonl.linesFromBody(
                client.readText("wayang_audit", auditDocumentKey).orElseThrow());
        assertRetainedJsonl(auditDocumentLines);
    }

    @Test
    void failingFactoryReportsIssue() {
        AgenticCommerceWayangPersistenceTransferAuditContractReport report =
                AgenticCommerceWayangPersistenceTransferAuditContractHarness.retainedLatestTwo()
                        .run(() -> {
                            throw new IllegalStateException("audit store unavailable");
                        }, true);

        assertThat(report.passed()).isFalse();
        assertThat(report.issueCount()).isEqualTo(1);
        assertThat(report.issues()).containsExactly(SINK_BUILD_FAILED);
        assertThat(report.reloadAttempted()).isTrue();
        assertThat(report.retainedTrailCount()).isZero();
        assertThat(report.reloadTrailCount()).isZero();
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }

    private static void assertRetainedJsonl(List<String> lines) {
        assertThat(lines).hasSize(2);
        String body = String.join("\n", lines);
        assertThat(body)
                .contains(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY,
                        "complete",
                        "applied")
                .doesNotContain(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT);
    }
}
