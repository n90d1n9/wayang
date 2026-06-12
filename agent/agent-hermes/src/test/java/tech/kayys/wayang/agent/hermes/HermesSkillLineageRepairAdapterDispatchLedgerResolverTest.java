package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

class HermesSkillLineageRepairAdapterDispatchLedgerResolverTest {

    @Test
    void defaultsToNoopLedger() {
        HermesSkillLineageRepairAdapterDispatchLedger ledger =
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(null);

        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "noop")
                .containsEntry("recordCount", 0)
                .containsEntry("replaySupported", false);
        assertThat(HermesSkillLineageRepairAdapterDispatchLedgerResolver.metadata(null))
                .containsEntry("ledgerStore", "noop")
                .containsEntry("durable", false)
                .containsEntry("replaySupported", false);
    }

    @Test
    void resolvesInMemoryLedger() {
        HermesSkillLineageRepairAdapterDispatchLedger ledger =
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(HermesAgentModeConfig.builder()
                        .skillLineageRepairDispatchLedgerStore("in_memory")
                        .build());

        ledger.record(request("in-memory-key-001"), dispatch("in-memory"));

        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "in-memory")
                .containsEntry("recordCount", 1)
                .containsEntry("replaySupported", true);
        assertThat(ledger.find("in-memory-key-001")).isPresent();
    }

    @Test
    void resolvesFileSystemLedgerFromConfig(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("repair-dispatch-ledger.jsonl");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerStore("file-system")
                .skillLineageRepairDispatchLedgerPath(ledgerPath.toString())
                .skillLineageRepairDispatchLedgerMaxRecords(2)
                .build();
        HermesSkillLineageRepairAdapterDispatchLedger ledger =
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(config);

        ledger.record(request("file-key-001"), dispatch("file-system"));

        HermesSkillLineageRepairAdapterDispatchLedger replayLedger =
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(config);

        assertThat(ledger)
                .isInstanceOf(FileSystemHermesSkillLineageRepairAdapterDispatchLedger.class);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "file-system")
                .containsEntry("ledgerPath", ledgerPath.toString())
                .containsEntry("maxRecords", 2)
                .containsEntry("replaySupported", true);
        assertThat(Files.exists(ledgerPath)).isTrue();
        assertThat(replayLedger.find("file-key-001")).isPresent();
        assertThat(HermesSkillLineageRepairAdapterDispatchLedgerResolver.metadata(config))
                .containsEntry("ledgerStore", "file-system")
                .containsEntry("ledgerPath", ledgerPath.toString())
                .containsEntry("maxRecords", 2)
                .containsEntry("durable", true)
                .containsEntry("fileFallback", true)
                .containsEntry("replaySupported", true);
    }

    @Test
    void resolvesObjectStorageLedgerFromConfig() {
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerStore("object-storage")
                .skillLineageRepairDispatchLedgerObjectPrefix("/tenant-a/hermes/repair-dispatch")
                .skillLineageRepairDispatchLedgerMaxRecords(2)
                .build();
        HermesSkillLineageRepairAdapterDispatchLedger ledger =
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(config, Optional.of(storage));

        ledger.record(request("object-key-001"), dispatch("object-storage-1"));
        ledger.record(request("object-key-002"), dispatch("object-storage-2"));
        ledger.record(request("object-key-003"), dispatch("object-storage-3"));

        assertThat(ledger)
                .isInstanceOf(ObjectStorageHermesSkillLineageRepairAdapterDispatchLedger.class);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "object-storage")
                .containsEntry("ledgerPrefix", "tenant-a/hermes/repair-dispatch/")
                .containsEntry("recordCount", 2)
                .containsEntry("maxRecords", 2)
                .containsEntry("replaySupported", true);
        assertThat(storage.objects)
                .hasSize(2)
                .allSatisfy((key, value) -> assertThat(key)
                        .startsWith("tenant-a/hermes/repair-dispatch/")
                        .endsWith(".jsonl"));
        assertThat(ledger.find("object-key-001")).isEmpty();
        assertThat(ledger.find("object-key-003")).isPresent();
        assertThat(HermesSkillLineageRepairAdapterDispatchLedgerResolver.metadata(config))
                .containsEntry("ledgerStore", "object-storage")
                .containsEntry("ledgerObjectPrefix", "/tenant-a/hermes/repair-dispatch")
                .containsEntry("durable", true)
                .containsEntry("fileFallback", false);
    }

    @Test
    void objectStorageLedgerRequiresObjectStorageService() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerStore("object-storage")
                .build();

        assertThatThrownBy(() -> HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ObjectStorageService");
    }

    @Test
    void resolvesDatabaseLedgerFromConfig() {
        InMemoryDispatchLedgerDataSource dataSource = new InMemoryDispatchLedgerDataSource();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerStore("database")
                .skillLineageRepairDispatchLedgerJdbcTableName("hermes_dispatch_ledger")
                .skillLineageRepairDispatchLedgerJdbcInitializeSchema(true)
                .skillLineageRepairDispatchLedgerMaxRecords(2)
                .build();
        HermesSkillLineageRepairAdapterDispatchLedger ledger =
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(
                        config,
                        Optional.empty(),
                        Optional.of(dataSource));

        ledger.record(request("database-key-001"), dispatch("database-1"));
        ledger.record(request("database-key-002"), dispatch("database-2"));
        ledger.record(request("database-key-003"), dispatch("database-3"));

        assertThat(ledger)
                .isInstanceOf(DatabaseHermesSkillLineageRepairAdapterDispatchLedger.class);
        assertThat(ledger.toMetadata())
                .containsEntry("ledgerType", "database")
                .containsEntry("ledgerTable", "hermes_dispatch_ledger")
                .containsEntry("recordCount", 2)
                .containsEntry("maxRecords", 2)
                .containsEntry("replaySupported", true);
        assertThat(ledger.find("database-key-001")).isEmpty();
        assertThat(ledger.find("database-key-003"))
                .isPresent()
                .get()
                .extracting(dispatch -> dispatch.metadata().get("source"))
                .isEqualTo("database-3");
        assertThat(HermesSkillLineageRepairAdapterDispatchLedgerResolver.metadata(config))
                .containsEntry("ledgerStore", "database")
                .containsEntry("ledgerJdbcTableName", "hermes_dispatch_ledger")
                .containsEntry("ledgerJdbcInitializeSchema", true)
                .containsEntry("databaseCapable", true)
                .containsEntry("fileFallback", false);
    }

    @Test
    void databaseLedgerRequiresDataSource() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerStore("database")
                .build();

        assertThatThrownBy(() -> HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(config))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DataSource");
    }

    @Test
    void resolvesHybridLedgerWithFileFallback(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("hybrid-repair-dispatch-ledger.jsonl");
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerStore("hybrid")
                .skillLineageRepairDispatchLedgerPath(ledgerPath.toString())
                .skillLineageRepairDispatchLedgerObjectPrefix("tenant-a/hermes/hybrid-repair-dispatch")
                .skillLineageRepairDispatchLedgerMaxRecords(4)
                .build();
        HermesSkillLineageRepairAdapterDispatchLedger ledger =
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(config, Optional.of(storage));

        ledger.record(request("hybrid-key-001"), dispatch("hybrid"));

        HermesSkillLineageRepairAdapterDispatchLedger fileFallbackLedger =
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(config, Optional.empty());

        assertThat(ledger)
                .isInstanceOf(HybridHermesSkillLineageRepairAdapterDispatchLedger.class);
        assertThat(metadataMap(ledger.toMetadata(), "primaryLedger"))
                .containsEntry("ledgerType", "object-storage")
                .containsEntry("ledgerPrefix", "tenant-a/hermes/hybrid-repair-dispatch/");
        assertThat(metadataMap(ledger.toMetadata(), "fallbackLedger"))
                .containsEntry("ledgerType", "file-system")
                .containsEntry("ledgerPath", ledgerPath.toString());
        assertThat(storage.objects).hasSize(1);
        assertThat(Files.exists(ledgerPath)).isTrue();
        assertThat(fileFallbackLedger.find("hybrid-key-001")).isPresent();
        assertThat(HermesSkillLineageRepairAdapterDispatchLedgerResolver.metadata(config))
                .containsEntry("ledgerStore", "hybrid")
                .containsEntry("durable", true)
                .containsEntry("fileFallback", true)
                .containsEntry("replaySupported", true);
    }

    @Test
    void resolvesHybridLedgerWithDatabasePrimary(@TempDir Path tempDir) {
        Path ledgerPath = tempDir.resolve("hybrid-database-repair-dispatch-ledger.jsonl");
        InMemoryDispatchLedgerDataSource dataSource = new InMemoryDispatchLedgerDataSource();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .skillLineageRepairDispatchLedgerStore("hybrid")
                .skillLineageRepairDispatchLedgerPath(ledgerPath.toString())
                .skillLineageRepairDispatchLedgerJdbcTableName("hermes_hybrid_dispatch_ledger")
                .skillLineageRepairDispatchLedgerMaxRecords(4)
                .build();
        HermesSkillLineageRepairAdapterDispatchLedger ledger =
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(
                        config,
                        Optional.empty(),
                        Optional.of(dataSource));

        ledger.record(request("hybrid-database-key-001"), dispatch("hybrid-database"));

        HermesSkillLineageRepairAdapterDispatchLedger fileFallbackLedger =
                HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(
                        config,
                        Optional.empty(),
                        Optional.empty());

        assertThat(ledger)
                .isInstanceOf(HybridHermesSkillLineageRepairAdapterDispatchLedger.class);
        assertThat(metadataMap(ledger.toMetadata(), "primaryLedger"))
                .containsEntry("ledgerType", "database")
                .containsEntry("ledgerTable", "hermes_hybrid_dispatch_ledger");
        assertThat(metadataMap(ledger.toMetadata(), "fallbackLedger"))
                .containsEntry("ledgerType", "file-system")
                .containsEntry("ledgerPath", ledgerPath.toString());
        assertThat(ledger.find("hybrid-database-key-001")).isPresent();
        assertThat(fileFallbackLedger.find("hybrid-database-key-001")).isPresent();
        assertThat(HermesSkillLineageRepairAdapterDispatchLedgerResolver.metadata(config))
                .containsEntry("ledgerStore", "hybrid")
                .containsEntry("databaseCapable", true)
                .containsEntry("fileFallback", true);
    }

    private static HermesSkillLineageRepairAdapterDispatchRequest request(String idempotencyKey) {
        return new HermesSkillLineageRepairAdapterDispatchRequest(
                HermesSkillLineageRepairAdapter.APPLY,
                null,
                true,
                "approval-001",
                idempotencyKey,
                Map.of());
    }

    private static HermesSkillLineageRepairAdapterDispatch dispatch(String source) {
        return HermesSkillLineageRepairAdapterDispatch.from(
                HermesSkillLineageRepairAdapter.APPLY,
                List.of(new HermesSkillLineageRepairAdapterResult(
                        source + "-adapter",
                        HermesSkillLineageRepairAdapter.APPLY,
                        "batch-001",
                        "database",
                        "database",
                        1,
                        true,
                        true,
                        true,
                        true,
                        "applied",
                        "ok",
                        Map.of("source", source))),
                Map.of("source", source));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> metadataMap(Map<String, Object> metadata, String key) {
        return (Map<String, Object>) metadata.get(key);
    }

    private record DispatchLedgerRow(
            long sequence,
            String recordId,
            String idempotencyKey,
            String action,
            String approvalId,
            String recordMetadata,
            String recordedAt) {
    }

    private static final class InMemoryDispatchLedgerDataSource extends AbstractHermesJdbcDataSource {
        private final List<DispatchLedgerRow> rows = new ArrayList<>();
        private long sequence;

        @Override
        protected int executeUpdate(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (normalizedSql.startsWith("INSERT")) {
                rows.add(new DispatchLedgerRow(
                        ++sequence,
                        (String) parameters.get(1),
                        (String) parameters.get(2),
                        (String) parameters.get(3),
                        (String) parameters.get(4),
                        (String) parameters.get(5),
                        (String) parameters.get(6)));
                return 1;
            }
            if (normalizedSql.startsWith("DELETE")) {
                String recordId = (String) parameters.get(1);
                int before = rows.size();
                rows.removeIf(row -> row.recordId().equals(recordId));
                return before - rows.size();
            }
            return 0;
        }

        @Override
        protected List<List<Object>> select(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (normalizedSql.contains("COUNT(*)")) {
                return List.of(List.of(rows.size()));
            }
            if (normalizedSql.contains("WHERE IDEMPOTENCY_KEY = ?")) {
                String idempotencyKey = (String) parameters.get(1);
                return rows.stream()
                        .filter(row -> row.idempotencyKey().equals(idempotencyKey))
                        .sorted(dispatchLedgerOrdering())
                        .map(row -> List.<Object>of(row.recordMetadata()))
                        .toList();
            }
            if (normalizedSql.startsWith("SELECT RECORD_ID")) {
                return rows.stream()
                        .sorted(dispatchLedgerOrdering())
                        .map(row -> List.<Object>of(row.recordId()))
                        .toList();
            }
            return List.of();
        }

        private Comparator<DispatchLedgerRow> dispatchLedgerOrdering() {
            return Comparator
                    .comparing(DispatchLedgerRow::recordedAt)
                    .thenComparing(DispatchLedgerRow::sequence)
                    .reversed();
        }
    }
}
