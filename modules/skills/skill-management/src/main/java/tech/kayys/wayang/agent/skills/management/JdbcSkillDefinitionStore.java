package tech.kayys.wayang.agent.skills.management;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC-backed skill definition store for database-primary deployments.
 */
public final class JdbcSkillDefinitionStore implements SkillDefinitionStore {

    public static final String DEFAULT_TABLE_NAME = "wayang_skill_definitions";

    private final DataSource dataSource;
    private final String tableName;
    private final SkillDefinitionPropertiesCodec codec;

    public JdbcSkillDefinitionStore(DataSource dataSource) {
        this(dataSource, DEFAULT_TABLE_NAME, true);
    }

    public JdbcSkillDefinitionStore(DataSource dataSource, String tableName, boolean initializeSchema) {
        this(dataSource, tableName, initializeSchema, new SkillDefinitionPropertiesCodec());
    }

    JdbcSkillDefinitionStore(
            DataSource dataSource,
            String tableName,
            boolean initializeSchema,
            SkillDefinitionPropertiesCodec codec) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.tableName = normalizeTableName(tableName);
        this.codec = Objects.requireNonNull(codec, "codec");
        if (initializeSchema) {
            initializeSchema();
        }
    }

    @Override
    public Optional<SkillDefinition> getSkill(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return Optional.empty();
        }
        String sql = "SELECT content FROM " + tableName + " WHERE skill_id = ?";
        return JdbcSkillStoreSupport.queryOne(
                dataSource,
                sql,
                statement -> statement.setString(1, skillId),
                resultSet -> read(resultSet.getString(1), skillId),
                "Failed to load skill definition from JDBC store: " + skillId);
    }

    @Override
    public List<SkillDefinition> listSkills() {
        String sql = "SELECT content FROM " + tableName + " ORDER BY skill_id";
        return JdbcSkillStoreSupport.query(
                dataSource,
                sql,
                resultSet -> read(resultSet.getString(1), "JDBC row"),
                "Failed to list skill definitions from JDBC store");
    }

    @Override
    public void registerSkill(SkillDefinition skill) {
        Objects.requireNonNull(skill, "skill");
        String content = content(skill);
        Timestamp now = Timestamp.from(Instant.now());
        String updateSql = "UPDATE " + tableName + " SET content = ?, updated_at = ? WHERE skill_id = ?";
        String insertSql = "INSERT INTO " + tableName + " (skill_id, content, updated_at) VALUES (?, ?, ?)";
        JdbcSkillStoreSupport.updateThenInsert(
                dataSource,
                updateSql,
                update -> {
                    update.setString(1, content);
                    update.setTimestamp(2, now);
                    update.setString(3, skill.id());
                },
                insertSql,
                insert -> {
                    insert.setString(1, skill.id());
                    insert.setString(2, content);
                    insert.setTimestamp(3, now);
                },
                "Failed to persist skill definition in JDBC store: " + skill.id());
    }

    @Override
    public boolean unregisterSkill(String skillId) {
        if (SkillManagementSkillIds.isBlank(skillId)) {
            return false;
        }
        return JdbcSkillStoreSupport.deleteById(
                dataSource,
                tableName,
                "skill_id",
                skillId,
                "Failed to delete skill definition from JDBC store: " + skillId);
    }

    private void initializeSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "skill_id VARCHAR(255) PRIMARY KEY, "
                + "content TEXT NOT NULL, "
                + "updated_at TIMESTAMP NOT NULL"
                + ")";
        JdbcSkillStoreSupport.execute(
                dataSource,
                sql,
                "Failed to initialize JDBC skill definition table: " + tableName);
    }

    private SkillDefinition read(String content, String sourceDescription) {
        return codec.fromBytes(SkillManagementPropertiesCodecSupport.toUtf8Bytes(content), sourceDescription);
    }

    private String content(SkillDefinition skill) {
        return SkillManagementPropertiesCodecSupport.fromUtf8Bytes(codec.toBytes(skill));
    }

    private static String normalizeTableName(String tableName) {
        return JdbcSkillStoreTables.normalize(tableName, DEFAULT_TABLE_NAME, "skill definition");
    }
}
