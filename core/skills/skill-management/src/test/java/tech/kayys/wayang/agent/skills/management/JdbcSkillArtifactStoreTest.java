package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcSkillArtifactStoreTest {

    @Test
    void persistsArtifactsAcrossStoreInstances() {
        InMemoryArtifactDataSource dataSource = new InMemoryArtifactDataSource();
        JdbcSkillArtifactStore writer = new JdbcSkillArtifactStore(dataSource, "skill_artifacts", true);
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");
        SkillArtifact artifact = new SkillArtifact(
                reference,
                "hello".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                Map.of("tenant", "tenant-a"));

        writer.putArtifact(artifact);
        writer.putArtifact(new SkillArtifact(
                reference,
                "updated".getBytes(StandardCharsets.UTF_8),
                "text/plain",
                Map.of("tenant", "tenant-b")));

        JdbcSkillArtifactStore reader = new JdbcSkillArtifactStore(dataSource, "skill_artifacts", false);
        assertThat(reader.getArtifact(reference))
                .hasValueSatisfying(reloaded -> {
                    assertThat(new String(reloaded.content(), StandardCharsets.UTF_8)).isEqualTo("updated");
                    assertThat(reloaded.contentType()).isEqualTo("text/plain");
                    assertThat(reloaded.metadata()).containsEntry("tenant", "tenant-b");
                });
        assertThat(reader.listArtifacts("planner")).containsExactly(reference);
        assertThat(reader.deleteArtifact(reference)).isTrue();
        assertThat(reader.deleteArtifact(reference)).isFalse();
        assertThat(reader.getArtifact(reference)).isEmpty();
    }

    @Test
    void listsArtifactsByQueryFromIdentityColumns() {
        JdbcSkillArtifactStore store =
                new JdbcSkillArtifactStore(new InMemoryArtifactDataSource(), "skill_artifacts", true);
        SkillArtifactReference b = SkillArtifactReference.resource("planner", "b", "v1");
        SkillArtifactReference a = SkillArtifactReference.resource("planner", "a", "v1");
        SkillArtifactReference packageArtifact = SkillArtifactReference.packageArtifact("planner", "v1");
        SkillArtifactReference otherSkill = SkillArtifactReference.resource("writer", "a", "v1");

        store.putArtifact(SkillArtifact.of(b, new byte[] {2}));
        store.putArtifact(SkillArtifact.of(a, new byte[] {1}));
        store.putArtifact(SkillArtifact.of(packageArtifact, new byte[] {3}));
        store.putArtifact(SkillArtifact.of(otherSkill, new byte[] {4}));

        assertThat(store.listArtifacts(SkillArtifactQuery.forKind("planner", SkillArtifactKind.RESOURCE, 10)))
                .containsExactly(a, b);
        assertThat(store.listArtifacts(SkillArtifactQuery.forSkill("planner", 2)))
                .containsExactly(packageArtifact, a);
    }

    @Test
    void rejectsCorruptedPayloadWhenManifestHasDigest() {
        InMemoryArtifactDataSource dataSource = new InMemoryArtifactDataSource();
        JdbcSkillArtifactStore store = new JdbcSkillArtifactStore(dataSource, "skill_artifacts", true);
        SkillArtifactReference reference = SkillArtifactReference.resource("planner", "prompt", "v1");

        store.putArtifact(SkillArtifact.text(reference, "hello"));
        dataSource.corruptContent(reference, "other".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> store.getArtifact(reference))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SHA-256 mismatch")
                .hasMessageContaining("planner:resource:prompt:v1");
    }

    private record Row(
            String skillId,
            String kind,
            String name,
            String version,
            String contentBase64,
            String manifest) {
    }

    private static final class InMemoryArtifactDataSource implements DataSource {
        private final Map<String, Row> rows = new LinkedHashMap<>();

        void corruptContent(SkillArtifactReference reference, byte[] content) {
            Row row = rows.get(key(reference));
            rows.put(
                    key(reference),
                    new Row(
                            row.skillId(),
                            row.kind(),
                            row.name(),
                            row.version(),
                            Base64.getEncoder().encodeToString(content),
                            row.manifest()));
        }

        @Override
        public Connection getConnection() {
            return proxy(Connection.class, this::connection);
        }

        @Override
        public Connection getConnection(String username, String password) {
            return getConnection();
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("unwrap is not supported");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }

        private Object connection(Object proxy, Method method, Object[] args) throws SQLException {
            return switch (method.getName()) {
                case "prepareStatement" -> preparedStatement((String) args[0]);
                case "close" -> null;
                case "isClosed" -> false;
                case "unwrap" -> throw new SQLException("unwrap is not supported");
                case "isWrapperFor" -> false;
                default -> defaultValue(method.getReturnType());
            };
        }

        private PreparedStatement preparedStatement(String sql) {
            Map<Integer, Object> parameters = new LinkedHashMap<>();
            return proxy(PreparedStatement.class, (proxy, method, args) -> switch (method.getName()) {
                case "setString", "setTimestamp" -> {
                    parameters.put((Integer) args[0], args[1]);
                    yield null;
                }
                case "execute" -> true;
                case "executeUpdate" -> executeUpdate(sql, parameters);
                case "executeQuery" -> resultSet(sql, select(sql, parameters));
                case "close" -> null;
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            });
        }

        private int executeUpdate(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (normalizedSql.startsWith("UPDATE")) {
                String key = key(parameters, 4);
                Row existing = rows.get(key);
                if (existing == null) {
                    return 0;
                }
                rows.put(
                        key,
                        new Row(
                                existing.skillId(),
                                existing.kind(),
                                existing.name(),
                                existing.version(),
                                (String) parameters.get(1),
                                (String) parameters.get(2)));
                return 1;
            }
            if (normalizedSql.startsWith("INSERT")) {
                rows.put(
                        key(parameters, 1),
                        new Row(
                                (String) parameters.get(1),
                                (String) parameters.get(2),
                                (String) parameters.get(3),
                                (String) parameters.get(4),
                                (String) parameters.get(5),
                                (String) parameters.get(6)));
                return 1;
            }
            if (normalizedSql.startsWith("DELETE")) {
                return rows.remove(key(parameters, 1)) == null ? 0 : 1;
            }
            return 0;
        }

        private List<Row> select(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            return rows.values().stream()
                    .filter(row -> matches(row, normalizedSql, parameters))
                    .sorted(Comparator.comparing(Row::skillId)
                            .thenComparing(Row::kind)
                            .thenComparing(Row::name)
                            .thenComparing(Row::version))
                    .toList();
        }

        private boolean matches(Row row, String normalizedSql, Map<Integer, Object> parameters) {
            int index = 1;
            if (normalizedSql.contains("SKILL_ID = ?")
                    && !row.skillId().equals(parameters.get(index++))) {
                return false;
            }
            if (normalizedSql.contains("ARTIFACT_KIND = ?")
                    && !row.kind().equals(parameters.get(index++))) {
                return false;
            }
            if (normalizedSql.contains("ARTIFACT_NAME = ?")
                    && !row.name().equals(parameters.get(index++))) {
                return false;
            }
            return !normalizedSql.contains("ARTIFACT_VERSION = ?")
                    || row.version().equals(parameters.get(index));
        }

        private ResultSet resultSet(String sql, List<Row> values) {
            boolean artifactQuery = sql.toUpperCase(Locale.ROOT).startsWith("SELECT MANIFEST");
            return proxy(ResultSet.class, new InvocationHandler() {
                private int index = -1;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    return switch (method.getName()) {
                        case "next" -> ++index < values.size();
                        case "getString" -> value(values.get(index), (Integer) args[0], artifactQuery);
                        case "close" -> null;
                        case "isClosed" -> false;
                        default -> defaultValue(method.getReturnType());
                    };
                }
            });
        }

        private String value(Row row, int column, boolean artifactQuery) {
            if (artifactQuery) {
                return column == 1 ? row.manifest() : row.contentBase64();
            }
            return switch (column) {
                case 1 -> row.skillId();
                case 2 -> row.kind();
                case 3 -> row.name();
                case 4 -> row.version();
                default -> throw new IllegalArgumentException("Unexpected column: " + column);
            };
        }

        private static String key(SkillArtifactReference reference) {
            return String.join("|",
                    reference.skillId(),
                    reference.kind().label(),
                    reference.name(),
                    reference.version());
        }

        private static String key(Map<Integer, Object> parameters, int startIndex) {
            return String.join("|",
                    (String) parameters.get(startIndex),
                    (String) parameters.get(startIndex + 1),
                    (String) parameters.get(startIndex + 2),
                    (String) parameters.get(startIndex + 3));
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == Void.TYPE) {
            return null;
        }
        if (type == Boolean.TYPE) {
            return false;
        }
        if (type == Integer.TYPE) {
            return 0;
        }
        if (type == Long.TYPE) {
            return 0L;
        }
        if (type == Double.TYPE) {
            return 0.0d;
        }
        if (type == Float.TYPE) {
            return 0.0f;
        }
        if (type == Short.TYPE) {
            return (short) 0;
        }
        if (type == Byte.TYPE) {
            return (byte) 0;
        }
        if (type == Character.TYPE) {
            return (char) 0;
        }
        return null;
    }
}
