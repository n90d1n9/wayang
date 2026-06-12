package tech.kayys.wayang.agent.hermes;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * JDBC-backed runtime event journal for database-primary Hermes deployments.
 */
public final class DatabaseHermesRuntimeEventSink implements HermesRuntimeEventSink, HermesRuntimeEventReader {

    public static final String DEFAULT_TABLE_NAME = "wayang_hermes_runtime_events";

    private final DataSource dataSource;
    private final String tableName;
    private final HermesRecordRetentionPolicy retentionPolicy;

    public DatabaseHermesRuntimeEventSink(DataSource dataSource) {
        this(dataSource, DEFAULT_TABLE_NAME, true, FileSystemHermesRuntimeEventSink.DEFAULT_MAX_EVENTS);
    }

    public DatabaseHermesRuntimeEventSink(
            DataSource dataSource,
            String tableName,
            boolean initializeSchema,
            int maxEvents) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.tableName = normalizeTableName(tableName);
        this.retentionPolicy = HermesRecordRetentionPolicy.bounded(maxEvents);
        if (initializeSchema) {
            initializeSchema();
        }
    }

    public String tableName() {
        return tableName;
    }

    public int maxEvents() {
        return retentionPolicy.maxEntries();
    }

    @Override
    public synchronized void emit(HermesRuntimeEvent event) {
        if (event == null) {
            return;
        }
        String occurredAt = event.occurredAt().toString();
        String sql = "INSERT INTO " + tableName
                + " (record_id, event_id, event_type, request_id, tenant_id, session_id, user_id, "
                + "outcome, occurred_at, event_json) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, occurredAt + "-" + UUID.randomUUID());
            statement.setString(2, event.eventId());
            statement.setString(3, event.type());
            statement.setString(4, event.requestId());
            statement.setString(5, event.tenantId());
            statement.setString(6, event.sessionId());
            statement.setString(7, event.userId());
            statement.setString(8, event.outcome());
            statement.setString(9, occurredAt);
            statement.setString(10, HermesRuntimeEventJsonCodec.toJsonLine(event));
            statement.executeUpdate();
            pruneToCapacity(connection);
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to persist Hermes runtime event in JDBC store", error);
        }
    }

    @Override
    public synchronized HermesRuntimeEventPage query(HermesRuntimeEventQuery query) {
        return HermesRuntimeEventPages.from(events(), query);
    }

    public synchronized List<HermesRuntimeEvent> events() {
        String sql = "SELECT event_json FROM " + tableName + " ORDER BY occurred_at ASC, record_id ASC";
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            List<HermesRuntimeEvent> events = new ArrayList<>();
            while (resultSet.next()) {
                events.add(HermesRuntimeEventJsonCodec.fromJsonLine(resultSet.getString(1)));
            }
            return List.copyOf(events);
        } catch (SQLException error) {
            throw new IllegalStateException("Failed to read Hermes runtime event JDBC journal", error);
        }
    }

    public synchronized int eventCount() {
        return HermesJdbcStoreSupport.countRows(
                dataSource,
                tableName,
                "Failed to count Hermes runtime events in JDBC store");
    }

    private void initializeSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "record_id VARCHAR(255) NOT NULL, "
                + "event_id VARCHAR(255) NOT NULL, "
                + "event_type VARCHAR(128) NOT NULL, "
                + "request_id VARCHAR(255) NOT NULL, "
                + "tenant_id VARCHAR(255) NOT NULL, "
                + "session_id VARCHAR(255) NOT NULL, "
                + "user_id VARCHAR(255) NOT NULL, "
                + "outcome VARCHAR(64) NOT NULL, "
                + "occurred_at VARCHAR(64) NOT NULL, "
                + "event_json TEXT NOT NULL, "
                + "PRIMARY KEY (record_id)"
                + ")";
        HermesJdbcStoreSupport.execute(
                dataSource,
                sql,
                "Failed to initialize Hermes runtime event JDBC table: " + tableName);
    }

    private void pruneToCapacity(Connection connection) throws SQLException {
        HermesJdbcStoreSupport.pruneRowsToCapacity(
                connection,
                tableName,
                "record_id",
                "occurred_at DESC, record_id DESC",
                retentionPolicy);
    }

    static String normalizeTableName(String tableName) {
        return HermesJdbcTableNames.normalize(
                tableName,
                DEFAULT_TABLE_NAME,
                "runtime event");
    }
}
