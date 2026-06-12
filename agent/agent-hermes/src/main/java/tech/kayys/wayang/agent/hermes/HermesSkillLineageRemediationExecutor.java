package tech.kayys.wayang.agent.hermes;

/**
 * Read-only executor for previewing learned-skill lineage remediation.
 */
public final class HermesSkillLineageRemediationExecutor {

    private static final HermesSkillLineageRemediationExecutor DEFAULT =
            new HermesSkillLineageRemediationExecutor();

    private final HermesSkillLineageRepairBackendRegistry backendRegistry;

    public HermesSkillLineageRemediationExecutor() {
        this(HermesSkillLineageRepairBackendRegistry.defaultRegistry());
    }

    public HermesSkillLineageRemediationExecutor(HermesSkillLineageRepairBackendRegistry backendRegistry) {
        this.backendRegistry = backendRegistry == null
                ? HermesSkillLineageRepairBackendRegistry.defaultRegistry()
                : backendRegistry;
    }

    public static HermesSkillLineageRemediationExecutor defaultExecutor() {
        return DEFAULT;
    }

    public HermesSkillLineageRemediationExecution dryRun(HermesSkillLineageCatalog catalog) {
        return dryRun(catalog, HermesSkillLineageRemediationPolicy.dryRun());
    }

    public HermesSkillLineageRemediationExecution dryRun(
            HermesSkillLineageCatalog catalog,
            HermesSkillLineageRemediationPolicy policy) {
        HermesSkillLineageCatalog resolved = catalog == null ? HermesSkillLineageCatalog.empty() : catalog;
        HermesSkillStoreConsistencyReport consistencyReport = resolved.consistencyReport();
        HermesSkillLineageRemediationPlan remediationPlan = resolved.health().remediationPlan();
        return HermesSkillLineageRemediationExecution.dryRun(
                remediationPlan,
                consistencyReport,
                policy,
                backendRegistry);
    }
}
