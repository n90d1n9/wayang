package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Typed runtime capability view for Hermes mode.
 */
public record HermesRuntimeCapabilities(
        boolean supportsPersistentMemory,
        boolean supportsSkillLearning,
        boolean supportsSkillSelfImprovement,
        boolean supportsMcpTooling,
        boolean supportsGateway,
        boolean supportsCron,
        boolean supportsSubAgents,
        boolean supportsTrajectoryExport,
        boolean prefersLocalProviders,
        boolean requiresToolCalling,
        List<String> enabledFeatures,
        List<String> disabledFeatures,
        List<String> defaultToolsets,
        List<String> gatewayPlatforms,
        List<String> executionBackends,
        Map<String, String> persistenceHints) {

    public HermesRuntimeCapabilities {
        enabledFeatures = enabledFeatures == null ? List.of() : List.copyOf(enabledFeatures);
        disabledFeatures = disabledFeatures == null ? List.of() : List.copyOf(disabledFeatures);
        defaultToolsets = defaultToolsets == null ? List.of() : List.copyOf(defaultToolsets);
        gatewayPlatforms = gatewayPlatforms == null ? List.of() : List.copyOf(gatewayPlatforms);
        executionBackends = executionBackends == null ? List.of() : List.copyOf(executionBackends);
        persistenceHints = persistenceHints == null ? Map.of() : Map.copyOf(persistenceHints);
    }

    public static HermesRuntimeCapabilities from(HermesAgentModeConfig config) {
        HermesAgentModeConfig effective = config == null ? HermesAgentModeConfig.defaults() : config;
        List<String> enabledFeatures = enabledFeatures(effective);
        return new HermesRuntimeCapabilities(
                effective.persistentMemoryEnabled(),
                effective.skillLearningEnabled(),
                effective.skillLearningEnabled() && effective.skillSelfImprovementEnabled(),
                effective.mcpEnabled(),
                effective.gatewayEnabled(),
                effective.cronEnabled(),
                effective.subAgentsEnabled(),
                effective.trajectoryExportEnabled(),
                effective.preferLocalProviders(),
                effective.requireToolCalling(),
                enabledFeatures,
                HermesAgentMode.features().stream()
                        .filter(feature -> !enabledFeatures.contains(feature))
                        .toList(),
                effectiveToolsets(effective),
                effective.gatewayEnabled() ? effective.gatewayPlatforms() : List.of(),
                effective.executionBackends(),
                effective.persistenceHints());
    }

    public boolean supportsFeature(String feature) {
        String normalized = normalize(feature);
        return enabledFeatures.stream().anyMatch(value -> normalize(value).equals(normalized));
    }

    public boolean supportsGatewayPlatform(String platform) {
        if (!supportsGateway) {
            return false;
        }
        String normalized = normalize(platform);
        return gatewayPlatforms.stream().anyMatch(value -> normalize(value).equals(normalized));
    }

    public boolean supportsExecutionBackend(String backend) {
        String normalized = normalize(backend);
        return executionBackends.stream().anyMatch(value -> normalize(value).equals(normalized));
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("supportsPersistentMemory", supportsPersistentMemory);
        metadata.put("supportsSkillLearning", supportsSkillLearning);
        metadata.put("supportsSkillSelfImprovement", supportsSkillSelfImprovement);
        metadata.put("supportsMcpTooling", supportsMcpTooling);
        metadata.put("supportsGateway", supportsGateway);
        metadata.put("supportsCron", supportsCron);
        metadata.put("supportsSubAgents", supportsSubAgents);
        metadata.put("supportsTrajectoryExport", supportsTrajectoryExport);
        metadata.put("prefersLocalProviders", prefersLocalProviders);
        metadata.put("requiresToolCalling", requiresToolCalling);
        metadata.put("enabledFeatures", enabledFeatures);
        metadata.put("disabledFeatures", disabledFeatures);
        metadata.put("defaultToolsets", defaultToolsets);
        metadata.put("gatewayPlatforms", gatewayPlatforms);
        metadata.put("executionBackends", executionBackends);
        metadata.put("persistenceHints", persistenceHints);
        metadata.put("skillPersistenceStrategy", skillPersistenceStrategy().toMetadata());
        return Map.copyOf(metadata);
    }

    public HermesSkillPersistenceStrategy skillPersistenceStrategy() {
        return HermesSkillPersistenceStrategy.fromHints(persistenceHints);
    }

    private static List<String> enabledFeatures(HermesAgentModeConfig config) {
        Map<String, Boolean> flags = new LinkedHashMap<>();
        flags.put("persistent-memory", config.persistentMemoryEnabled());
        flags.put("autonomous-skill-creation", config.skillLearningEnabled());
        flags.put("skill-self-improvement", config.skillLearningEnabled() && config.skillSelfImprovementEnabled());
        flags.put("mcp-tooling", config.mcpEnabled());
        flags.put("gateway-continuity", config.gatewayEnabled());
        flags.put("cron-automation", config.cronEnabled());
        flags.put("parallel-subagents", config.subAgentsEnabled());
        flags.put("execution-backend-routing", !config.executionBackends().isEmpty());
        flags.put("trajectory-export", config.trajectoryExportEnabled());
        return flags.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static List<String> effectiveToolsets(HermesAgentModeConfig config) {
        return config.defaultToolsets().stream()
                .filter(toolset -> config.persistentMemoryEnabled() || !"memory".equals(normalize(toolset)))
                .filter(toolset -> config.mcpEnabled() || !"mcp".equals(normalize(toolset)))
                .toList();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT)
                .replace('_', '-')
                .trim();
    }
}
