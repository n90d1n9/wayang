package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.List;
import java.util.Optional;

/**
 * Assembles Hermes runtime ports from explicit beans, adapter profiles, and
 * persistence-backed defaults.
 */
final class HermesRuntimePortsFactory {

    private HermesRuntimePortsFactory() {
    }

    static HermesRuntimePorts create(
            Optional<HermesRuntimePorts> runtimePorts,
            Optional<HermesRuntimeAdapterRegistry> runtimeAdapterRegistry,
            HermesAgentModeConfig modeConfig,
            Optional<HermesExecutionPort> executionPort,
            Optional<HermesGatewayPort> gatewayPort,
            Optional<HermesAutomationPort> automationPort,
            Optional<HermesDelegationPort> delegationPort,
            Optional<HermesProviderRoutingPort> providerRoutingPort,
            Optional<HermesMemoryReflectionPort> memoryReflectionPort,
            Optional<HermesTrajectoryExportPort> trajectoryExportPort,
            Optional<HermesSkillPersistencePort> skillPersistencePort,
            Optional<HermesRuntimeJournalPort> runtimeJournalPort,
            Optional<HermesSkillLineagePort> skillLineagePort,
            HermesLearnedSkillRepository learnedSkillRepository,
            List<HermesSkillLineageRepairAdapter> repairAdapters,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        return create(
                runtimePorts,
                runtimeAdapterRegistry,
                modeConfig,
                executionPort,
                gatewayPort,
                automationPort,
                delegationPort,
                providerRoutingPort,
                memoryReflectionPort,
                trajectoryExportPort,
                skillPersistencePort,
                runtimeJournalPort,
                Optional.empty(),
                skillLineagePort,
                learnedSkillRepository,
                repairAdapters,
                objectStorageService,
                dataSource);
    }

    static HermesRuntimePorts create(
            Optional<HermesRuntimePorts> runtimePorts,
            Optional<HermesRuntimeAdapterRegistry> runtimeAdapterRegistry,
            HermesAgentModeConfig modeConfig,
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
            HermesLearnedSkillRepository learnedSkillRepository,
            List<HermesSkillLineageRepairAdapter> repairAdapters,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        HermesAgentModeConfig effectiveConfig = modeConfig == null ? HermesAgentModeConfig.defaults() : modeConfig;
        Optional<ObjectStorageService> effectiveObjectStorageService = HermesOptionals.orEmpty(objectStorageService);
        Optional<DataSource> effectiveDataSource = HermesOptionals.orEmpty(dataSource);
        Optional<HermesLearningAuditPort> effectiveLearningAuditPort = HermesOptionals.orEmpty(learningAuditPort)
                .or(() -> Optional.of(defaultLearningAuditPort(
                        effectiveConfig,
                        effectiveObjectStorageService,
                        effectiveDataSource)));
        Optional<HermesSkillLineagePort> effectiveSkillLineagePort = HermesOptionals.orEmpty(skillLineagePort)
                .or(() -> Optional.of(defaultSkillLineagePort(
                        effectiveConfig,
                        learnedSkillRepository,
                        repairAdapterRegistry(
                                effectiveConfig,
                                repairAdapters,
                                effectiveObjectStorageService,
                                effectiveDataSource),
                        effectiveObjectStorageService,
                        effectiveDataSource)));
        return compose(
                HermesOptionals.orEmpty(runtimePorts),
                HermesOptionals.orEmpty(runtimeAdapterRegistry),
                effectiveConfig,
                HermesOptionals.orEmpty(executionPort),
                HermesOptionals.orEmpty(gatewayPort),
                HermesOptionals.orEmpty(automationPort),
                HermesOptionals.orEmpty(delegationPort),
                HermesOptionals.orEmpty(providerRoutingPort),
                HermesOptionals.orEmpty(memoryReflectionPort),
                HermesOptionals.orEmpty(trajectoryExportPort),
                HermesOptionals.orEmpty(skillPersistencePort),
                HermesOptionals.orEmpty(runtimeJournalPort),
                effectiveLearningAuditPort,
                effectiveSkillLineagePort);
    }

    static HermesRuntimePorts compose(
            Optional<HermesRuntimePorts> runtimePorts,
            Optional<HermesExecutionPort> executionPort,
            Optional<HermesGatewayPort> gatewayPort,
            Optional<HermesAutomationPort> automationPort,
            Optional<HermesDelegationPort> delegationPort,
            Optional<HermesProviderRoutingPort> providerRoutingPort,
            Optional<HermesMemoryReflectionPort> memoryReflectionPort,
            Optional<HermesTrajectoryExportPort> trajectoryExportPort,
            Optional<HermesSkillPersistencePort> skillPersistencePort) {
        return compose(
                runtimePorts,
                Optional.empty(),
                HermesAgentModeConfig.defaults(),
                executionPort,
                gatewayPort,
                automationPort,
                delegationPort,
                providerRoutingPort,
                memoryReflectionPort,
                trajectoryExportPort,
                skillPersistencePort,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    static HermesRuntimePorts compose(
            Optional<HermesRuntimePorts> runtimePorts,
            Optional<HermesRuntimeAdapterRegistry> runtimeAdapterRegistry,
            HermesAgentModeConfig modeConfig,
            Optional<HermesExecutionPort> executionPort,
            Optional<HermesGatewayPort> gatewayPort,
            Optional<HermesAutomationPort> automationPort,
            Optional<HermesDelegationPort> delegationPort,
            Optional<HermesProviderRoutingPort> providerRoutingPort,
            Optional<HermesMemoryReflectionPort> memoryReflectionPort,
            Optional<HermesTrajectoryExportPort> trajectoryExportPort,
            Optional<HermesSkillPersistencePort> skillPersistencePort) {
        return compose(
                runtimePorts,
                runtimeAdapterRegistry,
                modeConfig,
                executionPort,
                gatewayPort,
                automationPort,
                delegationPort,
                providerRoutingPort,
                memoryReflectionPort,
                trajectoryExportPort,
                skillPersistencePort,
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
    }

    static HermesRuntimePorts compose(
            Optional<HermesRuntimePorts> runtimePorts,
            Optional<HermesRuntimeAdapterRegistry> runtimeAdapterRegistry,
            HermesAgentModeConfig modeConfig,
            Optional<HermesExecutionPort> executionPort,
            Optional<HermesGatewayPort> gatewayPort,
            Optional<HermesAutomationPort> automationPort,
            Optional<HermesDelegationPort> delegationPort,
            Optional<HermesProviderRoutingPort> providerRoutingPort,
            Optional<HermesMemoryReflectionPort> memoryReflectionPort,
            Optional<HermesTrajectoryExportPort> trajectoryExportPort,
            Optional<HermesSkillPersistencePort> skillPersistencePort,
            Optional<HermesRuntimeJournalPort> runtimeJournalPort) {
        return compose(
                runtimePorts,
                runtimeAdapterRegistry,
                modeConfig,
                executionPort,
                gatewayPort,
                automationPort,
                delegationPort,
                providerRoutingPort,
                memoryReflectionPort,
                trajectoryExportPort,
                skillPersistencePort,
                runtimeJournalPort,
                Optional.empty(),
                Optional.empty());
    }

    static HermesRuntimePorts compose(
            Optional<HermesRuntimePorts> runtimePorts,
            Optional<HermesRuntimeAdapterRegistry> runtimeAdapterRegistry,
            HermesAgentModeConfig modeConfig,
            Optional<HermesExecutionPort> executionPort,
            Optional<HermesGatewayPort> gatewayPort,
            Optional<HermesAutomationPort> automationPort,
            Optional<HermesDelegationPort> delegationPort,
            Optional<HermesProviderRoutingPort> providerRoutingPort,
            Optional<HermesMemoryReflectionPort> memoryReflectionPort,
            Optional<HermesTrajectoryExportPort> trajectoryExportPort,
            Optional<HermesSkillPersistencePort> skillPersistencePort,
            Optional<HermesRuntimeJournalPort> runtimeJournalPort,
            Optional<HermesSkillLineagePort> skillLineagePort) {
        return compose(
                runtimePorts,
                runtimeAdapterRegistry,
                modeConfig,
                executionPort,
                gatewayPort,
                automationPort,
                delegationPort,
                providerRoutingPort,
                memoryReflectionPort,
                trajectoryExportPort,
                skillPersistencePort,
                runtimeJournalPort,
                Optional.empty(),
                skillLineagePort);
    }

    static HermesRuntimePorts compose(
            Optional<HermesRuntimePorts> runtimePorts,
            Optional<HermesRuntimeAdapterRegistry> runtimeAdapterRegistry,
            HermesAgentModeConfig modeConfig,
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
            Optional<HermesSkillLineagePort> skillLineagePort) {
        if (runtimePorts != null && runtimePorts.isPresent()) {
            return runtimePorts.orElseThrow();
        }
        if (runtimeAdapterRegistry != null && runtimeAdapterRegistry.isPresent()) {
            HermesRuntimeAdapterRegistry registry = runtimeAdapterRegistry.orElseThrow();
            if (!registry.empty()) {
                return registry.resolve(modeConfig);
            }
        }
        return HermesRuntimePorts.builder()
                .executionPort(orNull(executionPort))
                .gatewayPort(orNull(gatewayPort))
                .automationPort(orNull(automationPort))
                .delegationPort(orNull(delegationPort))
                .providerRoutingPort(orNull(providerRoutingPort))
                .memoryReflectionPort(orNull(memoryReflectionPort))
                .trajectoryExportPort(orNull(trajectoryExportPort))
                .skillPersistencePort(orNull(skillPersistencePort))
                .runtimeJournalPort(orNull(runtimeJournalPort))
                .learningAuditPort(orNull(learningAuditPort))
                .skillLineagePort(orNull(skillLineagePort))
                .build();
    }

    static HermesSkillLineageRepairAdapterRegistry repairAdapterRegistry(
            HermesAgentModeConfig modeConfig,
            List<HermesSkillLineageRepairAdapter> repairAdapters,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        HermesAgentModeConfig effectiveConfig = modeConfig == null ? HermesAgentModeConfig.defaults() : modeConfig;
        HermesSkillLineageRepairAdapterRegistry.Builder builder =
                HermesSkillLineageRepairAdapterRegistry.builder()
                        .dispatchLedger(effectiveConfig, objectStorageService, dataSource);
        if (repairAdapters != null) {
            repairAdapters.forEach(builder::register);
        }
        return builder.build();
    }

    private static HermesSkillLineagePort defaultSkillLineagePort(
            HermesAgentModeConfig modeConfig,
            HermesLearnedSkillRepository learnedSkillRepository,
            HermesSkillLineageRepairAdapterRegistry repairAdapterRegistry,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        HermesAgentModeConfig effectiveConfig = modeConfig == null ? HermesAgentModeConfig.defaults() : modeConfig;
        return HermesSkillLineagePort.service(
                learnedSkillRepository,
                effectiveConfig.skillLineageRemediationPolicy(),
                effectiveConfig.skillLineageRepairBackendRegistry(),
                repairAdapterRegistry,
                HermesSkillLineageRepairApprovalStoreResolver.resolve(
                        effectiveConfig,
                        objectStorageService,
                        dataSource));
    }

    private static HermesLearningAuditPort defaultLearningAuditPort(
            HermesAgentModeConfig modeConfig,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        HermesLearningPromotionReceiptLedgerSettings settings =
                HermesLearningPromotionReceiptLedgerSettings.from(modeConfig);
        if (settings.store() == HermesPersistenceStoreKind.NOOP) {
            return HermesLearningAuditPort.noop();
        }
        HermesLearningPromotionReceiptLedger ledger = HermesLearningPromotionReceiptLedgerFactory.create(
                settings,
                HermesPersistenceResources.of(objectStorageService, dataSource));
        return HermesLearningAuditPort.service(new HermesLearningAuditService(ledger));
    }

    private static <T> T orNull(Optional<T> value) {
        return value == null ? null : value.orElse(null);
    }
}
