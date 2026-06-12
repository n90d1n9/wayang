package tech.kayys.gamelan.agent.scaffolding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.PromptBuilder;
import tech.kayys.gamelan.agent.routing.WorkloadModelRouter;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.context.prompt.ConditionalPromptComposer;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.skill.SkillRegistry;
import tech.kayys.gamelan.skill.discovery.SkillDiscoveryEngine;
import tech.kayys.gamelan.tool.BuiltInTools;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * AgentScaffoldingFactory — eager agent construction before the first prompt arrives.
 *
 * <h2>From the OPENDEV paper (§2.2.1 — Agent Scaffolding)</h2>
 * Before the agent can process user prompts, it must be fully assembled. Every agent in
 * OPENDEV is constructed (system prompt compiled, tool schemas built, subagents registered)
 * before the conversation lifecycle begins.
 *
 * <p>The critical design choice is <b>eager construction</b>: BaseAgent.__init__() calls both
 * build_system_prompt() and build_tool_schemas() before the constructor returns. By the time
 * __init__() completes, the agent is fully ready to serve requests, with no lazy prompt assembly,
 * no first-call latency.
 *
 * <h2>Why eager construction? (Paper §2.2.1 — Design evolution)</h2>
 * Lazy prompt building (constructing the system prompt on the first run_sync call) was replaced
 * by the eager-build pattern. The lazy approach introduced first-call latency visible to the
 * user and caused race conditions with MCP server discovery: tools registered after the first
 * call would not appear in the prompt until a manual refresh. Eager building guarantees that
 * every agent is complete at construction time.
 *
 * <h2>Three-phase factory assembly (paper §2.2.1)</h2>
 * <pre>
 * Phase 1 (Skills): Discover skill definitions, register SkillLoader.
 * Phase 2 (Subagents): Compile builtin + custom subagent specs, register SubAgentManager.
 * Phase 3 (Main agent): Construct MainAgent with full tool access (includes Phase 1+2 tools).
 * </pre>
 * The ordering constraint is essential: Phase 2 must complete before Phase 3 because the
 * spawn_subagent tool description is dynamically built from the set of registered agents.
 *
 * <h2>AgentSuite</h2>
 * The factory returns an AgentSuite bundling the assembled agent configuration, timing metadata,
 * and a refresh() method that re-invokes all build phases when the tool registry changes.
 */
@ApplicationScoped
public class AgentScaffoldingFactory {

    private static final Logger log = LoggerFactory.getLogger(AgentScaffoldingFactory.class);

    @Inject PromptBuilder          promptBuilder;
    @Inject ConditionalPromptComposer composer;
    @Inject SkillDiscoveryEngine   skillDiscovery;
    @Inject SkillRegistry          skillRegistry;
    @Inject BuiltInTools           builtInTools;
    @Inject WorkloadModelRouter    modelRouter;
    @Inject GamelanConfig          config;
    @Inject AgentTelemetry         telemetry;

    // Cached suite (invalidated on refresh)
    private final AtomicReference<AgentSuite> cachedSuite = new AtomicReference<>();

    // ── Factory API ────────────────────────────────────────────────────────

    /**
     * Assembles a complete AgentSuite through the three-phase pipeline.
     * This is eager: the suite is fully ready before this method returns.
     *
     * @param mode the agent operating mode (affects system prompt assembly)
     * @return a fully assembled AgentSuite
     */
    public AgentSuite assemble(AgentMode mode) {
        Instant start = Instant.now();
        log.info("[scaffolding] assembling agent suite (mode={})", mode);

        try {
            // Phase 1: Skills — discover + register
            SkillPhaseResult skills = executeSkillPhase();

            // Phase 2: Subagents — compile specs + register
            SubagentPhaseResult subagents = executeSubagentPhase(mode);

            // Phase 3: Main agent — construct with full context
            MainAgentConfig mainAgent = executeMainAgentPhase(mode, skills, subagents);

            Duration elapsed = Duration.between(start, Instant.now());
            log.info("[scaffolding] assembled in {}ms: {} skills, {} subagents, {} tools",
                    elapsed.toMillis(), skills.skillCount(), subagents.subagentCount(),
                    mainAgent.toolCount());
            telemetry.count("scaffolding.assembled");
            telemetry.recordLatency("scaffolding.assembly_ms", elapsed.toMillis());

            AgentSuite suite = new AgentSuite(mode, skills, subagents, mainAgent,
                    elapsed, Instant.now(), this);
            cachedSuite.set(suite);
            return suite;

        } catch (Exception e) {
            log.error("[scaffolding] assembly failed: {}", e.getMessage(), e);
            throw new ScaffoldingException("Agent assembly failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the cached suite if available, otherwise assembles a new one.
     * Avoids redundant re-assembly on consecutive calls without config changes.
     */
    public AgentSuite getOrAssemble(AgentMode mode) {
        AgentSuite existing = cachedSuite.get();
        if (existing != null && existing.mode() == mode) {
            telemetry.count("scaffolding.cache_hit");
            return existing;
        }
        return assemble(mode);
    }

    /**
     * Rebuilds the agent suite (e.g., after a new MCP tool is discovered or skill installed).
     * Called when the tool registry changes — paper §2.2.1: refresh_tools().
     */
    public AgentSuite refresh(AgentMode mode) {
        log.info("[scaffolding] refreshing agent suite");
        cachedSuite.set(null);
        skillDiscovery.refresh();
        telemetry.count("scaffolding.refreshed");
        return assemble(mode);
    }

    /** Returns the currently cached suite without triggering assembly. */
    public Optional<AgentSuite> cached() { return Optional.ofNullable(cachedSuite.get()); }

    // ── Phase implementations ──────────────────────────────────────────────

    private SkillPhaseResult executeSkillPhase() {
        Instant start = Instant.now();
        // Skills already scanned by SkillDiscoveryEngine @PostConstruct
        int count = skillDiscovery.discoveredCount();
        String metaBlock = skillDiscovery.metadataPromptBlock();
        Duration elapsed = Duration.between(start, Instant.now());
        log.debug("[scaffolding] Phase 1 complete: {} skills in {}ms", count, elapsed.toMillis());
        return new SkillPhaseResult(count, metaBlock, elapsed);
    }

    private SubagentPhaseResult executeSubagentPhase(AgentMode mode) {
        Instant start = Instant.now();
        // Build subagent specifications based on mode
        List<SubagentSpec> specs = buildSubagentSpecs(mode);
        Duration elapsed = Duration.between(start, Instant.now());
        log.debug("[scaffolding] Phase 2 complete: {} subagents in {}ms", specs.size(), elapsed.toMillis());
        return new SubagentPhaseResult(specs, elapsed);
    }

    private MainAgentConfig executeMainAgentPhase(AgentMode mode,
                                                   SkillPhaseResult skills,
                                                   SubagentPhaseResult subagents) {
        Instant start = Instant.now();

        // Update composer context with runtime state
        composer.updateContext("mode",          mode.name().toLowerCase());
        composer.updateContext("in_git_repo",   isGitRepo());
        composer.updateContext("has_subagents", !subagents.specs().isEmpty());
        composer.updateContext("skill_count",   skills.skillCount());

        // Build system prompt — eager, before first call
        String systemPrompt = buildSystemPrompt(mode, skills, subagents);

        // Count tools
        long toolCount = builtInTools.toolCount() + subagents.subagentCount();

        String model = modelRouter.modelFor(WorkloadModelRouter.ModelRole.ACTION);
        Duration elapsed = Duration.between(start, Instant.now());
        log.debug("[scaffolding] Phase 3 complete: {}t prompt, {} tools in {}ms",
                systemPrompt.length() / 4, toolCount, elapsed.toMillis());
        return new MainAgentConfig(model, systemPrompt, (int) toolCount, elapsed);
    }

    private String buildSystemPrompt(AgentMode mode, SkillPhaseResult skills,
                                      SubagentPhaseResult subagents) {
        // Compose the full modular prompt
        String base = composer.compose();
        StringBuilder sb = new StringBuilder(base);

        // Inject skill metadata (Phase 1 output)
        if (!skills.metadataBlock().isBlank()) {
            sb.append("\n\n").append(skills.metadataBlock());
        }

        // Inject subagent catalog (Phase 2 output)
        if (!subagents.specs().isEmpty()) {
            sb.append("\n\n## Available Subagents\n");
            subagents.specs().forEach(spec ->
                    sb.append("- `").append(spec.name()).append("`: ")
                      .append(spec.description()).append("\n"));
        }

        // Inject tool catalog
        sb.append("\n\n## Available Tools\n").append(builtInTools.describeAll());

        return sb.toString();
    }

    private List<SubagentSpec> buildSubagentSpecs(AgentMode mode) {
        List<SubagentSpec> specs = new ArrayList<>();

        // Built-in subagent specs (from paper §2.4.8 — 8 subagent types)
        specs.add(new SubagentSpec("code-explorer",
                "Read-only codebase navigation, architectural analysis, pattern discovery",
                List.of("read_file", "search_files", "list_dir", "glob"), mode));
        specs.add(new SubagentSpec("planner",
                "Codebase exploration + plan writing. Read files + generate structured implementation plans",
                List.of("read_file", "search_files", "list_dir", "write_file"), mode));
        specs.add(new SubagentSpec("security-reviewer",
                "Security audits, vulnerability assessment with severity/confidence scoring",
                List.of("read_file", "search_files", "list_dir", "run_command"), mode));
        specs.add(new SubagentSpec("pr-reviewer",
                "Pull request code review, diff analysis, pre-merge feedback",
                List.of("read_file", "search_files", "git"), mode));

        if (mode == AgentMode.FULL) {
            specs.add(new SubagentSpec("ask-user",
                    "Present structured multi-choice questions to gather requirements",
                    List.of(), mode));
            specs.add(new SubagentSpec("web-clone",
                    "Analyze websites and generate UI replication code",
                    List.of("read_file", "write_file"), mode));
        }

        return specs;
    }

    private boolean isGitRepo() {
        try {
            Process p = new ProcessBuilder("git", "rev-parse", "--git-dir")
                    .redirectErrorStream(true).start();
            return p.waitFor() == 0;
        } catch (Exception e) { return false; }
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum AgentMode {
        /** Full access — normal interactive use. */
        FULL,
        /** Plan mode — read-only tools only, safe for codebase analysis. */
        PLAN,
        /** Minimal — for quick one-shot commands with reduced overhead. */
        MINIMAL
    }

    public record SkillPhaseResult(int skillCount, String metadataBlock, Duration elapsed) {}

    public record SubagentSpec(
            String       name,
            String       description,
            List<String> allowedTools,
            AgentMode    mode
    ) {}

    public record SubagentPhaseResult(List<SubagentSpec> specs, Duration elapsed) {
        public int subagentCount() { return specs.size(); }
    }

    public record MainAgentConfig(
            String   model,
            String   systemPrompt,
            int      toolCount,
            Duration elapsed
    ) {
        public int estimatedPromptTokens() { return systemPrompt.length() / 4; }
    }

    /** The fully assembled agent suite returned by the factory. */
    public record AgentSuite(
            AgentMode            mode,
            SkillPhaseResult     skills,
            SubagentPhaseResult  subagents,
            MainAgentConfig      mainAgent,
            Duration             assemblyTime,
            Instant              assembledAt,
            AgentScaffoldingFactory factory
    ) {
        /** Re-assembles the suite (e.g., after MCP tool discovery). */
        public AgentSuite refresh() { return factory.refresh(mode); }

        public String summary() {
            return String.format("AgentSuite[mode=%s, skills=%d, subagents=%d, tools=%d, " +
                    "prompt=%dt, assembled in %dms]",
                    mode, skills.skillCount(), subagents.subagentCount(),
                    mainAgent.toolCount(), mainAgent.estimatedPromptTokens(),
                    assemblyTime.toMillis());
        }
    }

    public static final class ScaffoldingException extends RuntimeException {
        public ScaffoldingException(String msg, Throwable cause) { super(msg, cause); }
    }
}
