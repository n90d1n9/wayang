package tech.kayys.wayang.agent.skills.management;

import java.util.regex.Pattern;

/**
 * Shared JDBC table-name validation for skill-management stores.
 */
final class JdbcSkillStoreTables {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)?");

    private JdbcSkillStoreTables() {
    }

    static String normalize(String tableName, String defaultTableName, String label) {
        String normalized = tableName == null || tableName.isBlank() ? defaultTableName : tableName.trim();
        if (!SAFE_TABLE_NAME.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid JDBC " + label + " table name: " + tableName);
        }
        return normalized;
    }
}
