package tech.kayys.wayang.agent.hermes;

/**
 * Runtime adapter boundary for learned-skill lineage inspection.
 */
public interface HermesSkillLineagePort {

    HermesPortDispatchResult inspect(HermesSkillLineageDirective directive);

    default HermesRuntimePortDescriptor descriptor() {
        return HermesRuntimePortDescriptor.configured(HermesRuntimePortCatalog.SKILL_LINEAGE, this);
    }

    static HermesSkillLineagePort service(HermesLearnedSkillRepository repository) {
        return repository == null ? noop() : new HermesSkillLineageServicePort(repository);
    }

    static HermesSkillLineagePort service(
            HermesLearnedSkillRepository repository,
            HermesSkillLineageRemediationPolicy remediationPolicy) {
        return repository == null ? noop() : new HermesSkillLineageServicePort(repository, remediationPolicy);
    }

    static HermesSkillLineagePort service(
            HermesLearnedSkillRepository repository,
            HermesSkillLineageRemediationPolicy remediationPolicy,
            HermesSkillLineageRepairBackendRegistry repairBackendRegistry) {
        return repository == null
                ? noop()
                : new HermesSkillLineageServicePort(repository, remediationPolicy, repairBackendRegistry);
    }

    static HermesSkillLineagePort service(
            HermesLearnedSkillRepository repository,
            HermesSkillLineageRemediationPolicy remediationPolicy,
            HermesSkillLineageRepairBackendRegistry repairBackendRegistry,
            HermesSkillLineageRepairAdapterRegistry repairAdapterRegistry) {
        return repository == null
                ? noop()
                : new HermesSkillLineageServicePort(
                        repository,
                        remediationPolicy,
                        repairBackendRegistry,
                        repairAdapterRegistry);
    }

    static HermesSkillLineagePort service(
            HermesLearnedSkillRepository repository,
            HermesSkillLineageRemediationPolicy remediationPolicy,
            HermesSkillLineageRepairBackendRegistry repairBackendRegistry,
            HermesSkillLineageRepairAdapterRegistry repairAdapterRegistry,
            HermesSkillLineageRepairApprovalStore repairApprovalStore) {
        return repository == null
                ? noop()
                : new HermesSkillLineageServicePort(
                        repository,
                        remediationPolicy,
                        repairBackendRegistry,
                        repairAdapterRegistry,
                        repairApprovalStore);
    }

    static HermesSkillLineagePort noop() {
        return new HermesSkillLineagePort() {
            @Override
            public HermesPortDispatchResult inspect(HermesSkillLineageDirective directive) {
                HermesSkillLineageDirective resolved = directive == null
                        ? HermesSkillLineageDirective.none()
                        : directive;
                return HermesPortDispatchResult.noop(
                        HermesRuntimePortCatalog.SKILL_LINEAGE,
                        resolved.operation(),
                        resolved.target(),
                        "skill lineage adapter not configured",
                        resolved.toMetadata());
            }

            @Override
            public HermesRuntimePortDescriptor descriptor() {
                return HermesRuntimePortDescriptor.noop(HermesRuntimePortCatalog.SKILL_LINEAGE);
            }
        };
    }
}
