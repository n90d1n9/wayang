package tech.kayys.wayang.agent.hermes;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Small JDBC helper for Hermes stores that persist JSON-shaped records.
 */
final class HermesJdbcStoreSupport {

    private HermesJdbcStoreSupport() {
    }

    static void execute(DataSource dataSource, String sql, String failureMessage) {
        try (Connection connection = dataSource(dataSource).getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (SQLException error) {
            throw new IllegalStateException(failureMessage, error);
        }
    }

    static int countRows(DataSource dataSource, String tableName, String failureMessage) {
        return queryFirst(
                        dataSource,
                        "SELECT COUNT(*) FROM " + tableName,
                        statement -> {
                        },
                        resultSet -> resultSet.getInt(1),
                        failureMessage)
                .orElse(0);
    }

    static void pruneRowsToCapacity(
            Connection connection,
            String tableName,
            String idColumn,
            String orderBy,
            int maxRows) throws SQLException {
        pruneRowsToCapacity(
                connection,
                tableName,
                idColumn,
                orderBy,
                HermesRecordRetentionPolicy.bounded(maxRows));
    }

    static void pruneRowsToCapacity(
            Connection connection,
            String tableName,
            String idColumn,
            String orderBy,
            HermesRecordRetentionPolicy retentionPolicy) throws SQLException {
        HermesRecordRetentionPolicy policy = HermesRecordRetentionPolicy.orDefault(retentionPolicy);
        List<String> orderedRecordIds = new ArrayList<>();
        String selectSql = "SELECT " + idColumn + " FROM " + tableName + " ORDER BY " + orderBy;
        try (PreparedStatement statement = connection.prepareStatement(selectSql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                orderedRecordIds.add(resultSet.getString(1));
            }
        }
        List<String> staleRecordIds = policy.staleFromNewestFirst(orderedRecordIds);
        if (staleRecordIds.isEmpty()) {
            return;
        }
        String deleteSql = "DELETE FROM " + tableName + " WHERE " + idColumn + " = ?";
        try (PreparedStatement statement = connection.prepareStatement(deleteSql)) {
            for (String recordId : staleRecordIds) {
                statement.setString(1, recordId);
                statement.executeUpdate();
            }
        }
    }

    static <T> Optional<T> queryFirst(
            DataSource dataSource,
            String sql,
            StatementBinder binder,
            ResultMapper<T> mapper,
            String failureMessage) {
        try (Connection connection = dataSource(dataSource).getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.ofNullable(mapper.map(resultSet));
            }
        } catch (SQLException error) {
            throw new IllegalStateException(failureMessage, error);
        }
    }

    static Map<String, Object> jsonObject(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        return HermesRuntimeEventJsonCodec.objectFromJsonLine(value);
    }

    static String jsonLine(Map<String, Object> value) {
        return HermesRuntimeEventJsonCodec.toJsonLine(value == null ? Map.of() : value);
    }

    private static DataSource dataSource(DataSource dataSource) {
        return Objects.requireNonNull(dataSource, "dataSource");
    }

    @FunctionalInterface
    interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    interface ResultMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }
}
