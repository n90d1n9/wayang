package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.List;
import java.util.Optional;

/**
 * Decides when Hermes should ask memory adapters to consolidate learned context.
 */
public final class HermesMemoryReflectionResolver {

    private static final List<String> REFLECTION_KEYS = List.of(
            "hermes.memory.reflect",
            "hermes.reflection.enabled",
            "memory.reflect",
            "memoryReflection",
            "reflectMemory",
            "reflection",
            "reflection.enabled");

    private static final List<String> SCOPE_KEYS = List.of(
            "hermes.memory.scope",
            "hermes.reflection.scope",
            "memory.scope",
            "reflection.scope",
            "memoryScope",
            "reflectionScope");

    private static final List<String> CADENCE_KEYS = List.of(
            "hermes.memory.reflectionCadence",
            "hermes.reflection.cadence",
            "memory.reflectionCadence",
            "reflection.cadence",
            "memoryReflectionCadence",
            "reflectionCadence");

    private static final List<String> PRIORITY_KEYS = List.of(
            "hermes.memory.reflectionPriority",
            "hermes.reflection.priority",
            "memory.reflectionPriority",
            "reflection.priority",
            "memoryPriority",
            "reflectionPriority");

    private final HermesAgentModeConfig config;

    public HermesMemoryReflectionResolver(HermesAgentModeConfig config) {
        this.config = config == null ? HermesAgentModeConfig.defaults() : config;
    }

    public HermesMemoryReflectionPlan resolve(AgentRequest request) {
        HermesRequestValues values = HermesRequestValues.from(request);
        String prompt = values.prompt();
        Optional<Boolean> explicitReflection = values.firstBoolean(REFLECTION_KEYS, "memory reflection");
        Optional<String> explicitCadence = values.firstText(CADENCE_KEYS);
        String scope = scope(values.firstText(SCOPE_KEYS).orElseGet(() -> defaultScope(request)));
        String priority = priority(values.firstText(PRIORITY_KEYS)
                .orElseGet(() -> HermesPromptSignals.inferredMemoryPriority(prompt)));

        if (!config.persistentMemoryEnabled()) {
            return new HermesMemoryReflectionPlan(
                    false,
                    explicitReflection.orElse(false) || explicitCadence.isPresent()
                            || HermesPromptSignals.suggestsMemoryReflection(prompt),
                    false,
                    scope,
                    "none",
                    priority,
                    "disabled",
                    "persistent memory disabled");
        }

        if (explicitReflection.isPresent() && !explicitReflection.orElseThrow()) {
            return new HermesMemoryReflectionPlan(
                    true,
                    false,
                    false,
                    scope,
                    "none",
                    priority,
                    "explicit",
                    "memory reflection disabled for request");
        }

        if (explicitReflection.orElse(false)) {
            return new HermesMemoryReflectionPlan(
                    true,
                    true,
                    true,
                    scope,
                    cadence(explicitCadence.orElse("post-run")),
                    priority,
                    "explicit",
                    "explicit memory reflection requested");
        }

        if (explicitCadence.isPresent()) {
            String cadence = cadence(explicitCadence.orElseThrow());
            boolean reflect = !"none".equals(cadence);
            return new HermesMemoryReflectionPlan(
                    true,
                    reflect,
                    reflect,
                    scope,
                    cadence,
                    priority,
                    "explicit",
                    reflect ? "memory reflection cadence provided" : "memory reflection cadence disabled");
        }

        if (HermesPromptSignals.suggestsMemoryReflection(prompt)) {
            return new HermesMemoryReflectionPlan(
                    true,
                    true,
                    true,
                    scope,
                    "post-run",
                    priority,
                    "prompt",
                    "memory reflection inferred from prompt");
        }

        return new HermesMemoryReflectionPlan(
                true,
                false,
                false,
                scope,
                "none",
                priority,
                "none",
                "no memory reflection requested");
    }

    public HermesMemoryReflectionPlan defaultPlan() {
        return resolve(null);
    }

    private static String defaultScope(AgentRequest request) {
        if (request == null) {
            return "session";
        }
        if (request.sessionId() != null && !request.sessionId().isBlank()) {
            return "session";
        }
        if (request.userId() != null && !request.userId().isBlank()) {
            return "user";
        }
        return "tenant";
    }

    private static String scope(String value) {
        return switch (HermesRequestValues.normalize(value)) {
            case "global" -> "global";
            case "tenant", "workspace", "organization", "org" -> "tenant";
            case "user", "profile" -> "user";
            case "conversation", "thread", "chat" -> "conversation";
            case "agent" -> "agent";
            default -> "session";
        };
    }

    private static String cadence(String value) {
        return switch (HermesRequestValues.normalize(value)) {
            case "none", "off", "disabled", "false", "never" -> "none";
            case "periodic", "scheduled" -> "periodic";
            case "daily", "nightly", "weekly", "monthly" -> HermesRequestValues.normalize(value);
            case "ondemand", "onrequest", "manual" -> "on-demand";
            default -> "post-run";
        };
    }

    private static String priority(String value) {
        return switch (HermesRequestValues.normalize(value)) {
            case "low" -> "low";
            case "high", "critical", "important" -> "high";
            default -> "normal";
        };
    }
}
