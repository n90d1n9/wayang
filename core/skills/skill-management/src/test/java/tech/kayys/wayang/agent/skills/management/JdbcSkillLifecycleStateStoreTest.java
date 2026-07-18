package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;

class JdbcSkillLifecycleStateStoreTest {

    @Test
    void persistsLifecycleStateThroughJdbcDataSource() {
        InMemoryLifecycleDataSource dataSource = new InMemoryLifecycleDataSource();
        JdbcSkillLifecycleStateStore store = new JdbcSkillLifecycleStateStore(dataSource, "lifecycle_states", true);
        SkillLifecycleState disabled = state("planner", SkillLifecycleStatus.DISABLED, 3);

        store.save(disabled);
        store.save(state("planner", SkillLifecycleStatus.DEPRECATED, 4));

        assertThat(store.get("planner").orElseThrow().status()).isEqualTo(SkillLifecycleStatus.DEPRECATED);
        assertThat(store.get("planner").orElseThrow().revision()).isEqualTo(4);
        assertThat(store.snapshot()).containsKey("planner");
        assertThat(store.remove("planner")).isTrue();
        assertThat(store.get("planner")).isEmpty();
    }

    @Test
    void factoryBuildsJdbcLifecycleStore() {
        InMemoryLifecycleDataSource dataSource = new InMemoryLifecycleDataSource();
        SkillLifecycleStateStore store = new SkillLifecycleStateStoreFactory(dataSource)
                .create(SkillLifecycleStateStoreConfig.jdbc("lifecycle_states", true));

        store.save(state("planner", SkillLifecycleStatus.DISABLED, 2));

        assertThat(store.get("planner")).isPresent();
    }

    private static SkillLifecycleState state(String skillId, SkillLifecycleStatus status, int revision) {
        return new SkillLifecycleState(
                skillId,
                status,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-02T00:00:00Z"),
                revision);
    }

    private record Row(
            String skillId,
            String status,
            String createdAt,
            String updatedAt,
            int revision) {
    }

    private static final class InMemoryLifecycleDataSource implements DataSource {
        private final Map<String, Row> rows = new LinkedHashMap<>();

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
                case "setString", "setInt" -> {
                    parameters.put((Integer) args[0], args[1]);
                    yield null;
                }
                case "execute" -> true;
                case "executeUpdate" -> executeUpdate(sql, parameters);
                case "executeQuery" -> resultSet(select(sql, parameters));
                case "close" -> null;
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            });
        }

        private int executeUpdate(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (normalizedSql.startsWith("UPDATE")) {
                String skillId = (String) parameters.get(5);
                if (!rows.containsKey(skillId)) {
                    return 0;
                }
                rows.put(skillId, new Row(
                        skillId,
                        (String) parameters.get(1),
                        (String) parameters.get(2),
                        (String) parameters.get(3),
                        (Integer) parameters.get(4)));
                return 1;
            }
            if (normalizedSql.startsWith("INSERT")) {
                String skillId = (String) parameters.get(1);
                rows.put(skillId, new Row(
                        skillId,
                        (String) parameters.get(2),
                        (String) parameters.get(3),
                        (String) parameters.get(4),
                        (Integer) parameters.get(5)));
                return 1;
            }
            if (normalizedSql.startsWith("DELETE")) {
                return rows.remove((String) parameters.get(1)) == null ? 0 : 1;
            }
            return 0;
        }

        private List<Row> select(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (normalizedSql.contains("WHERE SKILL_ID = ?")) {
                Row row = rows.get((String) parameters.get(1));
                return row == null ? List.of() : List.of(row);
            }
            return rows.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .toList();
        }

        private ResultSet resultSet(List<Row> values) {
            return proxy(ResultSet.class, new InvocationHandler() {
                private int index = -1;

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    return switch (method.getName()) {
                        case "next" -> ++index < values.size();
                        case "getString" -> string(values.get(index), (Integer) args[0]);
                        case "getInt" -> values.get(index).revision();
                        case "close" -> null;
                        case "isClosed" -> false;
                        default -> defaultValue(method.getReturnType());
                    };
                }
            });
        }

        private String string(Row row, int columnIndex) {
            return switch (columnIndex) {
                case 1 -> row.skillId();
                case 2 -> row.status();
                case 3 -> row.createdAt();
                case 4 -> row.updatedAt();
                default -> throw new IllegalArgumentException("Unsupported column index: " + columnIndex);
            };
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] { type }, handler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0.0F;
        }
        if (returnType == Double.TYPE) {
            return 0.0D;
        }
        return null;
    }
}
