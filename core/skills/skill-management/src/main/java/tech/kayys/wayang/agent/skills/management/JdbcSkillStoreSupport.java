package tech.kayys.wayang.agent.skills.management;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared JDBC operations for skill-management stores.
 */
final class JdbcSkillStoreSupport {

    private JdbcSkillStoreSupport() {
    }

    @FunctionalInterface
    interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }

    @FunctionalInterface
    interface RowMapper<T> {
        T map(ResultSet resultSet) throws SQLException;
    }

    static void execute(DataSource dataSource, String sql, String failureMessage) {
        try (Connection connection = Objects.requireNonNull(dataSource, "dataSource").getConnection();
                PreparedStatement statement = connection.prepareStatement(Objects.requireNonNull(sql, "sql"))) {
            statement.execute();
        } catch (SQLException error) {
            throw new IllegalStateException(failureMessage, error);
        }
    }

    static boolean deleteById(
            DataSource dataSource,
            String tableName,
            String idColumn,
            String id,
            String failureMessage) {
        String sql = "DELETE FROM " + tableName + " WHERE " + idColumn + " = ?";
        try (Connection connection = Objects.requireNonNull(dataSource, "dataSource").getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException error) {
            throw new IllegalStateException(failureMessage, error);
        }
    }

    static int executeUpdate(
            DataSource dataSource,
            String sql,
            StatementBinder binder,
            String failureMessage) {
        try (Connection connection = Objects.requireNonNull(dataSource, "dataSource").getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(Objects.requireNonNull(sql, "sql"))) {
            Objects.requireNonNull(binder, "binder").bind(statement);
            return statement.executeUpdate();
        } catch (SQLException error) {
            throw new IllegalStateException(failureMessage, error);
        }
    }

    static boolean updateThenInsert(
            DataSource dataSource,
            String updateSql,
            StatementBinder updateBinder,
            String insertSql,
            StatementBinder insertBinder,
            String failureMessage) {
        try (Connection connection = Objects.requireNonNull(dataSource, "dataSource").getConnection()) {
            try (PreparedStatement update =
                    connection.prepareStatement(Objects.requireNonNull(updateSql, "updateSql"))) {
                Objects.requireNonNull(updateBinder, "updateBinder").bind(update);
                if (update.executeUpdate() > 0) {
                    return true;
                }
            }
            try (PreparedStatement insert =
                    connection.prepareStatement(Objects.requireNonNull(insertSql, "insertSql"))) {
                Objects.requireNonNull(insertBinder, "insertBinder").bind(insert);
                insert.executeUpdate();
                return false;
            }
        } catch (SQLException error) {
            throw new IllegalStateException(failureMessage, error);
        }
    }

    static <T> Optional<T> queryOne(
            DataSource dataSource,
            String sql,
            StatementBinder binder,
            RowMapper<T> mapper,
            String failureMessage) {
        List<T> rows = query(
                dataSource,
                sql,
                binder,
                mapper,
                failureMessage);
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }

    static <T> List<T> query(
            DataSource dataSource,
            String sql,
            RowMapper<T> mapper,
            String failureMessage) {
        return query(dataSource, sql, statement -> {
        }, mapper, failureMessage);
    }

    static <T> List<T> query(
            DataSource dataSource,
            String sql,
            StatementBinder binder,
            RowMapper<T> mapper,
            String failureMessage) {
        try (Connection connection = Objects.requireNonNull(dataSource, "dataSource").getConnection();
                PreparedStatement statement = connection.prepareStatement(Objects.requireNonNull(sql, "sql"))) {
            Objects.requireNonNull(binder, "binder").bind(statement);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> rows = new ArrayList<>();
                RowMapper<T> resolvedMapper = Objects.requireNonNull(mapper, "mapper");
                while (resultSet.next()) {
                    rows.add(resolvedMapper.map(resultSet));
                }
                return List.copyOf(rows);
            }
        } catch (SQLException error) {
            throw new IllegalStateException(failureMessage, error);
        }
    }
}
