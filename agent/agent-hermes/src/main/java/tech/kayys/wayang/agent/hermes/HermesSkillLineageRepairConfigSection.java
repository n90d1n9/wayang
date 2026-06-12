package tech.kayys.wayang.agent.hermes;

import java.util.List;
import java.util.Optional;

/**
 * Applies skill-lineage repair config keys to an agent mode builder.
 */
final class HermesSkillLineageRepairConfigSection implements HermesConfigSection {

    static final HermesConfigSection INSTANCE = new HermesSkillLineageRepairConfigSection();

    private static final HermesConfigAliasGroup REPAIR_APPROVAL_ALIASES = HermesConfigAliasGroup.of(
            HermesConfigAliasGroup.name("skill-lineage-repair-approval", "skillLineageRepairApproval"),
            HermesConfigAliasGroup.name("lineage-repair-approval", "lineageRepairApproval"),
            HermesConfigAliasGroup.name("repair-approval", "repairApproval"));
    private static final HermesConfigAliasGroup REPAIR_DISPATCH_LEDGER_ALIASES = HermesConfigAliasGroup.of(
            HermesConfigAliasGroup.name("skill-lineage-repair-dispatch-ledger", "skillLineageRepairDispatchLedger"),
            HermesConfigAliasGroup.name("lineage-repair-dispatch-ledger", "lineageRepairDispatchLedger"),
            HermesConfigAliasGroup.name("repair-dispatch-ledger", "repairDispatchLedger"));

    private HermesSkillLineageRepairConfigSection() {
    }

    @Override
    public void apply(
            HermesConfigValues scoped,
            HermesAgentModeConfig.Builder builder) {
        remediationPolicy(scoped).ifPresent(builder::skillLineageRemediationPolicy);
        scoped.listValue(
                        "skill-lineage-repair-backends",
                        "skillLineageRepairBackends",
                        "lineage-repair-backends",
                        "lineageRepairBackends")
                .ifPresent(builder::skillLineageRepairBackends);
        scoped.listValue(
                        "skill-lineage-repair-mutation-backends",
                        "skillLineageRepairMutationBackends",
                        "lineage-repair-mutation-backends",
                        "lineageRepairMutationBackends")
                .ifPresent(builder::skillLineageRepairMutationBackends);
        scoped.get(REPAIR_APPROVAL_ALIASES.keys("store", "Store"))
                .ifPresent(builder::skillLineageRepairApprovalStore);
        scoped.get(REPAIR_APPROVAL_ALIASES.keys("path", "Path"))
                .ifPresent(builder::skillLineageRepairApprovalPath);
        scoped.get(REPAIR_APPROVAL_ALIASES.keys("object-prefix", "ObjectPrefix"))
                .ifPresent(builder::skillLineageRepairApprovalObjectPrefix);
        scoped.get(REPAIR_APPROVAL_ALIASES.jdbcTableKeys())
                .ifPresent(builder::skillLineageRepairApprovalJdbcTableName);
        scoped.booleanValue(REPAIR_APPROVAL_ALIASES.jdbcInitializeSchemaKeys())
                .ifPresent(builder::skillLineageRepairApprovalJdbcInitializeSchema);
        scoped.get(REPAIR_DISPATCH_LEDGER_ALIASES.keys("store", "Store"))
                .ifPresent(builder::skillLineageRepairDispatchLedgerStore);
        scoped.get(REPAIR_DISPATCH_LEDGER_ALIASES.keys("path", "Path"))
                .ifPresent(builder::skillLineageRepairDispatchLedgerPath);
        scoped.get(REPAIR_DISPATCH_LEDGER_ALIASES.keys("object-prefix", "ObjectPrefix"))
                .ifPresent(builder::skillLineageRepairDispatchLedgerObjectPrefix);
        scoped.get(REPAIR_DISPATCH_LEDGER_ALIASES.jdbcTableKeys())
                .ifPresent(builder::skillLineageRepairDispatchLedgerJdbcTableName);
        scoped.booleanValue(REPAIR_DISPATCH_LEDGER_ALIASES.jdbcInitializeSchemaKeys())
                .ifPresent(builder::skillLineageRepairDispatchLedgerJdbcInitializeSchema);
        scoped.intValue(REPAIR_DISPATCH_LEDGER_ALIASES.keys("max-records", "MaxRecords"))
                .ifPresent(builder::skillLineageRepairDispatchLedgerMaxRecords);
    }

    private static Optional<HermesSkillLineageRemediationPolicy> remediationPolicy(HermesConfigValues scoped) {
        Optional<String> mode = scoped.get(
                "skill-lineage-remediation-mode",
                "skillLineageRemediationMode",
                "lineage-remediation-mode",
                "lineageRemediationMode");
        Optional<Integer> maxActions = scoped.intValue(
                "skill-lineage-remediation-max-actions",
                "skillLineageRemediationMaxActions",
                "lineage-remediation-max-actions",
                "lineageRemediationMaxActions");
        Optional<List<String>> allowedActions = scoped.listValue(
                "skill-lineage-remediation-allowed-actions",
                "skillLineageRemediationAllowedActions",
                "lineage-remediation-allowed-actions",
                "lineageRemediationAllowedActions");
        Optional<List<String>> allowedTargetTypes = scoped.listValue(
                "skill-lineage-remediation-allowed-target-types",
                "skillLineageRemediationAllowedTargetTypes",
                "lineage-remediation-allowed-target-types",
                "lineageRemediationAllowedTargetTypes");
        if (mode.isEmpty() && maxActions.isEmpty() && allowedActions.isEmpty() && allowedTargetTypes.isEmpty()) {
            return Optional.empty();
        }
        HermesSkillLineageRemediationPolicy defaults =
                HermesAgentModeConfig.defaults().skillLineageRemediationPolicy();
        return Optional.of(new HermesSkillLineageRemediationPolicy(
                mode.orElse(defaults.mode()),
                maxActions.orElse(defaults.maxActionsPerRun()),
                allowedActions.orElse(defaults.allowedActions()),
                allowedTargetTypes.orElse(defaults.allowedTargetTypes())));
    }
}
