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
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcSkillManagementEventStoreTest {

    @Test
    void persistsAndQueriesEventsThroughJdbcDataSource() {
        InMemoryEventDataSource dataSource = new InMemoryEventDataSource();
        JdbcSkillManagementEventStore writer = new JdbcSkillManagementEventStore(dataSource, "skill_events", true, 10);
        writer.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        writer.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        JdbcSkillManagementEventStore reader = new JdbcSkillManagementEventStore(dataSource, "skill_events", false, 10);
        SkillManagementEventPage failures = reader.query(SkillManagementEventQuery.failures(10));

        assertThat(reader.events()).hasSize(2);
        assertThat(failures.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.DELETE_SKILL);
        assertThat(failures.events().get(0).attributes()).containsEntry("status", "DELETE_SKILL");
        assertThat(failures.summary().failedEvents()).isEqualTo(1);
    }

    @Test
    void prunesOldestEventsWhenRetentionIsExceeded() {
        InMemoryEventDataSource dataSource = new InMemoryEventDataSource();
        JdbcSkillManagementEventStore store = new JdbcSkillManagementEventStore(dataSource, "skill_events", true, 2);

        store.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.UPDATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:02Z", SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        assertThat(store.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.UPDATE_SKILL,
                        SkillManagementEventOperation.DELETE_SKILL);
    }

    @Test
    void prunesOldestEventsOnDemandWithDryRun() {
        InMemoryEventDataSource dataSource = new InMemoryEventDataSource();
        JdbcSkillManagementEventStore store = new JdbcSkillManagementEventStore(dataSource, "skill_events", true, 10);
        store.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.UPDATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:02Z", SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        SkillManagementEventPruneResult preview =
                store.pruneEvents(SkillManagementEventPruneOptions.dryRun(1));

        assertThat(preview.prunedEvents()).isEqualTo(2);
        assertThat(store.events()).hasSize(3);

        SkillManagementEventPruneResult result =
                store.pruneEvents(SkillManagementEventPruneOptions.keepLatest(1));

        assertThat(result.changed()).isTrue();
        assertThat(result.prunedEventReferences()).hasSize(2);
        assertThat(store.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.DELETE_SKILL);
    }

    @Test
    void ignoresNullEvents() {
        InMemoryEventDataSource dataSource = new InMemoryEventDataSource();
        JdbcSkillManagementEventStore store = new JdbcSkillManagementEventStore(dataSource, "skill_events", true, 10);

        store.record(null);

        assertThat(store.events()).isEmpty();
        assertThat(store.latest().events()).isEmpty();
    }

    @Test
    void factoryBuildsJdbcEventStore() {
        InMemoryEventDataSource dataSource = new InMemoryEventDataSource();
        SkillManagementEventSink sink = new SkillManagementEventStoreFactory(dataSource)
                .create(SkillManagementEventStoreConfig.jdbc("skill_events", true, 10));

        sink.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));

        assertThat(sink).isInstanceOf(SkillManagementEventReader.class);
        assertThat(((SkillManagementEventReader) sink).latest().events()).hasSize(1);
    }

    @Test
    void factoryRejectsJdbcEventStoreWithoutDataSource() {
        SkillManagementEventStoreFactory factory = new SkillManagementEventStoreFactory();

        assertThatThrownBy(() -> factory.create(SkillManagementEventStoreConfig.jdbc()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DataSource");
    }

    private SkillManagementEvent event(
            String occurredAt,
            SkillManagementEventOperation operation,
            String skillId,
            boolean success) {
        return new SkillManagementEvent(
                Instant.parse(occurredAt),
                operation,
                skillId,
                success,
                Map.of("status", operation.name()));
    }

    private record Row(
            String eventId,
            String occurredAt,
            String operation,
            String skillId,
            String success,
            String attributes) {
    }

    private static final class InMemoryEventDataSource implements DataSource {
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
                case "setString" -> {
                    parameters.put((Integer) args[0], args[1]);
                    yield null;
                }
                case "execute" -> true;
                case "executeUpdate" -> executeUpdate(sql, parameters);
                case "executeQuery" -> resultSet(select(sql));
                case "close" -> null;
                case "isClosed" -> false;
                default -> defaultValue(method.getReturnType());
            });
        }

        private int executeUpdate(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (normalizedSql.startsWith("INSERT")) {
                String eventId = (String) parameters.get(1);
                rows.put(eventId, new Row(
                        eventId,
                        (String) parameters.get(2),
                        (String) parameters.get(3),
                        (String) parameters.get(4),
                        (String) parameters.get(5),
                        (String) parameters.get(6)));
                return 1;
            }
            if (normalizedSql.startsWith("DELETE")) {
                return rows.remove((String) parameters.get(1)) == null ? 0 : 1;
            }
            return 0;
        }

        private List<Row> select(String sql) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (!normalizedSql.startsWith("SELECT EVENT_ID")) {
                return List.of();
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
                        case "close" -> null;
                        case "isClosed" -> false;
                        default -> defaultValue(method.getReturnType());
                    };
                }
            });
        }

        private String string(Row row, int columnIndex) {
            return switch (columnIndex) {
                case 1 -> row.eventId();
                case 2 -> row.occurredAt();
                case 3 -> row.operation();
                case 4 -> row.skillId();
                case 5 -> row.success();
                case 6 -> row.attributes();
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
