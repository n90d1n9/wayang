package tech.kayys.wayang.agent.hermes;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.registry.BackendRegistry;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Optional CDI producer for Hermes mode.
 *
 * <p>When {@code agent-hermes} is present, it contributes a {@code hermes-agent}
 * orchestrator to the generic {@code AgentClient} producer. The learning loop
 * uses an existing {@link SkillManagementService} bean when available, or builds
 * one from the active {@link SkillRegistry}.</p>
 */
@ApplicationScoped
public class HermesAgentModeProducer {

    private static final Logger LOG = Logger.getLogger(HermesAgentModeProducer.class);

    @Produces
    @Singleton
    public HermesAgentOrchestrator hermesAgentOrchestrator(
            Instance<InferenceBackend> inferenceBackends,
            Instance<SkillManagementService> skillManagementServices,
            Instance<SkillRegistry> skillRegistries,
            Instance<ObjectStorageService> objectStorageServices,
            Instance<DataSource> dataSources,
            Instance<SkillManagementServiceConfig> skillManagementConfigs,
            Instance<HermesMemorySnapshotProvider> memorySnapshotProviders,
            Instance<HermesAgentModeConfig> modeConfigs,
            Instance<HermesRuntimeEventSink> runtimeEventSinks,
            Instance<HermesRuntimePorts> runtimePorts,
            Instance<HermesRuntimeAdapterRegistry> runtimeAdapterRegistries,
            Instance<HermesExecutionPort> executionPorts,
            Instance<HermesGatewayPort> gatewayPorts,
            Instance<HermesAutomationPort> automationPorts,
            Instance<HermesDelegationPort> delegationPorts,
            Instance<HermesProviderRoutingPort> providerRoutingPorts,
            Instance<HermesMemoryReflectionPort> memoryReflectionPorts,
            Instance<HermesTrajectoryExportPort> trajectoryExportPorts,
            Instance<HermesSkillPersistencePort> skillPersistencePorts,
            Instance<HermesRuntimeJournalPort> runtimeJournalPorts,
            Instance<HermesLearningAuditPort> learningAuditPorts,
            Instance<HermesSkillLineagePort> skillLineagePorts,
            Instance<HermesSkillLineageRepairAdapter> skillLineageRepairAdapters) {
        InferenceBackend inferenceBackend = first(inferenceBackends)
                .orElseGet(this::defaultInferenceBackend);
        HermesMemorySnapshotProvider memorySnapshotProvider = first(memorySnapshotProviders)
                .orElseGet(HermesMemorySnapshotProvider::none);
        HermesAgentModeConfig modeConfig = first(modeConfigs)
                .orElseGet(HermesAgentModeConfigs::fromRuntime);
        Optional<ObjectStorageService> objectStorageService = first(objectStorageServices);
        Optional<DataSource> dataSource = first(dataSources);
        SkillManagementService skillManagementService = HermesSkillManagementServiceResolver.resolve(
                first(skillManagementServices),
                first(skillRegistries),
                first(skillManagementConfigs),
                objectStorageService,
                dataSource);

        return HermesAgentOrchestratorFactory.create(
                HermesAgentRuntimeAssemblyRequest.builder()
                        .inferenceBackend(inferenceBackend)
                        .skillManagementService(skillManagementService)
                        .memorySnapshotProvider(memorySnapshotProvider)
                        .config(modeConfig)
                        .resources(HermesPersistenceResources.of(objectStorageService, dataSource))
                        .portContributions(new HermesRuntimePortContributions(
                                first(runtimePorts),
                                first(runtimeAdapterRegistries),
                                first(executionPorts),
                                first(gatewayPorts),
                                first(automationPorts),
                                first(delegationPorts),
                                first(providerRoutingPorts),
                                first(memoryReflectionPorts),
                                first(trajectoryExportPorts),
                                first(skillPersistencePorts),
                                first(runtimeJournalPorts),
                                first(learningAuditPorts),
                                first(skillLineagePorts),
                                all(skillLineageRepairAdapters)))
                        .runtimeEventSink(first(runtimeEventSinks))
                        .build());
    }

    @Produces
    @Singleton
    public HermesRuntimeJournalService hermesRuntimeJournalService(
            Instance<HermesAgentModeConfig> modeConfigs,
            Instance<HermesRuntimeEventSink> runtimeEventSinks,
            Instance<ObjectStorageService> objectStorageServices,
            Instance<DataSource> dataSources) {
        HermesAgentModeConfig modeConfig = first(modeConfigs)
                .orElseGet(HermesAgentModeConfigs::fromRuntime);
        return HermesRuntimeJournalService.fromSink(
                HermesRuntimeEventSinkFactory.create(
                        modeConfig,
                        first(runtimeEventSinks),
                        first(objectStorageServices),
                        first(dataSources)));
    }

    @Produces
    @Singleton
    public HermesRuntimeJournalPort hermesRuntimeJournalPort(HermesRuntimeJournalService journalService) {
        return HermesRuntimeJournalPort.service(journalService);
    }

    @Produces
    @Singleton
    public HermesLearningAuditService hermesLearningAuditService(
            Instance<HermesAgentModeConfig> modeConfigs,
            Instance<ObjectStorageService> objectStorageServices,
            Instance<DataSource> dataSources) {
        HermesAgentModeConfig modeConfig = first(modeConfigs)
                .orElseGet(HermesAgentModeConfigs::fromRuntime);
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedgerResolver.resolve(
                modeConfig,
                first(objectStorageServices),
                first(dataSources));
        return new HermesLearningAuditService(ledger);
    }

    @Produces
    @Singleton
    public HermesLearningAuditPort hermesLearningAuditPort(HermesLearningAuditService learningAuditService) {
        return HermesLearningAuditPort.service(learningAuditService);
    }

    @Produces
    @Singleton
    public HermesRuntimeDiagnosticsPort hermesRuntimeDiagnosticsPort(HermesAgentOrchestrator orchestrator) {
        if (orchestrator == null) {
            return HermesRuntimeDiagnosticsPort.service(null);
        }
        return HermesRuntimeDiagnosticsPort.service(
                orchestrator.runtimeDiagnostics(),
                () -> Map.of(
                        HermesMetadataKeys.METADATA_LEARNING_AUDIT_RETENTION_OBSERVATION,
                        orchestrator.learningAuditRetentionObservation().toMetadata()));
    }

    private InferenceBackend defaultInferenceBackend() {
        try {
            BackendRegistry.initialize();
            return BackendRegistry.getDefaultInferenceBackend();
        } catch (RuntimeException error) {
            LOG.debugf(error, "Unable to resolve default inference backend for Hermes mode");
            throw error;
        }
    }

    private static <T> Optional<T> first(Instance<T> values) {
        if (values == null) {
            return Optional.empty();
        }
        return values.stream().findFirst();
    }

    private static <T> List<T> all(Instance<T> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream().toList();
    }

}
