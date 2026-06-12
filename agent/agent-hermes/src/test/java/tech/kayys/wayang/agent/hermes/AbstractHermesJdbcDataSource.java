package tech.kayys.wayang.agent.hermes;

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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

abstract class AbstractHermesJdbcDataSource implements DataSource {

    @Override
    public final Connection getConnection() {
        return proxy(Connection.class, this::connection);
    }

    @Override
    public final Connection getConnection(String username, String password) {
        return getConnection();
    }

    @Override
    public final PrintWriter getLogWriter() {
        return null;
    }

    @Override
    public final void setLogWriter(PrintWriter out) {
    }

    @Override
    public final void setLoginTimeout(int seconds) {
    }

    @Override
    public final int getLoginTimeout() {
        return 0;
    }

    @Override
    public final Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public final <T> T unwrap(Class<T> iface) throws SQLException {
        throw new SQLException("unwrap is not supported");
    }

    @Override
    public final boolean isWrapperFor(Class<?> iface) {
        return false;
    }

    protected abstract int executeUpdate(String sql, Map<Integer, Object> parameters);

    protected abstract List<List<Object>> select(String sql, Map<Integer, Object> parameters);

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
            case "setString", "setInt", "setLong", "setBoolean", "setObject" -> {
                parameters.put((Integer) args[0], args[1]);
                yield null;
            }
            case "clearParameters" -> {
                parameters.clear();
                yield null;
            }
            case "execute" -> false;
            case "executeUpdate" -> executeUpdate(sql, parameters);
            case "executeQuery" -> resultSet(select(sql, parameters));
            case "close" -> null;
            case "isClosed" -> false;
            default -> defaultValue(method.getReturnType());
        });
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[] {type},
                handler);
    }

    private static ResultSet resultSet(List<List<Object>> rows) {
        return proxy(ResultSet.class, new InvocationHandler() {
            private int index = -1;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                return switch (method.getName()) {
                    case "next" -> ++index < rows.size();
                    case "getString" -> stringValue(rows.get(index).get(((Integer) args[0]) - 1));
                    case "getInt" -> ((Number) rows.get(index).get(((Integer) args[0]) - 1)).intValue();
                    case "close" -> null;
                    case "isClosed" -> false;
                    default -> defaultValue(method.getReturnType());
                };
            }
        });
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == void.class) {
            return null;
        }
        if (type == char.class) {
            return '\0';
        }
        return 0;
    }
}
