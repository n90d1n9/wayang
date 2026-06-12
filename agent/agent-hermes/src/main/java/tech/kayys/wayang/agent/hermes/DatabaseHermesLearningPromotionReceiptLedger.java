package tech.kayys.wayang.agent.hermes;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * JDBC-backed idempotency ledger for learned-skill promotion receipts.
 */
public final class DatabaseHermesLearningPromotionReceiptLedger
        implements HermesLearningPromotionReceiptLedger {

    public static final String DEFAULT_TABLE_NAME = "wayang_hermes_learning_promotion_receipts";

    private final DataSource dataSource;
    private final String tableName;
    private final HermesRecordRetentionPolicy retentionPolicy;

    public DatabaseHermesLearningPromotionReceiptLedger(DataSource dataSource) {
        this(
                dataSource,
                DEFAULT_TABLE_NAME,
                true,
                FileSystemHermesLearningPromotionReceiptLedger.DEFAULT_MAX_RECORDS);
    }

    public DatabaseHermesLearningPromotionReceiptLedger(
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
    public synchronized Optional<HermesLearningPromotionReceipt> find(String idempotencyKey) {
        String key = HermesLearningPromotionReceiptLedgerRecords.key(idempotencyKey);
        if (key.isBlank()) {
            return Optional.empty();
        }
        String sql = "SELECT record_metadata FROM " + tableName
                + " WHERE idempotency_key = ? ORDER BY recorded_at DESC, record_id DESC";
        return HermesJdbcStoreSupport.queryFirst(
                dataSource,
                sql,
                statement -> statement.setString(1, key),
                resultSet -> receiptFromMetadata(resultSet.getString(1)),
                "Failed to read Hermes learning promotion receipt ledger from JDBC store");
    }

    @Override
    public synchronized HermesLearningPromotionReceipt record(HermesLearningPromotionReceipt receipt) {
        if (receipt == null || receipt.idempotencyKey().isBlank()) {
            return receipt;
        }
        Optional<HermesLearningPromotionReceipt> existing = find(receipt.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        String recordedAt = Instant.now().toString();
        String sql = "INSERT INTO " + tableName
                + " (record_id, idempotency_key, promotion_id, skill_id, status, outcome, "
                + "record_metadata, recorded_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, recordedAt + "-" + UUID.randomUUID());
            statement.setString(2, HermesLearningPromotionReceiptLedgerRecords.key(receipt.idempotencyKey()));
            statement.setString(3, receipt.promotionId());
            statement.setString(4, receipt.skillId());
            statement.setString(5, receipt.status());
            statement.setString(6, receipt.outcome());
            statement.setString(7, HermesJdbcStoreSupport.jsonLine(
                    HermesLearningPromotionReceiptLedgerRecords.recordMetadata(receipt)));
            statement.setString(8, recordedAt);
            statement.executeUpdate();
            pruneToCapacity(connection);
            return receipt;
        } catch (SQLException error) {
            Optional<HermesLearningPromotionReceipt> replay = find(receipt.idempotencyKey());
            if (replay.isPresent()) {
                return replay.get();
            }
            throw new IllegalStateException(
                    "Failed to persist Hermes learning promotion receipt ledger in JDBC store",
                    error);
        }
    }

    @Override
    public synchronized int recordCount() {
        return HermesJdbcStoreSupport.countRows(
                dataSource,
                tableName,
                "Failed to count Hermes learning promotion receipt ledger records");
    }

    @Override
    public synchronized HermesLearningPromotionReceiptPage query(HermesLearningPromotionReceiptQuery query) {
        return HermesLearningPromotionReceiptPage.fromEntries(entries(), query);
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

    public synchronized List<HermesLearningPromotionReceiptLedgerEntry> entries() {
        String sql = "SELECT record_metadata FROM " + tableName + " ORDER BY recorded_at DESC, record_id DESC";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            List<HermesLearningPromotionReceiptLedgerEntry> entries = new ArrayList<>();
            while (resultSet.next()) {
                entryFromMetadata(resultSet.getString(1)).ifPresent(entries::add);
            }
            return List.copyOf(entries);
        } catch (SQLException error) {
            throw new IllegalStateException(
                    "Failed to read Hermes learning promotion receipt ledger entries from JDBC store",
                    error);
        }
    }

    private void initializeSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "record_id VARCHAR(255) NOT NULL, "
                + "idempotency_key VARCHAR(255) NOT NULL, "
                + "promotion_id VARCHAR(255) NOT NULL, "
                + "skill_id VARCHAR(255) NOT NULL, "
                + "status VARCHAR(64) NOT NULL, "
                + "outcome VARCHAR(64) NOT NULL, "
                + "record_metadata TEXT NOT NULL, "
                + "recorded_at VARCHAR(64) NOT NULL, "
                + "PRIMARY KEY (record_id), "
                + "UNIQUE (idempotency_key)"
                + ")";
        HermesJdbcStoreSupport.execute(
                dataSource,
                sql,
                "Failed to initialize Hermes learning promotion receipt ledger JDBC table: " + tableName);
    }

    private void pruneToCapacity(Connection connection) throws SQLException {
        HermesJdbcStoreSupport.pruneRowsToCapacity(
                connection,
                tableName,
                "record_id",
                "recorded_at DESC, record_id DESC",
                retentionPolicy);
    }

    private static HermesLearningPromotionReceipt receiptFromMetadata(String value) {
        return entryFromMetadata(value)
                .map(HermesLearningPromotionReceiptLedgerEntry::receipt)
                .orElseGet(() -> HermesLearningPromotionReceiptLedgerRecords.receipt(Map.of()));
    }

    private static Optional<HermesLearningPromotionReceiptLedgerEntry> entryFromMetadata(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> record = HermesJdbcStoreSupport.jsonObject(value);
        return HermesLearningPromotionReceiptLedgerEntry.fromRecord(record);
    }

    static String normalizeTableName(String tableName) {
        return HermesJdbcTableNames.normalize(
                tableName,
                DEFAULT_TABLE_NAME,
                "learning promotion receipt ledger");
    }
}
