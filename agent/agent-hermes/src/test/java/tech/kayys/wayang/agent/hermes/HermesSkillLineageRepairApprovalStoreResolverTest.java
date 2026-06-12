package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HermesSkillLineageRepairApprovalStoreResolverTest {

    @Test
    void defaultsToNoopApprovalStore() {
        HermesSkillLineageRepairApprovalStore store =
                HermesSkillLineageRepairApprovalStoreResolver.resolve(null);

        assertThat(store.toMetadata())
                .containsEntry("storeType", "noop")
                .containsEntry("configured", false)
                .containsEntry("approvalCount", 0);
        assertThat(HermesSkillLineageRepairApprovalStoreResolver.metadata(null))
                .containsEntry("approvalStore", "noop")
                .containsEntry("durable", false)
                .containsEntry("fileFallback", false);
    }

    @Test
    void resolvesFileSystemApprovalStoreFromConfig(@TempDir Path tempDir) throws Exception {
        Path approvalPath = tempDir.resolve("repair-approvals.jsonl");
        writeApproval(
                approvalPath,
                new HermesSkillLineageRepairApproval(
                        "approval-file-001",
                        HermesSkillLineageRepairAdapter.APPLY,
                        "repair-key-file-001",
                        "rustfs",
                        "object-storage",
                        true,
                        "operator-a",
                        "file approval",
                        Map.of("ticket", "OPS-101")));
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairApprovalStore("file-system")
                .skillLineageRepairApprovalPath(approvalPath.toString())
                .build();

        HermesSkillLineageRepairApprovalStore store =
                HermesSkillLineageRepairApprovalStoreResolver.resolve(config);
        HermesSkillLineageRepairApprovalDecision decision = store.authorize(request(
                "approval-file-001",
                "repair-key-file-001",
                "rustfs",
                "object-storage"));

        assertThat(store)
                .isInstanceOf(FileSystemHermesSkillLineageRepairApprovalStore.class);
        assertThat(decision)
                .returns(true, HermesSkillLineageRepairApprovalDecision::approved)
                .returns("approved", HermesSkillLineageRepairApprovalDecision::status)
                .returns("file approval", HermesSkillLineageRepairApprovalDecision::reason);
        assertThat(store.toMetadata())
                .containsEntry("storeType", "file-system")
                .containsEntry("approvalPath", approvalPath.toString())
                .containsEntry("approvalCount", 1);
        assertThat(HermesSkillLineageRepairApprovalStoreResolver.metadata(config))
                .containsEntry("approvalStore", "file-system")
                .containsEntry("approvalPath", approvalPath.toString())
                .containsEntry("durable", true)
                .containsEntry("fileFallback", true);
    }

    @Test
    void resolvesObjectStorageApprovalStoreFromConfig() {
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        putApproval(
                storage,
                "tenant-a/hermes/repair-approvals/approval-object-001.jsonl",
                new HermesSkillLineageRepairApproval(
                        "approval-object-001",
                        HermesSkillLineageRepairAdapter.APPLY,
                        "repair-key-object-001",
                        "s3",
                        "object-storage",
                        true,
                        "operator-a",
                        "object approval",
                        Map.of()));
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairApprovalStore("object-storage")
                .skillLineageRepairApprovalObjectPrefix("/tenant-a/hermes/repair-approvals")
                .build();

        HermesSkillLineageRepairApprovalStore store =
                HermesSkillLineageRepairApprovalStoreResolver.resolve(config, Optional.of(storage));
        HermesSkillLineageRepairApprovalDecision decision = store.authorize(request(
                "approval-object-001",
                "repair-key-object-001",
                "s3",
                "object-storage"));

        assertThat(store)
                .isInstanceOf(ObjectStorageHermesSkillLineageRepairApprovalStore.class);
        assertThat(decision.approved()).isTrue();
        assertThat(store.toMetadata())
                .containsEntry("storeType", "object-storage")
                .containsEntry("approvalPrefix", "tenant-a/hermes/repair-approvals/")
                .containsEntry("approvalCount", 1);
        assertThat(HermesSkillLineageRepairApprovalStoreResolver.metadata(config))
                .containsEntry("approvalStore", "object-storage")
                .containsEntry("approvalObjectPrefix", "/tenant-a/hermes/repair-approvals")
                .containsEntry("durable", true)
                .containsEntry("fileFallback", false);
    }

    @Test
    void objectStorageApprovalStoreRequiresObjectStorageService() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairApprovalStore("object-storage")
                .build();

        assertThatThrownBy(() -> HermesSkillLineageRepairApprovalStoreResolver.resolve(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ObjectStorageService");
    }

    @Test
    void resolvesDatabaseApprovalStoreFromConfig() {
        InMemoryApprovalDataSource dataSource = new InMemoryApprovalDataSource();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairApprovalStore("database")
                .skillLineageRepairApprovalJdbcTableName("hermes_approval_grants")
                .skillLineageRepairApprovalJdbcInitializeSchema(true)
                .build();

        HermesSkillLineageRepairApprovalStore store =
                HermesSkillLineageRepairApprovalStoreResolver.resolve(
                        config,
                        Optional.empty(),
                        Optional.of(dataSource));
        ((DatabaseHermesSkillLineageRepairApprovalStore) store).save(new HermesSkillLineageRepairApproval(
                "approval-db-001",
                HermesSkillLineageRepairAdapter.APPLY,
                "repair-key-db-001",
                "database",
                "database",
                true,
                "operator-a",
                "database approval",
                Map.of("ticket", "DB-1")));

        HermesSkillLineageRepairApprovalDecision decision = store.authorize(request(
                "approval-db-001",
                "repair-key-db-001",
                "database",
                "database"));

        assertThat(store)
                .isInstanceOf(DatabaseHermesSkillLineageRepairApprovalStore.class);
        assertThat(decision)
                .returns(true, HermesSkillLineageRepairApprovalDecision::approved)
                .returns("database approval", HermesSkillLineageRepairApprovalDecision::reason);
        assertThat(store.toMetadata())
                .containsEntry("storeType", "database")
                .containsEntry("approvalTable", "hermes_approval_grants")
                .containsEntry("approvalCount", 1);
        assertThat(HermesSkillLineageRepairApprovalStoreResolver.metadata(config))
                .containsEntry("approvalStore", "database")
                .containsEntry("approvalJdbcTableName", "hermes_approval_grants")
                .containsEntry("databaseCapable", true)
                .containsEntry("fileFallback", false);
    }

    @Test
    void databaseApprovalStoreRequiresDataSource() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairApprovalStore("database")
                .build();

        assertThatThrownBy(() -> HermesSkillLineageRepairApprovalStoreResolver.resolve(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DataSource");
    }

    @Test
    void resolvesHybridApprovalStoreWithFileFallback(@TempDir Path tempDir) throws Exception {
        Path approvalPath = tempDir.resolve("hybrid-repair-approvals.jsonl");
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        writeApproval(
                approvalPath,
                new HermesSkillLineageRepairApproval(
                        "approval-hybrid-001",
                        HermesSkillLineageRepairAdapter.APPLY,
                        "repair-key-hybrid-001",
                        "rustfs",
                        "object-storage",
                        true,
                        "operator-a",
                        "fallback approval",
                        Map.of()));
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairApprovalStore("hybrid")
                .skillLineageRepairApprovalPath(approvalPath.toString())
                .skillLineageRepairApprovalObjectPrefix("tenant-a/hermes/repair-approvals")
                .build();

        HermesSkillLineageRepairApprovalStore store =
                HermesSkillLineageRepairApprovalStoreResolver.resolve(config, Optional.of(storage));
        HermesSkillLineageRepairApprovalStore fileOnlyStore =
                HermesSkillLineageRepairApprovalStoreResolver.resolve(config, Optional.empty());

        assertThat(store)
                .isInstanceOf(HybridHermesSkillLineageRepairApprovalStore.class);
        assertThat(store.authorize(request(
                "approval-hybrid-001",
                "repair-key-hybrid-001",
                "rustfs",
                "object-storage")).approved())
                .isTrue();
        assertThat(fileOnlyStore.authorize(request(
                "approval-hybrid-001",
                "repair-key-hybrid-001",
                "rustfs",
                "object-storage")).approved())
                .isTrue();
        assertThat(metadataMap(store.toMetadata(), "primaryStore"))
                .containsEntry("storeType", "object-storage")
                .containsEntry("approvalPrefix", "tenant-a/hermes/repair-approvals/");
        assertThat(metadataMap(store.toMetadata(), "fallbackStore"))
                .containsEntry("storeType", "file-system")
                .containsEntry("approvalPath", approvalPath.toString());
        assertThat(HermesSkillLineageRepairApprovalStoreResolver.metadata(config))
                .containsEntry("approvalStore", "hybrid")
                .containsEntry("durable", true)
                .containsEntry("fileFallback", true);
    }

    @Test
    void resolvesHybridApprovalStoreWithDatabasePrimary(@TempDir Path tempDir) throws Exception {
        Path approvalPath = tempDir.resolve("hybrid-database-repair-approvals.jsonl");
        InMemoryApprovalDataSource dataSource = new InMemoryApprovalDataSource();
        writeApproval(
                approvalPath,
                new HermesSkillLineageRepairApproval(
                        "approval-hybrid-fallback-001",
                        HermesSkillLineageRepairAdapter.APPLY,
                        "repair-key-hybrid-fallback-001",
                        "rustfs",
                        "object-storage",
                        true,
                        "operator-a",
                        "fallback approval",
                        Map.of()));
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairApprovalStore("hybrid")
                .skillLineageRepairApprovalPath(approvalPath.toString())
                .skillLineageRepairApprovalJdbcTableName("hermes_hybrid_approvals")
                .build();

        HermesSkillLineageRepairApprovalStore store =
                HermesSkillLineageRepairApprovalStoreResolver.resolve(
                        config,
                        Optional.empty(),
                        Optional.of(dataSource));
        DatabaseHermesSkillLineageRepairApprovalStore databaseStore =
                new DatabaseHermesSkillLineageRepairApprovalStore(
                        dataSource,
                        "hermes_hybrid_approvals",
                        false);
        databaseStore.save(new HermesSkillLineageRepairApproval(
                "approval-hybrid-db-001",
                HermesSkillLineageRepairAdapter.APPLY,
                "repair-key-hybrid-db-001",
                "database",
                "database",
                true,
                "operator-a",
                "database primary approval",
                Map.of()));

        assertThat(store.authorize(request(
                "approval-hybrid-db-001",
                "repair-key-hybrid-db-001",
                "database",
                "database")).approved())
                .isTrue();
        assertThat(store.authorize(request(
                "approval-hybrid-fallback-001",
                "repair-key-hybrid-fallback-001",
                "rustfs",
                "object-storage")).approved())
                .isTrue();
        assertThat(metadataMap(store.toMetadata(), "primaryStore"))
                .containsEntry("storeType", "database")
                .containsEntry("approvalTable", "hermes_hybrid_approvals");
        assertThat(metadataMap(store.toMetadata(), "fallbackStore"))
                .containsEntry("storeType", "file-system")
                .containsEntry("approvalPath", approvalPath.toString());
    }

    private static void writeApproval(Path path, HermesSkillLineageRepairApproval approval) throws Exception {
        Files.writeString(
                path,
                HermesRuntimeEventJsonCodec.toJsonLine(approval.toMetadata()) + System.lineSeparator(),
                StandardCharsets.UTF_8);
    }

    private static void putApproval(
            InMemoryHermesObjectStorageService storage,
            String key,
            HermesSkillLineageRepairApproval approval) {
        storage.putObject(
                        key,
                        HermesRuntimeEventJsonCodec.toJsonLine(approval.toMetadata())
                                .getBytes(StandardCharsets.UTF_8))
                .await()
                .indefinitely();
    }

    private static HermesSkillLineageRepairAdapterDispatchRequest request(
            String approvalId,
            String idempotencyKey,
            String backendId,
            String storageFamily) {
        HermesSkillLineageRepairOperationBatchSelection selection =
                HermesSkillLineageRepairOperationBatchSelection.from(
                        HermesSkillLineageRepairOperationBatchQuery.adapterReadyForBackend(backendId),
                        List.of(HermesSkillLineageRepairOperationBatch.from(
                                backendId,
                                storageFamily,
                                false,
                                true,
                                true,
                                List.of(new HermesSkillLineageRepairOperation(
                                        "",
                                        "intent-001",
                                        backendId,
                                        storageFamily,
                                        "restore-lineage-root-definition",
                                        "lineage-root",
                                        "hermes-root",
                                        true,
                                        true,
                                        true,
                                        false,
                                        "",
                                        "",
                                        Map.of())))));
        return new HermesSkillLineageRepairAdapterDispatchRequest(
                HermesSkillLineageRepairAdapter.APPLY,
                selection,
                true,
                approvalId,
                idempotencyKey,
                Map.of("source", "approval-resolver-test"));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }

    private record ApprovalRow(
            String approvalId,
            String action,
            String idempotencyKey,
            String backendId,
            String storageFamily,
            String active,
            String approvedBy,
            String reason,
            String metadata,
            String recordedAt) {
    }

    private static final class InMemoryApprovalDataSource extends AbstractHermesJdbcDataSource {
        private final List<ApprovalRow> rows = new ArrayList<>();

        @Override
        protected int executeUpdate(String sql, Map<Integer, Object> parameters) {
            if (sql.toUpperCase(Locale.ROOT).startsWith("INSERT")) {
                rows.add(new ApprovalRow(
                        (String) parameters.get(1),
                        (String) parameters.get(2),
                        (String) parameters.get(3),
                        (String) parameters.get(4),
                        (String) parameters.get(5),
                        (String) parameters.get(6),
                        (String) parameters.get(7),
                        (String) parameters.get(8),
                        (String) parameters.get(9),
                        (String) parameters.get(10)));
                return 1;
            }
            return 0;
        }

        @Override
        protected List<List<Object>> select(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (normalizedSql.contains("COUNT(*)")) {
                return List.of(List.of(rows.size()));
            }
            if (normalizedSql.contains("WHERE APPROVAL_ID = ?")) {
                String approvalId = (String) parameters.get(1);
                return rows.stream()
                        .filter(row -> row.approvalId().equals(approvalId))
                        .sorted(Comparator.comparing(ApprovalRow::recordedAt).reversed())
                        .map(row -> {
                            List<Object> values = List.of(
                                    row.approvalId(),
                                    row.action(),
                                    row.idempotencyKey(),
                                    row.backendId(),
                                    row.storageFamily(),
                                    row.active(),
                                    row.approvedBy(),
                                    row.reason(),
                                    row.metadata());
                            return values;
                        })
                        .toList();
            }
            return List.of();
        }
    }
}
