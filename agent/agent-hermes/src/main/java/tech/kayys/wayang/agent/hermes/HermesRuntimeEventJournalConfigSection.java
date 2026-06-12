package tech.kayys.wayang.agent.hermes;

/**
 * Applies runtime event journal config keys to an agent mode builder.
 */
final class HermesRuntimeEventJournalConfigSection implements HermesConfigSection {

    static final HermesConfigSection INSTANCE = new HermesRuntimeEventJournalConfigSection();

    private static final HermesConfigAliasGroup ALIASES = HermesConfigAliasGroup.of(
            HermesConfigAliasGroup.name("runtime-event-journal", "runtimeEventJournal"),
            HermesConfigAliasGroup.name("runtime-events", "runtimeEvents"));

    private HermesRuntimeEventJournalConfigSection() {
    }

    @Override
    public void apply(
            HermesConfigValues scoped,
            HermesAgentModeConfig.Builder builder) {
        scoped.booleanValue(ALIASES.keys("enabled", "Enabled"))
                .ifPresent(builder::runtimeEventJournalEnabled);
        scoped.get(ALIASES.keys("store", "Store"))
                .ifPresent(builder::runtimeEventJournalStore);
        scoped.get(ALIASES.keys("path", "Path"))
                .ifPresent(builder::runtimeEventJournalPath);
        scoped.get(ALIASES.keys("object-prefix", "ObjectPrefix"))
                .ifPresent(builder::runtimeEventJournalObjectPrefix);
        scoped.get(ALIASES.jdbcTableKeys())
                .ifPresent(builder::runtimeEventJournalJdbcTableName);
        scoped.booleanValue(ALIASES.jdbcInitializeSchemaKeys())
                .ifPresent(builder::runtimeEventJournalJdbcInitializeSchema);
        scoped.get(ALIASES.keys("format", "Format"))
                .ifPresent(builder::runtimeEventJournalFormat);
        scoped.intValue(ALIASES.keys("max-events", "MaxEvents"))
                .ifPresent(builder::runtimeEventJournalMaxEvents);
    }
}
