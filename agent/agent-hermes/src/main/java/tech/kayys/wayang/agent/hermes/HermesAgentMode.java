package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentType;
import tech.kayys.wayang.agent.spi.OrchestrationStrategy;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Stable identifiers for the Wayang Hermes Agent mode.
 */
public final class HermesAgentMode {

    public static final String MODE_ID = "hermes-agent";
    public static final String CONTEXT_MODE_KEY = "agentMode";
    public static final String CONTEXT_FEATURES_KEY = "hermesFeatures";
    public static final String PARAM_LEARN_KEY = "hermes.learn";
    public static final String PARAM_SKIP_LEARN_KEY = "hermes.skipLearning";
    public static final String LEARNED_SKILL_CATEGORY = "HERMES_LEARNED";

    private static final List<String> FEATURES = List.of(
            "persistent-memory",
            "autonomous-skill-creation",
            "skill-self-improvement",
            "mcp-tooling",
            "gateway-continuity",
            "cron-automation",
            "parallel-subagents",
            "execution-backend-routing",
            "trajectory-export");

    private HermesAgentMode() {
    }

    public static HermesAgentModeDescriptor descriptor() {
        return descriptor(HermesAgentModeConfig.defaults());
    }

    public static HermesAgentModeDescriptor descriptor(HermesAgentModeConfig config) {
        HermesAgentModeConfig effective = config == null ? HermesAgentModeConfig.defaults() : config;
        HermesRuntimeCapabilities capabilities = effective.runtimeCapabilities();
        return new HermesAgentModeDescriptor(
                MODE_ID,
                AgentType.HERMES,
                OrchestrationStrategy.HERMES_AGENT,
                capabilities.enabledFeatures(),
                capabilities.defaultToolsets(),
                capabilities.executionBackends());
    }

    public static boolean matches(AgentRequest request) {
        if (request == null) {
            return false;
        }
        if (request.strategy() == OrchestrationStrategy.HERMES_AGENT) {
            return true;
        }
        return containsMode(request.context()) || containsMode(request.parameters());
    }

    public static List<String> features() {
        return FEATURES;
    }

    public static HermesRuntimeCapabilities capabilities(HermesAgentModeConfig config) {
        return HermesRuntimeCapabilities.from(config);
    }

    private static boolean containsMode(Map<String, ?> values) {
        Object value = values == null ? null : values.get(CONTEXT_MODE_KEY);
        return value instanceof String text && MODE_ID.equals(text.toLowerCase(Locale.ROOT).trim());
    }
}
