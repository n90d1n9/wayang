package tech.kayys.gamelan.agent.routing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.util.*;

/**
 * WorkloadModelRouter — per-workflow LLM binding with five specialized model roles.
 *
 * <h2>From the OPENDEV paper (§2.2.5)</h2>
 * A core realization of the compound AI systems paradigm is that different execution phases
 * benefit from different model capabilities. Reasoning tasks benefit from extended thinking
 * without tool distraction. Visual tasks require vision-language models. Bulk summarization
 * benefits from cheaper, faster models. Using a single model for all tasks either wastes cost
 * (using expensive models for simple tasks) or sacrifices quality (using cheap models for
 * complex reasoning).
 *
 * <h2>Five model roles with fallback chains</h2>
 * <pre>
 * ACTION  — Primary execution model for tool-based reasoning. Default for all workloads.
 *           Fallback: (none — required)
 *
 * THINKING — Optional model for extended reasoning WITHOUT tool access.
 *            Prevents premature tool use: when tools are available, models act rather than think.
 *            Fallback: ACTION model.
 *
 * CRITIQUE — Optional model for self-evaluation (Reflexion-inspired, applied selectively).
 *            Used only at HIGH thinking depth — merging with HIGH simplified the UX.
 *            Fallback: THINKING → ACTION.
 *
 * VISION  — Vision-language model for processing screenshots and images.
 *           Fallback: ACTION (if vision-capable).
 *
 * COMPACT — Smaller, faster model for summarization during context compaction.
 *           Prioritizes speed and cost over reasoning depth.
 *           Fallback: ACTION model.
 * </pre>
 *
 * <h2>Key design decisions</h2>
 * <ul>
 *   <li>Lazy initialization: each role's client materializes on first use — reduces startup latency</li>
 *   <li>Model-agnostic by construction: switching providers requires only config change, not code</li>
 *   <li>Capabilities are not fixed at deployment — continuously upgradeable as better models emerge</li>
 * </ul>
 */
@ApplicationScoped
public class WorkloadModelRouter {

    private static final Logger log = LoggerFactory.getLogger(WorkloadModelRouter.class);

    @Inject GamelanConfig  config;
    @Inject AgentTelemetry telemetry;

    // Per-role model override cache (lazily populated from config)
    private final Map<ModelRole, String> overrides = new EnumMap<>(ModelRole.class);

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Returns the model identifier to use for the given workload role.
     * Applies the fallback chain if the role has no dedicated model configured.
     *
     * @param role    the cognitive workload type
     * @return model identifier string (for Gollek SDK)
     */
    public String modelFor(ModelRole role) {
        String override = overrides.get(role);
        if (override != null && !override.isBlank()) {
            telemetry.count("router.hit." + role.name().toLowerCase());
            return override;
        }

        // Apply fallback chain from paper §2.2.5
        String resolved = switch (role) {
            case ACTION   -> config.defaultModel();
            case THINKING -> overrides.getOrDefault(ModelRole.THINKING,
                             config.defaultModel());
            case CRITIQUE -> Optional.ofNullable(overrides.get(ModelRole.CRITIQUE))
                             .orElse(modelFor(ModelRole.THINKING));
            case VISION   -> Optional.ofNullable(overrides.get(ModelRole.VISION))
                             .orElse(config.defaultModel());
            case COMPACT  -> Optional.ofNullable(overrides.get(ModelRole.COMPACT))
                             .orElse(config.defaultModel());
        };

        log.debug("[router] role={} → model={} (fallback)", role, resolved);
        telemetry.count("router.fallback." + role.name().toLowerCase());
        return resolved;
    }

    /**
     * Configures a model override for a specific role.
     * Setting null or blank removes the override, reverting to fallback.
     */
    public void configure(ModelRole role, String modelId) {
        if (modelId == null || modelId.isBlank()) {
            overrides.remove(role);
            log.info("[router] cleared override for role={}", role);
        } else {
            overrides.put(role, modelId);
            log.info("[router] configured role={} → {}", role, modelId);
        }
    }

    /**
     * Returns the thinking depth configuration for the THINKING role.
     * Higher depth enables self-critique (CRITIQUE role).
     */
    public ThinkingDepth thinkingDepth() {
        String val = config.get("gamelan.thinking.depth", "MEDIUM");
        try { return ThinkingDepth.valueOf(val.toUpperCase()); }
        catch (IllegalArgumentException e) { return ThinkingDepth.MEDIUM; }
    }

    /**
     * Returns true if the CRITIQUE role should be invoked.
     * Per the paper: critique is active at HIGH depth only (merging it into HIGH simplified UX
     * since users who want deep thinking invariably benefit from critique as well).
     */
    public boolean isCritiqueEnabled() {
        return thinkingDepth() == ThinkingDepth.HIGH;
    }

    /**
     * Returns the current role→model mapping for display or config export.
     */
    public Map<ModelRole, String> currentMapping() {
        Map<ModelRole, String> mapping = new EnumMap<>(ModelRole.class);
        for (ModelRole role : ModelRole.values()) {
            mapping.put(role, modelFor(role));
        }
        return Collections.unmodifiableMap(mapping);
    }

    public String summary() {
        return currentMapping().entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .reduce("", (a, b) -> a + " | " + b).strip();
    }

    // ── Data types ─────────────────────────────────────────────────────────

    /**
     * The five cognitive workload types from OPENDEV §2.2.5.
     */
    public enum ModelRole {
        /** Primary execution: tool-based reasoning, file edits, command execution. */
        ACTION,
        /** Deliberation without tools: pre-action reasoning, strategy planning. */
        THINKING,
        /** Self-evaluation: critique of thinking traces, invoked at HIGH depth only. */
        CRITIQUE,
        /** Image and screenshot analysis: UI mockups, architecture diagrams, error screens. */
        VISION,
        /** Context compaction: summarization of older conversation history. Smaller/faster. */
        COMPACT
    }

    /**
     * Thinking depth levels (paper §2.2.6).
     * At HIGH depth, self-critique is automatically included.
     */
    public enum ThinkingDepth {
        /** No separate thinking phase; action model reasons directly. */
        OFF,
        /** Brief thinking trace — minimal overhead. */
        LOW,
        /** Standard thinking trace — balanced latency vs. quality. */
        MEDIUM,
        /** Deep deliberation + self-critique — highest quality, highest latency. */
        HIGH
    }
}
