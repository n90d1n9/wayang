package tech.kayys.wayang.agent.skills.management;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * JDBC-backed skill-management event sink/reader for database-primary audit history.
 */
public final class JdbcSkillManagementEventStore
        implements SkillManagementEventSink, SkillManagementEventReader, SkillManagementEventPruner {

    public static final String DEFAULT_TABLE_NAME = "wayang_skill_management_events";

    private final DataSource dataSource;
    private final String tableName;
    private final int maxEvents;

    public JdbcSkillManagementEventStore(DataSource dataSource) {
        this(dataSource, DEFAULT_TABLE_NAME, true);
    }

    public JdbcSkillManagementEventStore(
            DataSource dataSource,
            String tableName,
            boolean initializeSchema) {
        this(dataSource, tableName, initializeSchema, InMemorySkillManagementEventSink.DEFAULT_MAX_EVENTS);
    }

    public JdbcSkillManagementEventStore(
            DataSource dataSource,
            String tableName,
            boolean initializeSchema,
            int maxEvents) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.tableName = JdbcSkillStoreTables.normalize(tableName, DEFAULT_TABLE_NAME, "skill-management event");
        this.maxEvents = SkillManagementEventRetention.normalizeCapacity(maxEvents);
        if (initializeSchema) {
            initializeSchema();
        }
    }

    @Override
    public synchronized void record(SkillManagementEvent event) {
        if (event == null) {
            return;
        }
        String sql = "INSERT INTO " + tableName
                + " (event_id, occurred_at, operation, skill_id, success, attributes) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        JdbcSkillStoreSupport.executeUpdate(
                dataSource,
                sql,
                statement -> {
                    statement.setString(1, SkillManagementEventReferences.sortableReference(event));
                    statement.setString(2, event.occurredAt().toString());
                    statement.setString(3, event.operation().name());
                    statement.setString(4, event.skillId());
                    statement.setString(5, Boolean.toString(event.success()));
                    statement.setString(6, SkillManagementEventPropertiesCodec.encodeAttributes(event.attributes()));
                },
                "Failed to persist skill-management event in JDBC store");
        pruneToCapacity();
    }

    @Override
    public synchronized SkillManagementEventPage query(SkillManagementEventQuery query) {
        return SkillManagementEventPages.from(events(), query);
    }

    public synchronized List<SkillManagementEvent> events() {
        String sql = "SELECT event_id, occurred_at, operation, skill_id, success, attributes FROM "
                + tableName + " ORDER BY event_id";
        return JdbcSkillStoreSupport.query(
                dataSource,
                sql,
                this::read,
                "Failed to list skill-management events from JDBC store");
    }

    @Override
    public synchronized SkillManagementEventPruneResult pruneEvents(SkillManagementEventPruneOptions options) {
        SkillManagementEventPruneOptions resolved =
                SkillManagementEventRetention.resolve(options, maxEvents);
        List<String> eventIds = eventIds();
        List<String> targets = SkillManagementEventRetention.oldestToPrune(eventIds, resolved.keepLatestEvents());
        if (!resolved.dryRun()) {
            targets.forEach(this::delete);
        }
        return SkillManagementEventPruneResult.success(resolved, eventIds.size(), targets);
    }

    private void initializeSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "event_id VARCHAR(96) PRIMARY KEY, "
                + "occurred_at VARCHAR(64) NOT NULL, "
                + "operation VARCHAR(64) NOT NULL, "
                + "skill_id VARCHAR(255) NOT NULL, "
                + "success VARCHAR(16) NOT NULL, "
                + "attributes TEXT NOT NULL"
                + ")";
        JdbcSkillStoreSupport.execute(
                dataSource,
                sql,
                "Failed to initialize JDBC skill-management event table: " + tableName);
    }

    private void pruneToCapacity() {
        SkillManagementEventRetention.oldestToPrune(eventIds(), maxEvents)
                .forEach(this::delete);
    }

    private List<String> eventIds() {
        String sql = "SELECT event_id FROM " + tableName + " ORDER BY event_id";
        return JdbcSkillStoreSupport.query(
                dataSource,
                sql,
                resultSet -> resultSet.getString(1),
                "Failed to list skill-management event ids from JDBC store");
    }

    private void delete(String eventId) {
        JdbcSkillStoreSupport.deleteById(
                dataSource,
                tableName,
                "event_id",
                eventId,
                "Failed to prune skill-management event from JDBC store: " + eventId);
    }

    private SkillManagementEvent read(ResultSet resultSet) throws SQLException {
        return new SkillManagementEvent(
                Instant.parse(resultSet.getString(2)),
                SkillManagementEventOperation.valueOf(resultSet.getString(3)),
                resultSet.getString(4),
                Boolean.parseBoolean(resultSet.getString(5)),
                SkillManagementEventPropertiesCodec.decodeAttributes(
                        resultSet.getString(6),
                        resultSet.getString(1)));
    }
}
