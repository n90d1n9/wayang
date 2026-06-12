package tech.kayys.wayang.agent.hermes;

/**
 * Applies Hermes core mode config keys to an agent mode builder.
 */
final class HermesAgentModeCoreConfigSection implements HermesConfigSection {

    static final HermesConfigSection INSTANCE = new HermesAgentModeCoreConfigSection();

    private HermesAgentModeCoreConfigSection() {
    }

    @Override
    public void apply(
            HermesConfigValues scoped,
            HermesAgentModeConfig.Builder builder) {
        scoped.booleanValue("persistent-memory-enabled", "persistent-memory", "persistentMemoryEnabled")
                .ifPresent(builder::persistentMemoryEnabled);
        scoped.booleanValue("skill-learning-enabled", "skill-learning", "skillLearningEnabled")
                .ifPresent(builder::skillLearningEnabled);
        scoped.booleanValue("skill-self-improvement-enabled", "skill-self-improvement", "skillSelfImprovementEnabled")
                .ifPresent(builder::skillSelfImprovementEnabled);
        scoped.booleanValue("mcp-enabled", "mcp", "mcpEnabled")
                .ifPresent(builder::mcpEnabled);
        scoped.booleanValue("gateway-enabled", "gateway", "gatewayEnabled")
                .ifPresent(builder::gatewayEnabled);
        scoped.booleanValue("cron-enabled", "cron", "cronEnabled")
                .ifPresent(builder::cronEnabled);
        scoped.booleanValue("sub-agents-enabled", "sub-agents", "subAgentsEnabled")
                .ifPresent(builder::subAgentsEnabled);
        scoped.booleanValue("trajectory-export-enabled", "trajectory-export", "trajectoryExportEnabled")
                .ifPresent(builder::trajectoryExportEnabled);
        scoped.booleanValue("prefer-local-providers", "prefer-local", "preferLocalProviders")
                .ifPresent(builder::preferLocalProviders);
        scoped.booleanValue("require-tool-calling", "tool-calling", "requireToolCalling")
                .ifPresent(builder::requireToolCalling);

        scoped.intValue("min-steps-to-learn", "minStepsToLearn")
                .ifPresent(builder::minStepsToLearn);
        scoped.intValue("max-skill-procedure-steps", "maxSkillProcedureSteps")
                .ifPresent(builder::maxSkillProcedureSteps);
        scoped.intValue("max-sub-agents", "maxSubAgents", "sub-agent-limit", "subAgentLimit")
                .ifPresent(builder::maxSubAgents);
        scoped.intValue("memory-entry-limit", "memoryEntryLimit")
                .ifPresent(builder::memoryEntryLimit);

        scoped.get("preferred-provider", "preferredProvider")
                .ifPresent(builder::preferredProvider);
        scoped.get("fallback-provider", "fallbackProvider")
                .ifPresent(builder::fallbackProvider);
        scoped.listValue("default-toolsets", "defaultToolsets")
                .ifPresent(builder::defaultToolsets);
        scoped.listValue("gateway-platforms", "gatewayPlatforms")
                .ifPresent(builder::gatewayPlatforms);
        scoped.listValue("execution-backends", "executionBackends")
                .ifPresent(builder::executionBackends);
        scoped.get("runtime-adapter-profile", "runtime-adapter", "runtimeAdapterProfile")
                .ifPresent(builder::runtimeAdapterProfile);
    }
}
