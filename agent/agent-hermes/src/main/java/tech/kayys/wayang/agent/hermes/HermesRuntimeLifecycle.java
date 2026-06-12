package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact lifecycle view for always-on Hermes runtime concerns.
 */
public record HermesRuntimeLifecycle(
        String phase,
        boolean ready,
        boolean runtimePortsReady,
        boolean skillPersistenceReady,
        boolean backgroundWorkEnabled,
        boolean sessionContinuityEnabled,
        boolean gatewayContinuityEnabled,
        boolean automationEnabled,
        boolean skillLearningEnabled,
        boolean runtimeJournalEnabled,
        List<String> enabledLoops,
        List<String> disabledLoops,
        List<String> attention) {

    public HermesRuntimeLifecycle {
        phase = HermesText.trimOr(phase, ready ? "ready" : "degraded");
        enabledLoops = enabledLoops == null ? List.of() : List.copyOf(enabledLoops);
        disabledLoops = disabledLoops == null ? List.of() : List.copyOf(disabledLoops);
        attention = HermesCollections.copyNonNull(attention);
    }

    public static HermesRuntimeLifecycle from(
            HermesAgentModeConfig config,
            HermesRuntimePorts runtimePorts,
            HermesLearnedSkillPersistencePreflightReport skillPersistencePreflight,
            List<String> attention) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        boolean portsReady = runtimePortsReady(runtimePorts);
        boolean persistenceReady = skillPersistencePreflight == null || skillPersistencePreflight.ready();
        Map<String, Boolean> loops = loops(effectiveConfig);
        List<String> enabledLoops = selectedLoops(loops, true);
        return new HermesRuntimeLifecycle(
                phase(portsReady, persistenceReady),
                portsReady && persistenceReady,
                portsReady,
                persistenceReady,
                !enabledLoops.isEmpty(),
                effectiveConfig.persistentMemoryEnabled() || effectiveConfig.runtimeEventJournalEnabled(),
                effectiveConfig.gatewayEnabled(),
                effectiveConfig.cronEnabled(),
                effectiveConfig.skillLearningEnabled(),
                effectiveConfig.runtimeEventJournalEnabled(),
                enabledLoops,
                selectedLoops(loops, false),
                attention);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("phase", phase);
        metadata.put("ready", ready);
        metadata.put("runtimePortsReady", runtimePortsReady);
        metadata.put("skillPersistenceReady", skillPersistenceReady);
        metadata.put("backgroundWorkEnabled", backgroundWorkEnabled);
        metadata.put("sessionContinuityEnabled", sessionContinuityEnabled);
        metadata.put("gatewayContinuityEnabled", gatewayContinuityEnabled);
        metadata.put("automationEnabled", automationEnabled);
        metadata.put("skillLearningEnabled", skillLearningEnabled);
        metadata.put("runtimeJournalEnabled", runtimeJournalEnabled);
        metadata.put("enabledLoops", enabledLoops);
        metadata.put("disabledLoops", disabledLoops);
        metadata.put("attention", attention);
        return Map.copyOf(metadata);
    }

    private static boolean runtimePortsReady(HermesRuntimePorts runtimePorts) {
        HermesRuntimePorts ports = runtimePorts == null ? HermesRuntimePorts.noop() : runtimePorts;
        return ports.descriptors().stream().allMatch(HermesRuntimePortDescriptor::ready);
    }

    private static String phase(boolean runtimePortsReady, boolean skillPersistenceReady) {
        if (runtimePortsReady && skillPersistenceReady) {
            return "ready";
        }
        if (runtimePortsReady || skillPersistenceReady) {
            return "degraded";
        }
        return "unavailable";
    }

    private static Map<String, Boolean> loops(HermesAgentModeConfig config) {
        Map<String, Boolean> loops = new LinkedHashMap<>();
        loops.put("gateway-continuity", config.gatewayEnabled());
        loops.put("cron-automation", config.cronEnabled());
        loops.put(HermesRuntimePortCatalog.MEMORY_REFLECTION, config.persistentMemoryEnabled());
        loops.put("skill-learning", config.skillLearningEnabled());
        loops.put("skill-self-improvement", config.skillLearningEnabled() && config.skillSelfImprovementEnabled());
        loops.put(HermesRuntimePortCatalog.RUNTIME_JOURNAL, config.runtimeEventJournalEnabled());
        loops.put("sub-agent-supervision", config.subAgentsEnabled());
        return loops;
    }

    private static List<String> selectedLoops(Map<String, Boolean> loops, boolean enabled) {
        return loops.entrySet().stream()
                .filter(entry -> entry.getValue() == enabled)
                .map(Map.Entry::getKey)
                .toList();
    }
}
