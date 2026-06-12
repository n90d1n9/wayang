package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.core.core.BackendBackedAgentOrchestrator;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;

import java.util.Objects;
import java.util.Optional;

/**
 * Assembles the complete Hermes agent orchestrator runtime graph.
 */
final class HermesAgentOrchestratorFactory {

    private HermesAgentOrchestratorFactory() {
    }

    static HermesAgentOrchestrator create(HermesAgentRuntimeAssemblyRequest request) {
        HermesAgentRuntimeAssemblyRequest effectiveRequest = Objects.requireNonNull(request, "request");
        HermesAgentModeConfig config = effectiveRequest.config();
        HermesPersistenceResources resources = effectiveRequest.resources();
        HermesRuntimePortContributions portContributions = effectiveRequest.portContributions();
        HermesRuntimeEventSink runtimeEventSink = HermesRuntimeEventSinkFactory.create(
                config,
                effectiveRequest.runtimeEventSink(),
                resources);
        HermesLearningPromotionReceiptLedger receiptLedger =
                HermesLearningLoopFactory.receiptLedger(config, resources);
        HermesLearningAuditService learningAuditService = new HermesLearningAuditService(receiptLedger);
        Optional<HermesLearningAuditPort> learningAuditPort =
                portContributions.learningAuditPort().or(() -> defaultLearningAuditPort(config, learningAuditService));
        HermesLearnedSkillRepository learnedSkillRepository = HermesLearnedSkillRepositoryFactory.createWithResources(
                effectiveRequest.skillManagementService(),
                config,
                resources,
                new HermesSkillMarkdownRenderer());
        HermesRuntimePorts runtimePorts = HermesRuntimePortsFactory.create(
                portContributions.runtimePorts(),
                portContributions.runtimeAdapterRegistry(),
                config,
                portContributions.executionPort(),
                portContributions.gatewayPort(),
                portContributions.automationPort(),
                portContributions.delegationPort(),
                portContributions.providerRoutingPort(),
                portContributions.memoryReflectionPort(),
                portContributions.trajectoryExportPort(),
                portContributions.skillPersistencePort(),
                portContributions.runtimeJournalPort(),
                learningAuditPort,
                portContributions.skillLineagePort(),
                learnedSkillRepository,
                portContributions.skillLineageRepairAdapters(),
                resources.objectStorageService(),
                resources.dataSource());
        HermesRuntimeDiagnostics runtimeDiagnostics = HermesRuntimeDiagnostics.from(
                config,
                runtimePorts,
                resources.objectStorageService(),
                resources.dataSource(),
                HermesRuntimeAssemblyReport.from(
                        effectiveRequest,
                        learnedSkillRepository,
                        runtimePorts));
        return new HermesAgentOrchestrator(
                new BackendBackedAgentOrchestrator(
                        OrchestrationStrategy.REACT.id,
                        effectiveRequest.inferenceBackend()),
                config,
                effectiveRequest.memorySnapshotProvider(),
                HermesLearningLoopFactory.create(
                        learnedSkillRepository,
                        new HermesSkillDistiller(),
                        config,
                        new HermesLearningSignalFactory(),
                        new HermesSkillReusePolicy(),
                        receiptLedger),
                runtimePorts,
                runtimeEventSink,
                runtimeDiagnostics,
                new HermesLearningAuditRetentionObserver(
                        learningAuditService,
                        new HermesLearningAuditRetentionEventMonitor(runtimeEventSink)));
    }

    private static Optional<HermesLearningAuditPort> defaultLearningAuditPort(
            HermesAgentModeConfig config,
            HermesLearningAuditService learningAuditService) {
        HermesLearningPromotionReceiptLedgerSettings settings =
                HermesLearningPromotionReceiptLedgerSettings.from(config);
        return settings.store() == HermesPersistenceStoreKind.NOOP
                ? Optional.empty()
                : Optional.of(HermesLearningAuditPort.service(learningAuditService));
    }
}
