package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HermesConfigAliasGroupTest {

    @Test
    void buildsSuffixKeysInAliasPriorityOrder() {
        HermesConfigAliasGroup aliases = HermesConfigAliasGroup.of(
                HermesConfigAliasGroup.name("runtime-event-journal", "runtimeEventJournal"),
                HermesConfigAliasGroup.name("runtime-events", "runtimeEvents"));

        assertThat(aliases.keys("object-prefix", "ObjectPrefix"))
                .containsExactly(
                        "runtime-event-journal-object-prefix",
                        "runtimeEventJournalObjectPrefix",
                        "runtime-events-object-prefix",
                        "runtimeEventsObjectPrefix");
    }

    @Test
    void buildsJdbcKeysWithPrimaryDatabaseFallbackAlias() {
        HermesConfigAliasGroup aliases = HermesConfigAliasGroup.of(
                HermesConfigAliasGroup.name("skill-lineage-repair-approval", "skillLineageRepairApproval"),
                HermesConfigAliasGroup.name("lineage-repair-approval", "lineageRepairApproval"),
                HermesConfigAliasGroup.name("repair-approval", "repairApproval"));

        assertThat(aliases.jdbcTableKeys())
                .containsExactly(
                        "skill-lineage-repair-approval-jdbc-table-name",
                        "skillLineageRepairApprovalJdbcTableName",
                        "skill-lineage-repair-approval-database-table",
                        "lineage-repair-approval-jdbc-table-name",
                        "lineageRepairApprovalJdbcTableName",
                        "repair-approval-jdbc-table-name",
                        "repairApprovalJdbcTableName");
    }
}
