package tech.kayys.wayang.agent.hermes;

import java.util.regex.Pattern;

/**
 * Shared validation for configurable Hermes JDBC table names.
 */
final class HermesJdbcTableNames {

    private static final Pattern SAFE_TABLE_NAME =
            Pattern.compile("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)?");

    private HermesJdbcTableNames() {
    }

    static String normalize(String tableName, String fallback, String label) {
        String normalized = tableName == null || tableName.isBlank()
                ? fallback
                : tableName.trim();
        if (!SAFE_TABLE_NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid Hermes " + label + " JDBC table name: " + tableName);
        }
        return normalized;
    }
}
