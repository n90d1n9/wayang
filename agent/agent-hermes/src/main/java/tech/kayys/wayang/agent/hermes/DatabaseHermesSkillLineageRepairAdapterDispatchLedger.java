package tech.kayys.wayang.agent.hermes;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-backed idempotency ledger for repair adapter dispatches.
 */
public final class DatabaseHermesSkillLineageRepairAdapterDispatchLedger
        implements HermesSkillLineageRepairAdapterDispatchLedger {

    public static final String DEFAULT_TABLE_NAME = "wayang_hermes_repair_dispatch_ledger";

    private final DataSource dataSource;
    private final String tableName;
    private final HermesRecordRetentionPolicy retentionPolicy;

    public DatabaseHermesSkillLineageRepairAdapterDispatchLedger(DataSource dataSource) {
        this(
                dataSource,
                DEFAULT_TABLE_NAME,
                true,
                FileSystemHermesSkillLineageRepairAdapterDispatchLedger.DEFAULT_MAX_RECORDS);
    }

    public DatabaseHermesSkillLineageRepairAdapterDispatchLedger(
            DataSource dataSource,
            String tableName,
            boolean initializeSchema,
            int maxRecords) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.tableName = normalizeTableName(tableName);
        this.retentionPolicy = HermesRecordRetentionPolicy.bounded(maxRecords);
        if (initializeSchema) {
            initializeSchema();
        }
    }

    public String tableName() {
        return tableName;
    }

    public int maxRecords() {
        return retentionPolicy.maxEntries();
    }

    @Override
    public synchronized Optional<HermesSkillLineageRepairAdapterDispatch> find(String idempotencyKey) {
        String key = HermesSkillLineageRepairAdapterDispatchLedgerRecords.key(idempotencyKey);
        if (key.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT record_metadata FROM " + tableName
                + " WHERE idempotency_key = ? ORDER BY recorded_at DESC, record_id DESC";
        return HermesJdbcStoreSupport.queryFirst(
                dataSource,
                sql,
                statement -> statement.setString(1, key),
                resultSet -> dispatchFromMetadata(resultSet.getString(1)),
                "Failed to read Hermes repair dispatch ledger from JDBC store");
    }

    @Override
    public synchronized HermesSkillLineageRepairAdapterDispatch record(
            HermesSkillLineageRepairAdapterDispatchRequest request,
            HermesSkillLineageRepairAdapterDispatch dispatch) {
        if (request == null || dispatch == null || request.idempotencyKey().isBlank()) {
            return dispatch;
        }
        Optional<HermesSkillLineageRepairAdapterDispatch> existing = find(request.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        String recordedAt = Instant.now().toString();
        String sql = "INSERT INTO " + tableName
                + " (record_id, idempotency_key, action, approval_id, record_metadata, recorded_at) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, recordedAt + "-" + UUID.randomUUID());
            statement.setString(2, HermesSkillLineageRepairAdapterDispatchLedgerRecords.key(
                    request.idempotencyKey()));
            statement.setString(3, request.action());
            statement.setString(4, request.approvalId());
            statement.setString(5, HermesJdbcStoreSupport.jsonLine(
                    HermesSkillLineageRepairAdapterDispatchLedgerRecords.recordMetadata(request, dispatch)));
            statement.setString(6, recordedAt);
            statement.executeUpdate();
            pruneToCapacity(connection);
            return dispatch;
        } catch (SQLException error) {
            Optional<HermesSkillLineageRepairAdapterDispatch> replay = find(request.idempotencyKey());
            if (replay.isPresent()) {
                return replay.get();
            }
            throw new IllegalStateException("Failed to persist Hermes repair dispatch ledger in JDBC store", error);
        }
    }

    @Override
    public synchronized int recordCount() {
        return HermesJdbcStoreSupport.countRows(
                dataSource,
                tableName,
                "Failed to count Hermes repair dispatch ledger records");
    }

    @Override
    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("ledgerType", "database");
        metadata.put("ledgerTable", tableName);
        metadata.put("recordCount", recordCount());
        metadata.put("maxRecords", maxRecords());
        metadata.put("retentionPolicy", retentionPolicy.toMetadata());
        metadata.put("replaySupported", true);
        return Map.copyOf(metadata);
    }

    private void initializeSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "record_id VARCHAR(255) NOT NULL, "
                + "idempotency_key VARCHAR(255) NOT NULL, "
                + "action VARCHAR(64) NOT NULL, "
                + "approval_id VARCHAR(255) NOT NULL, "
                + "record_metadata TEXT NOT NULL, "
                + "recorded_at VARCHAR(64) NOT NULL, "
                + "PRIMARY KEY (record_id), "
                + "UNIQUE (idempotency_key)"
                + ")";
        HermesJdbcStoreSupport.execute(
                dataSource,
                sql,
                "Failed to initialize Hermes repair dispatch ledger JDBC table: " + tableName);
    }

    private void pruneToCapacity(Connection connection) throws SQLException {
        HermesJdbcStoreSupport.pruneRowsToCapacity(
                connection,
                tableName,
                "record_id",
                "recorded_at DESC, record_id DESC",
                retentionPolicy);
    }

    private static HermesSkillLineageRepairAdapterDispatch dispatchFromMetadata(String value) {
        if (value == null || value.isBlank()) {
            return HermesSkillLineageRepairAdapterDispatchLedgerRecords.dispatch(Map.of());
        }
        Map<String, Object> record = HermesJdbcStoreSupport.jsonObject(value);
        return HermesSkillLineageRepairAdapterDispatchLedgerRecords.dispatch(record.get("dispatch"));
    }

    static String normalizeTableName(String tableName) {
        return HermesJdbcTableNames.normalize(
                tableName,
                DEFAULT_TABLE_NAME,
                "repair dispatch ledger");
    }
}
