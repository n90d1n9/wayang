package tech.kayys.wayang.agent.hermes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Alias set for one Hermes config area.
 */
record HermesConfigAliasGroup(List<Name> names) {

    HermesConfigAliasGroup {
        if (names == null || names.isEmpty()) {
            throw new IllegalArgumentException("Config aliases must include a primary name");
        }
        names = List.copyOf(names);
    }

    static HermesConfigAliasGroup of(Name primary, Name... aliases) {
        Objects.requireNonNull(primary, "primary");
        List<Name> names = new ArrayList<>();
        names.add(primary);
        if (aliases != null) {
            names.addAll(Arrays.asList(aliases));
        }
        return new HermesConfigAliasGroup(names);
    }

    static Name name(String hyphen, String camel) {
        return new Name(hyphen, camel);
    }

    String[] keys(String hyphenSuffix, String camelSuffix) {
        String normalizedHyphenSuffix = required(hyphenSuffix, "hyphenSuffix");
        String normalizedCamelSuffix = required(camelSuffix, "camelSuffix");
        List<String> keys = new ArrayList<>(names.size() * 2);
        for (Name name : names) {
            keys.add(name.hyphen() + "-" + normalizedHyphenSuffix);
            keys.add(name.camel() + normalizedCamelSuffix);
        }
        return keys.toArray(String[]::new);
    }

    String[] jdbcTableKeys() {
        return databaseBackedKeys("jdbc-table-name", "JdbcTableName", "database-table");
    }

    String[] jdbcInitializeSchemaKeys() {
        return databaseBackedKeys(
                "jdbc-initialize-schema",
                "JdbcInitializeSchema",
                "database-initialize-schema");
    }

    private String[] databaseBackedKeys(
            String jdbcHyphenSuffix,
            String jdbcCamelSuffix,
            String databaseHyphenSuffix) {
        List<String> keys = new ArrayList<>(names.size() * 2 + 1);
        Name primary = names.get(0);
        keys.add(primary.hyphen() + "-" + jdbcHyphenSuffix);
        keys.add(primary.camel() + jdbcCamelSuffix);
        keys.add(primary.hyphen() + "-" + databaseHyphenSuffix);
        for (int index = 1; index < names.size(); index++) {
            Name alias = names.get(index);
            keys.add(alias.hyphen() + "-" + jdbcHyphenSuffix);
            keys.add(alias.camel() + jdbcCamelSuffix);
        }
        return keys.toArray(String[]::new);
    }

    record Name(String hyphen, String camel) {

        Name {
            hyphen = required(hyphen, "hyphen");
            camel = required(camel, "camel");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Config alias " + field + " must not be blank");
        }
        return value.trim();
    }
}
