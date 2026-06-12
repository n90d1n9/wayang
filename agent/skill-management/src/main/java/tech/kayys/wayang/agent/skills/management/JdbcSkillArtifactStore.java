package tech.kayys.wayang.agent.skills.management;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * JDBC-backed artifact store for database-primary dynamic skill deployments.
 */
public final class JdbcSkillArtifactStore implements SkillArtifactStore {

    public static final String DEFAULT_TABLE_NAME = "wayang_skill_artifacts";

    private final DataSource dataSource;
    private final String tableName;
    private final SkillArtifactManifestCodec manifestCodec;

    public JdbcSkillArtifactStore(DataSource dataSource) {
        this(dataSource, DEFAULT_TABLE_NAME, true);
    }

    public JdbcSkillArtifactStore(DataSource dataSource, String tableName, boolean initializeSchema) {
        this(dataSource, tableName, initializeSchema, new SkillArtifactManifestCodec());
    }

    JdbcSkillArtifactStore(
            DataSource dataSource,
            String tableName,
            boolean initializeSchema,
            SkillArtifactManifestCodec manifestCodec) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.tableName = JdbcSkillStoreTables.normalize(tableName, DEFAULT_TABLE_NAME, "skill artifact");
        this.manifestCodec = Objects.requireNonNull(manifestCodec, "manifestCodec");
        if (initializeSchema) {
            initializeSchema();
        }
    }

    @Override
    public Optional<SkillArtifact> getArtifact(SkillArtifactReference reference) {
        SkillArtifactReference resolved = requireReference(reference);
        String sql = "SELECT manifest, content_base64 FROM " + tableName
                + " WHERE skill_id = ? AND artifact_kind = ? AND artifact_name = ? AND artifact_version = ?";
        return JdbcSkillStoreSupport.queryOne(
                dataSource,
                sql,
                statement -> bindReference(statement, resolved, 1),
                resultSet -> readArtifact(resultSet, resolved.qualifiedName()),
                "Failed to load skill artifact from JDBC store: " + resolved.qualifiedName());
    }

    @Override
    public List<SkillArtifactReference> listArtifacts(SkillArtifactQuery query) {
        SkillArtifactQuery resolved = query == null ? SkillArtifactQuery.all() : query;
        ListQuery listQuery = listQuery(resolved);
        return JdbcSkillStoreSupport.query(
                        dataSource,
                        listQuery.sql(),
                        listQuery::bind,
                        this::readReference,
                        "Failed to list skill artifacts from JDBC store")
                .stream()
                .filter(resolved::matches)
                .limit(resolved.limit())
                .toList();
    }

    @Override
    public void putArtifact(SkillArtifact artifact) {
        Objects.requireNonNull(artifact, "artifact");
        String contentBase64 = Base64.getEncoder().encodeToString(artifact.content());
        String manifest = SkillManagementPropertiesCodecSupport.fromUtf8Bytes(manifestCodec.toBytes(artifact));
        Timestamp now = Timestamp.from(Instant.now());
        String updateSql = "UPDATE " + tableName
                + " SET content_base64 = ?, manifest = ?, updated_at = ?"
                + " WHERE skill_id = ? AND artifact_kind = ? AND artifact_name = ? AND artifact_version = ?";
        String insertSql = "INSERT INTO " + tableName
                + " (skill_id, artifact_kind, artifact_name, artifact_version, content_base64, manifest, updated_at)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)";
        JdbcSkillStoreSupport.updateThenInsert(
                dataSource,
                updateSql,
                update -> {
                    update.setString(1, contentBase64);
                    update.setString(2, manifest);
                    update.setTimestamp(3, now);
                    bindReference(update, artifact.reference(), 4);
                },
                insertSql,
                insert -> {
                    bindReference(insert, artifact.reference(), 1);
                    insert.setString(5, contentBase64);
                    insert.setString(6, manifest);
                    insert.setTimestamp(7, now);
                },
                "Failed to persist skill artifact in JDBC store: " + artifact.reference().qualifiedName());
    }

    @Override
    public boolean deleteArtifact(SkillArtifactReference reference) {
        SkillArtifactReference resolved = requireReference(reference);
        String sql = "DELETE FROM " + tableName
                + " WHERE skill_id = ? AND artifact_kind = ? AND artifact_name = ? AND artifact_version = ?";
        return JdbcSkillStoreSupport.executeUpdate(
                dataSource,
                sql,
                statement -> bindReference(statement, resolved, 1),
                "Failed to delete skill artifact from JDBC store: " + resolved.qualifiedName()) > 0;
    }

    private void initializeSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "skill_id VARCHAR(255) NOT NULL, "
                + "artifact_kind VARCHAR(64) NOT NULL, "
                + "artifact_name VARCHAR(255) NOT NULL, "
                + "artifact_version VARCHAR(255) NOT NULL, "
                + "content_base64 TEXT NOT NULL, "
                + "manifest TEXT NOT NULL, "
                + "updated_at TIMESTAMP NOT NULL, "
                + "PRIMARY KEY (skill_id, artifact_kind, artifact_name, artifact_version)"
                + ")";
        JdbcSkillStoreSupport.execute(
                dataSource,
                sql,
                "Failed to initialize JDBC skill artifact table: " + tableName);
    }

    private SkillArtifact readArtifact(ResultSet resultSet, String sourceDescription) throws SQLException {
        SkillArtifactManifest manifest = manifestCodec.fromBytes(
                SkillManagementPropertiesCodecSupport.toUtf8Bytes(resultSet.getString(1)),
                sourceDescription);
        byte[] content = Base64.getDecoder().decode(resultSet.getString(2));
        return manifest.toArtifact(content);
    }

    private SkillArtifactReference readReference(ResultSet resultSet) throws SQLException {
        return new SkillArtifactReference(
                resultSet.getString(1),
                SkillArtifactKind.fromLabel(resultSet.getString(2)),
                resultSet.getString(3),
                resultSet.getString(4));
    }

    private ListQuery listQuery(SkillArtifactQuery query) {
        StringBuilder sql = new StringBuilder("SELECT skill_id, artifact_kind, artifact_name, artifact_version FROM ")
                .append(tableName);
        List<String> parameters = new ArrayList<>();
        addCondition(sql, parameters, "skill_id", query.skillId());
        if (query.kind() != null) {
            addCondition(sql, parameters, "artifact_kind", query.kind().label());
        }
        addCondition(sql, parameters, "artifact_name", query.name());
        addCondition(sql, parameters, "artifact_version", query.version());
        sql.append(" ORDER BY skill_id, artifact_kind, artifact_name, artifact_version");
        return new ListQuery(sql.toString(), parameters);
    }

    private static void addCondition(
            StringBuilder sql,
            List<String> parameters,
            String column,
            String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sql.append(parameters.isEmpty() ? " WHERE " : " AND ")
                .append(column)
                .append(" = ?");
        parameters.add(value);
    }

    private static void bindReference(
            PreparedStatement statement,
            SkillArtifactReference reference,
            int startIndex) throws SQLException {
        SkillArtifactReference resolved = requireReference(reference);
        statement.setString(startIndex, resolved.skillId());
        statement.setString(startIndex + 1, resolved.kind().label());
        statement.setString(startIndex + 2, resolved.name());
        statement.setString(startIndex + 3, resolved.version());
    }

    private static SkillArtifactReference requireReference(SkillArtifactReference reference) {
        return Objects.requireNonNull(reference, "reference");
    }

    private record ListQuery(String sql, List<String> parameters) {

        private ListQuery {
            parameters = List.copyOf(parameters);
        }

        private void bind(PreparedStatement statement) throws SQLException {
            for (int index = 0; index < parameters.size(); index++) {
                statement.setString(index + 1, parameters.get(index));
            }
        }
    }
}
