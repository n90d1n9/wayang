package tech.kayys.wayang.agent.hermes;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC-backed approval store for database-primary Hermes deployments.
 */
public final class DatabaseHermesSkillLineageRepairApprovalStore
        implements HermesSkillLineageRepairApprovalStore {

    public static final String DEFAULT_TABLE_NAME = "wayang_hermes_repair_approvals";

    private final DataSource dataSource;
    private final String tableName;

    public DatabaseHermesSkillLineageRepairApprovalStore(DataSource dataSource) {
        this(dataSource, DEFAULT_TABLE_NAME, true);
    }

    public DatabaseHermesSkillLineageRepairApprovalStore(
            DataSource dataSource,
            String tableName,
            boolean initializeSchema) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.tableName = normalizeTableName(tableName);
        if (initializeSchema) {
            initializeSchema();
        }
    }

    public String tableName() {
        return tableName;
    }

    @Override
    public synchronized Optional<HermesSkillLineageRepairApproval> find(String approvalId) {
        String id = approvalId == null ? "" : HermesText.oneLine(approvalId);
        if (id.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT approval_id, action, idempotency_key, backend_id, storage_family, "
                + "active, approved_by, reason, metadata "
                + "FROM " + tableName
                + " WHERE approval_id = ? ORDER BY recorded_at DESC";
        return HermesJdbcStoreSupport.queryFirst(
                dataSource,
                sql,
                statement -> statement.setString(1, id),
                DatabaseHermesSkillLineageRepairApprovalStore::read,
                "Failed to read Hermes repair approval from JDBC store");
    }

    public synchronized void save(HermesSkillLineageRepairApproval approval) {
        if (approval == null || approval.approvalId().isBlank()) {
            return;
        }
        String sql = "INSERT INTO " + tableName
                + " (approval_id, action, idempotency_key, backend_id, storage_family, "
                + "active, approved_by, reason, metadata, recorded_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, approval.approvalId());
            statement.setString(2, approval.action());
            statement.setString(3, approval.idempotencyKey());
            statement.setString(4, approval.backendId());
            statement.setString(5, approval.storageFamily());
            statement.setString(6, Boolean.toString(approval.active()));
            statement.setString(7, approval.approvedBy());
            statement.setString(8, approval.reason());
            statement.setString(9, HermesJdbcStoreSupport.jsonLine(approval.metadata()));
            statement.setString(10, Instant.now().toString());
            statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to persist Hermes repair approval in JDBC store", error);
        }
    }

    public synchronized int approvalCount() {
        return HermesJdbcStoreSupport.countRows(
                dataSource,
                tableName,
                "Failed to count Hermes repair approvals in JDBC store");
    }

    @Override
    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("storeType", "database");
        metadata.put("configured", true);
        metadata.put("approvalTable", tableName);
        metadata.put("approvalCount", approvalCount());
        return Map.copyOf(metadata);
    }

    private void initializeSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "approval_id VARCHAR(255) NOT NULL, "
                + "action VARCHAR(64) NOT NULL, "
                + "idempotency_key VARCHAR(255) NOT NULL, "
                + "backend_id VARCHAR(128) NOT NULL, "
                + "storage_family VARCHAR(128) NOT NULL, "
                + "active VARCHAR(16) NOT NULL, "
                + "approved_by VARCHAR(255) NOT NULL, "
                + "reason TEXT NOT NULL, "
                + "metadata TEXT NOT NULL, "
                + "recorded_at VARCHAR(64) NOT NULL"
                + ")";
        HermesJdbcStoreSupport.execute(
                dataSource,
                sql,
                "Failed to initialize Hermes repair approval JDBC table: " + tableName);
    }

    private static HermesSkillLineageRepairApproval read(ResultSet resultSet) throws SQLException {
        return HermesSkillLineageRepairApproval.fromMetadata(Map.of(
                "approvalId", resultSet.getString(1),
                "action", resultSet.getString(2),
                "idempotencyKey", resultSet.getString(3),
                "backendId", resultSet.getString(4),
                "storageFamily", resultSet.getString(5),
                "active", resultSet.getString(6),
                "approvedBy", resultSet.getString(7),
                "reason", resultSet.getString(8),
                "metadata", HermesJdbcStoreSupport.jsonObject(resultSet.getString(9))));
    }

    static String normalizeTableName(String tableName) {
        return HermesJdbcTableNames.normalize(
                tableName,
                DEFAULT_TABLE_NAME,
                "repair approval");
    }
}
