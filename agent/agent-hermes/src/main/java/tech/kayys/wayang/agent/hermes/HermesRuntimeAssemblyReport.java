package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.InferenceBackend;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only report describing how Hermes runtime dependencies were assembled.
 */
public record HermesRuntimeAssemblyReport(
        String inferenceBackend,
        String inferenceBackendVersion,
        String inferenceBackendClass,
        String skillManagementServiceClass,
        String memorySnapshotProviderClass,
        boolean objectStorageAvailable,
        boolean dataSourceAvailable,
        boolean runtimeEventSinkContributed,
        String runtimeAdapterProfile,
        boolean runtimePortsContributed,
        boolean runtimeAdapterRegistryContributed,
        List<String> contributedRuntimePorts,
        List<String> configuredRuntimePorts,
        int skillLineageRepairAdapterCount,
        boolean runtimeEventJournalEnabled,
        String runtimeEventJournalStore,
        String learnedSkillTargetSummary) {

    public HermesRuntimeAssemblyReport {
        inferenceBackend = HermesDirectiveSupport.clean(inferenceBackend, "unknown");
        inferenceBackendVersion = HermesDirectiveSupport.clean(inferenceBackendVersion, "unknown");
        inferenceBackendClass = HermesDirectiveSupport.clean(inferenceBackendClass, "unknown");
        skillManagementServiceClass = HermesDirectiveSupport.clean(skillManagementServiceClass, "unknown");
        memorySnapshotProviderClass = HermesDirectiveSupport.clean(memorySnapshotProviderClass, "unknown");
        runtimeAdapterProfile = HermesDirectiveSupport.clean(
                runtimeAdapterProfile,
                HermesRuntimeAdapterRegistry.DEFAULT_PROFILE);
        contributedRuntimePorts = HermesCollections.copyNonNull(contributedRuntimePorts);
        configuredRuntimePorts = HermesCollections.copyNonNull(configuredRuntimePorts);
        if (skillLineageRepairAdapterCount < 0) {
            skillLineageRepairAdapterCount = 0;
        }
        runtimeEventJournalStore = HermesDirectiveSupport.clean(runtimeEventJournalStore, "disabled");
        learnedSkillTargetSummary = HermesDirectiveSupport.clean(learnedSkillTargetSummary, "unknown");
    }

    public static HermesRuntimeAssemblyReport empty() {
        return new HermesRuntimeAssemblyReport(
                "unknown",
                "unknown",
                "unknown",
                "unknown",
                "unknown",
                false,
                false,
                false,
                HermesRuntimeAdapterRegistry.DEFAULT_PROFILE,
                false,
                false,
                List.of(),
                List.of(),
                0,
                false,
                "disabled",
                "unknown");
    }

    static HermesRuntimeAssemblyReport from(
            HermesAgentRuntimeAssemblyRequest request,
            HermesLearnedSkillRepository learnedSkillRepository,
            HermesRuntimePorts runtimePorts) {
        if (request == null) {
            return empty();
        }
        InferenceBackend backend = request.inferenceBackend();
        HermesRuntimePortContributions portContributions = request.portContributions();
        HermesRuntimePorts effectiveRuntimePorts = runtimePorts == null ? HermesRuntimePorts.noop() : runtimePorts;
        HermesAgentModeConfig config = request.config();
        return new HermesRuntimeAssemblyReport(
                backend == null ? "unknown" : backend.name(),
                backend == null ? "unknown" : backend.version(),
                className(backend),
                className(request.skillManagementService()),
                className(request.memorySnapshotProvider()),
                request.resources().objectStorageService().isPresent(),
                request.resources().dataSource().isPresent(),
                request.runtimeEventSink().isPresent(),
                config.runtimeAdapterProfile(),
                portContributions.runtimePorts().isPresent(),
                portContributions.runtimeAdapterRegistry().isPresent(),
                contributedRuntimePorts(portContributions),
                configuredRuntimePorts(effectiveRuntimePorts),
                portContributions.skillLineageRepairAdapters().size(),
                config.runtimeEventJournalEnabled(),
                config.runtimeEventJournalStore(),
                learnedSkillTargetSummary(learnedSkillRepository));
    }

    public Map<String, Object> summaryMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("inferenceBackend", inferenceBackend);
        values.put("runtimeAdapterProfile", runtimeAdapterProfile);
        values.put("objectStorageAvailable", objectStorageAvailable);
        values.put("dataSourceAvailable", dataSourceAvailable);
        values.put("runtimeEventSinkContributed", runtimeEventSinkContributed);
        values.put("runtimePortsContributed", runtimePortsContributed);
        values.put("runtimeAdapterRegistryContributed", runtimeAdapterRegistryContributed);
        values.put("contributedRuntimePortCount", contributedRuntimePorts.size());
        values.put("configuredRuntimePortCount", configuredRuntimePorts.size());
        values.put("skillLineageRepairAdapterCount", skillLineageRepairAdapterCount);
        values.put("runtimeEventJournalEnabled", runtimeEventJournalEnabled);
        values.put("runtimeEventJournalStore", runtimeEventJournalStore);
        values.put("learnedSkillTargetSummary", learnedSkillTargetSummary);
        return Map.copyOf(values);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>(summaryMetadata());
        values.put("inferenceBackendVersion", inferenceBackendVersion);
        values.put("inferenceBackendClass", inferenceBackendClass);
        values.put("skillManagementServiceClass", skillManagementServiceClass);
        values.put("memorySnapshotProviderClass", memorySnapshotProviderClass);
        values.put("contributedRuntimePorts", contributedRuntimePorts);
        values.put("configuredRuntimePorts", configuredRuntimePorts);
        return Map.copyOf(values);
    }

    private static List<String> contributedRuntimePorts(HermesRuntimePortContributions contributions) {
        HermesRuntimePortContributions effective = contributions == null
                ? HermesRuntimePortContributions.empty()
                : contributions;
        List<String> ports = new ArrayList<>();
        if (effective.executionPort().isPresent()) {
            ports.add(HermesRuntimePortCatalog.EXECUTION);
        }
        if (effective.gatewayPort().isPresent()) {
            ports.add(HermesRuntimePortCatalog.GATEWAY);
        }
        if (effective.automationPort().isPresent()) {
            ports.add(HermesRuntimePortCatalog.AUTOMATION);
        }
        if (effective.delegationPort().isPresent()) {
            ports.add(HermesRuntimePortCatalog.DELEGATION);
        }
        if (effective.providerRoutingPort().isPresent()) {
            ports.add(HermesRuntimePortCatalog.PROVIDER_ROUTING);
        }
        if (effective.memoryReflectionPort().isPresent()) {
            ports.add(HermesRuntimePortCatalog.MEMORY_REFLECTION);
        }
        if (effective.trajectoryExportPort().isPresent()) {
            ports.add(HermesRuntimePortCatalog.TRAJECTORY_EXPORT);
        }
        if (effective.skillPersistencePort().isPresent()) {
            ports.add(HermesRuntimePortCatalog.SKILL_PERSISTENCE);
        }
        if (effective.runtimeJournalPort().isPresent()) {
            ports.add(HermesRuntimePortCatalog.RUNTIME_JOURNAL);
        }
        if (effective.learningAuditPort().isPresent()) {
            ports.add(HermesRuntimePortCatalog.LEARNING_AUDIT);
        }
        if (effective.skillLineagePort().isPresent()) {
            ports.add(HermesRuntimePortCatalog.SKILL_LINEAGE);
        }
        return List.copyOf(ports);
    }

    private static List<String> configuredRuntimePorts(HermesRuntimePorts runtimePorts) {
        HermesRuntimePorts effective = runtimePorts == null ? HermesRuntimePorts.noop() : runtimePorts;
        return effective.descriptors().stream()
                .filter(HermesRuntimePortDescriptor::configured)
                .map(HermesRuntimePortDescriptor::port)
                .toList();
    }

    private static String learnedSkillTargetSummary(HermesLearnedSkillRepository learnedSkillRepository) {
        return learnedSkillRepository == null ? "unknown" : learnedSkillRepository.targetPlan().targetSummary();
    }

    private static String className(Object value) {
        return value == null ? "unknown" : value.getClass().getName();
    }
}
