package tech.kayys.wayang.agent.skills.management;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC-backed lifecycle state store for database-primary deployments.
 */
public final class JdbcSkillLifecycleStateStore implements SkillLifecycleStateStore {

    public static final String DEFAULT_TABLE_NAME = "wayang_skill_lifecycle_states";

    private final DataSource dataSource;
    private final String tableName;

    public JdbcSkillLifecycleStateStore(DataSource dataSource) {
        this(dataSource, DEFAULT_TABLE_NAME, true);
    }

    public JdbcSkillLifecycleStateStore(DataSource dataSource, String tableName, boolean initializeSchema) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.tableName = JdbcSkillStoreTables.normalize(tableName, DEFAULT_TABLE_NAME, "skill lifecycle state");
        if (initializeSchema) {
            initializeSchema();
        }
    }

    @Override
    public Optional<SkillLifecycleState> get(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return Optional.empty();
        }
        String sql = "SELECT skill_id, status, created_at, updated_at, revision FROM "
                + tableName + " WHERE skill_id = ?";
        return JdbcSkillStoreSupport.queryOne(
                dataSource,
                sql,
                statement -> statement.setString(1, skillId),
                this::read,
                "Failed to load skill lifecycle state from JDBC store: " + skillId);
    }

    @Override
    public SkillLifecycleState save(SkillLifecycleState state) {
        Objects.requireNonNull(state, "state");
        String updateSql = "UPDATE " + tableName
                + " SET status = ?, created_at = ?, updated_at = ?, revision = ? WHERE skill_id = ?";
        String insertSql = "INSERT INTO " + tableName
                + " (skill_id, status, created_at, updated_at, revision) VALUES (?, ?, ?, ?, ?)";
        JdbcSkillStoreSupport.updateThenInsert(
                dataSource,
                updateSql,
                update -> {
                    update.setString(1, state.status().name());
                    update.setString(2, state.createdAt().toString());
                    update.setString(3, state.updatedAt().toString());
                    update.setInt(4, state.revision());
                    update.setString(5, state.skillId());
                },
                insertSql,
                insert -> {
                    insert.setString(1, state.skillId());
                    insert.setString(2, state.status().name());
                    insert.setString(3, state.createdAt().toString());
                    insert.setString(4, state.updatedAt().toString());
                    insert.setInt(5, state.revision());
                },
                "Failed to persist skill lifecycle state in JDBC store: " + state.skillId());
        return state;
    }

    @Override
    public boolean remove(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return false;
        }
        return JdbcSkillStoreSupport.deleteById(
                dataSource,
                tableName,
                "skill_id",
                skillId,
                "Failed to delete skill lifecycle state from JDBC store: " + skillId);
    }

    @Override
    public Map<String, SkillLifecycleState> snapshot() {
        String sql = "SELECT skill_id, status, created_at, updated_at, revision FROM " + tableName + " ORDER BY skill_id";
        List<SkillLifecycleState> rows = JdbcSkillStoreSupport.query(
                dataSource,
                sql,
                this::read,
                "Failed to list skill lifecycle states from JDBC store");
        Map<String, SkillLifecycleState> states = new LinkedHashMap<>();
        for (SkillLifecycleState state : rows) {
            states.put(state.skillId(), state);
        }
        return Map.copyOf(states);
    }

    private void initializeSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "skill_id VARCHAR(255) PRIMARY KEY, "
                + "status VARCHAR(64) NOT NULL, "
                + "created_at VARCHAR(64) NOT NULL, "
                + "updated_at VARCHAR(64) NOT NULL, "
                + "revision INTEGER NOT NULL"
                + ")";
        JdbcSkillStoreSupport.execute(
                dataSource,
                sql,
                "Failed to initialize JDBC skill lifecycle state table: " + tableName);
    }

    private SkillLifecycleState read(ResultSet resultSet) throws SQLException {
        return new SkillLifecycleState(
                resultSet.getString(1),
                SkillLifecycleStatus.valueOf(resultSet.getString(2)),
                Instant.parse(resultSet.getString(3)),
                Instant.parse(resultSet.getString(4)),
                resultSet.getInt(5));
    }
}
