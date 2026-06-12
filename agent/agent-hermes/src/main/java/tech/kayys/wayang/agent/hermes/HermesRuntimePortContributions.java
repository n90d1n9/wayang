package tech.kayys.wayang.agent.hermes;

import java.util.List;
import java.util.Optional;

/**
 * CDI-discovered runtime port beans available to Hermes runtime assembly.
 */
record HermesRuntimePortContributions(
        Optional<HermesRuntimePorts> runtimePorts,
        Optional<HermesRuntimeAdapterRegistry> runtimeAdapterRegistry,
        Optional<HermesExecutionPort> executionPort,
        Optional<HermesGatewayPort> gatewayPort,
        Optional<HermesAutomationPort> automationPort,
        Optional<HermesDelegationPort> delegationPort,
        Optional<HermesProviderRoutingPort> providerRoutingPort,
        Optional<HermesMemoryReflectionPort> memoryReflectionPort,
        Optional<HermesTrajectoryExportPort> trajectoryExportPort,
        Optional<HermesSkillPersistencePort> skillPersistencePort,
        Optional<HermesRuntimeJournalPort> runtimeJournalPort,
        Optional<HermesLearningAuditPort> learningAuditPort,
        Optional<HermesSkillLineagePort> skillLineagePort,
        List<HermesSkillLineageRepairAdapter> skillLineageRepairAdapters) {

    HermesRuntimePortContributions {
        runtimePorts = HermesOptionals.orEmpty(runtimePorts);
        runtimeAdapterRegistry = HermesOptionals.orEmpty(runtimeAdapterRegistry);
        executionPort = HermesOptionals.orEmpty(executionPort);
        gatewayPort = HermesOptionals.orEmpty(gatewayPort);
        automationPort = HermesOptionals.orEmpty(automationPort);
        delegationPort = HermesOptionals.orEmpty(delegationPort);
        providerRoutingPort = HermesOptionals.orEmpty(providerRoutingPort);
        memoryReflectionPort = HermesOptionals.orEmpty(memoryReflectionPort);
        trajectoryExportPort = HermesOptionals.orEmpty(trajectoryExportPort);
        skillPersistencePort = HermesOptionals.orEmpty(skillPersistencePort);
        runtimeJournalPort = HermesOptionals.orEmpty(runtimeJournalPort);
        learningAuditPort = HermesOptionals.orEmpty(learningAuditPort);
        skillLineagePort = HermesOptionals.orEmpty(skillLineagePort);
        skillLineageRepairAdapters = skillLineageRepairAdapters == null
                ? List.of()
                : List.copyOf(skillLineageRepairAdapters);
    }

    static HermesRuntimePortContributions empty() {
        return new HermesRuntimePortContributions(
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                List.of());
    }
}
