package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Optional;

/**
 * Decides when Hermes should ask the orchestration layer for isolated sub-agents.
 */
public final class HermesDelegationPlanner {

    private static final List<String> ENABLED_KEYS = List.of(
            "hermes.delegation.enabled",
            "delegation.enabled",
            "subAgents.enabled",
            "subagents.enabled",
            "parallelSubAgents.enabled",
            "delegate");

    private static final List<String> COUNT_KEYS = List.of(
            "hermes.subAgents.count",
            "hermes.delegation.count",
            "subAgents.count",
            "subAgentCount",
            "parallelism",
            "fanout",
            "workers");

    private static final List<String> LANES_KEYS = List.of(
            "hermes.subAgents.lanes",
            "hermes.delegation.lanes",
            "subAgents.lanes",
            "delegation.lanes",
            "lanes");

    private static final List<String> ISOLATION_KEYS = List.of(
            "hermes.subAgents.isolationMode",
            "hermes.delegation.isolationMode",
            "subAgents.isolationMode",
            "delegation.isolationMode",
            "isolationMode");

    private final HermesAgentModeConfig config;

    public HermesDelegationPlanner(HermesAgentModeConfig config) {
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
    }

    public HermesDelegationPlan plan(AgentRequest request) {
        HermesRequestValues values = HermesRequestValues.from(request);
        String prompt = values.prompt();
        Optional<Boolean> explicitEnabled = values.firstBoolean(ENABLED_KEYS, "delegation");
        Optional<Integer> explicitCount = values.firstInt(COUNT_KEYS, "delegation");
        List<String> explicitLanes = values.firstList(LANES_KEYS);
        String isolationMode = values.firstText(ISOLATION_KEYS).orElse("context-isolated");

        if (!config.subAgentsEnabled()) {
            List<String> lanes = explicitLanes.isEmpty() ? HermesPromptSignals.inferredDelegationLanes(prompt) : explicitLanes;
            return new HermesDelegationPlan(
                    false,
                    explicitEnabled.orElse(false) || explicitCount.orElse(0) > 1 || lanes.size() > 1,
                    false,
                    0,
                    config.maxSubAgents(),
                    lanes,
                    "none",
                    "disabled",
                    "sub-agent delegation disabled");
        }

        if (explicitEnabled.isPresent() && !explicitEnabled.orElseThrow()) {
            return new HermesDelegationPlan(
                    true,
                    false,
                    false,
                    0,
                    config.maxSubAgents(),
                    explicitLanes,
                    "none",
                    "explicit",
                    "delegation disabled for request");
        }

        if (explicitEnabled.orElse(false) || explicitCount.isPresent() || explicitLanes.size() > 1) {
            int requestedCount = explicitCount.orElseGet(() -> Math.max(2, explicitLanes.size()));
            int suggested = clamp(requestedCount);
            List<String> lanes = explicitLanes.isEmpty()
                    ? defaultLanes(suggested)
                    : limit(explicitLanes, suggested);
            return new HermesDelegationPlan(
                    true,
                    true,
                    suggested > 1,
                    suggested,
                    config.maxSubAgents(),
                    lanes,
                    isolationMode,
                    "explicit",
                    requestedCount > suggested
                            ? "explicit delegation requested; clamped to configured maximum"
                            : "explicit delegation requested");
        }

        if (HermesPromptSignals.suggestsDelegation(prompt)) {
            List<String> lanes = HermesPromptSignals.inferredDelegationLanes(prompt);
            int suggested = clamp(Math.max(2, lanes.size()));
            return new HermesDelegationPlan(
                    true,
                    true,
                    suggested > 1,
                    suggested,
                    config.maxSubAgents(),
                    limit(lanes, suggested),
                    isolationMode,
                    "prompt",
                    "parallel workstream inferred from prompt");
        }

        return new HermesDelegationPlan(
                true,
                false,
                false,
                0,
                config.maxSubAgents(),
                List.of(),
                "none",
                "none",
                "no delegation requested");
    }

    public HermesDelegationPlan defaultPlan() {
        return plan(null);
    }

    private static List<String> defaultLanes(int count) {
        List<String> defaults = List.of("analysis", "execution", "verification", "review");
        return defaults.subList(0, Math.min(Math.max(count, 0), defaults.size()));
    }

    private int clamp(int requested) {
        return Math.min(Math.max(requested, 0), config.maxSubAgents());
    }

    private static List<String> limit(List<String> lanes, int limit) {
        if (lanes.isEmpty() || limit <= 0) {
            return List.of();
        }
        return List.copyOf(lanes.subList(0, Math.min(limit, lanes.size())));
    }
}
