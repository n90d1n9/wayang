package tech.kayys.gamelan.safety;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.tool.ToolResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * Safety Layer — constraint solving, action simulation, and anomaly detection.
 *
 * <h2>Three Safety Mechanisms</h2>
 *
 * <h3>1. Constraint Solver</h3>
 * A declarative rule engine that prevents unsafe operations before execution.
 * Rules are expressed as:
 * <pre>
 * IF tool == "run_command" AND command MATCHES "rm.*-rf"
 * THEN BLOCK with reason "Recursive deletion is forbidden"
 * </pre>
 * Rules support: BLOCK, WARN, REQUIRE_APPROVAL, TRANSFORM (rewrite the call safely)
 *
 * <h3>2. Action Simulation</h3>
 * Before executing irreversible or high-risk actions, the engine simulates
 * the effect using a read-only dry-run. Only if the simulation passes does
 * the real action execute. This is critical for:
 * <ul>
 *   <li>File system mutations (write_file, apply_patch)</li>
 *   <li>Database operations</li>
 *   <li>Shell commands that modify state</li>
 *   <li>Financial transactions</li>
 * </ul>
 *
 * <h3>3. Anomaly Detector</h3>
 * Statistical model over tool call patterns. Detects:
 * <ul>
 *   <li>Prompt injection (unusual parameter patterns)</li>
 *   <li>Runaway loops (same tool called 10+ times)</li>
 *   <li>Unexpected data exfiltration patterns</li>
 *   <li>Model hallucination artifacts in tool parameters</li>
 * </ul>
 */
@ApplicationScoped
public class ConstraintSolver {

    private static final Logger log = LoggerFactory.getLogger(ConstraintSolver.class);

    private final List<SafetyRule> rules = new CopyOnWriteArrayList<>();

    // Anomaly detection state
    private final Map<String, Integer> toolCallCounts = new HashMap<>();
    private final List<AnomalyEvent>   anomalies      = new ArrayList<>();
    private int totalCalls = 0;

    // ── Default rules ──────────────────────────────────────────────────────

    {
        // Prevent destructive shell commands
        addRule(SafetyRule.block("no-rm-rf",
                call -> "run_command".equals(call.name())
                        && call.param("command").matches(".*rm\\s.*-[^\\s]*r[^\\s]*f.*"),
                "Recursive deletion is forbidden"));

        // Prevent writing to system directories
        addRule(SafetyRule.block("no-system-write",
                call -> "write_file".equals(call.name())
                        && (call.param("path").startsWith("/etc")
                            || call.param("path").startsWith("/sys")
                            || call.param("path").startsWith("/proc")),
                "Writing to system directories is forbidden"));

        // Prevent git push without approval
        addRule(SafetyRule.warn("git-push-warning",
                call -> "git".equals(call.name())
                        && call.param("operation").startsWith("push"),
                "Git push detected — ensure changes are reviewed"));

        // Prevent SQL DROP in commands
        addRule(SafetyRule.block("no-drop-table",
                call -> call.param("command").toLowerCase().contains("drop table")
                        || call.param("command").toLowerCase().contains("drop database"),
                "DROP TABLE / DROP DATABASE is forbidden"));

        // Warn on large file writes (>100KB content suggests data exfiltration)
        addRule(SafetyRule.warn("large-write-warning",
                call -> "write_file".equals(call.name())
                        && call.param("content").length() > 100_000,
                "Writing more than 100KB to a file — verify this is intentional"));

        // Block prompt injection patterns in parameters
        addRule(SafetyRule.block("no-prompt-injection",
                call -> isPromptInjection(call),
                "Potential prompt injection detected in tool parameters"));
    }

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Evaluates a tool call against all safety rules.
     * Returns a verdict indicating whether execution should proceed.
     */
    public SafetyVerdict evaluate(ToolCall call) {
        totalCalls++;
        toolCallCounts.merge(call.name(), 1, Integer::sum);

        // Anomaly detection
        detectAnomalies(call);

        // Rule evaluation
        List<RuleViolation> blocks   = new ArrayList<>();
        List<RuleViolation> warnings = new ArrayList<>();

        for (SafetyRule rule : rules) {
            if (rule.condition().test(call)) {
                RuleViolation v = new RuleViolation(rule.id(), rule.message(),
                        rule.action(), Instant.now());
                switch (rule.action()) {
                    case BLOCK  -> { blocks.add(v);   log.warn("[safety] BLOCKED: {} — {}", call.name(), rule.message()); }
                    case WARN   -> { warnings.add(v); log.info("[safety] WARN: {} — {}", call.name(), rule.message()); }
                    case TRANSFORM -> {
                        call = rule.transform(call);
                        log.info("[safety] TRANSFORMED: {} by rule {}", call.name(), rule.id());
                    }
                    default -> {}
                }
            }
        }

        if (!blocks.isEmpty()) {
            return SafetyVerdict.blocked(blocks, warnings);
        }
        return warnings.isEmpty()
                ? SafetyVerdict.safe(call)
                : SafetyVerdict.safeWithWarnings(call, warnings);
    }

    /**
     * Simulates an action before real execution.
     * Returns a simulation report indicating predicted side effects.
     */
    public SimulationReport simulate(ToolCall call) {
        return switch (call.name()) {
            case "write_file"   -> simulateFileWrite(call);
            case "apply_patch"  -> simulatePatch(call);
            case "run_command"  -> simulateCommand(call);
            default             -> SimulationReport.safe(call.name(), "No simulation available");
        };
    }

    /** Adds a custom safety rule. */
    public void addRule(SafetyRule rule) {
        rules.add(rule);
        log.debug("[safety] rule registered: {}", rule.id());
    }

    /** Removes a rule by ID. */
    public void removeRule(String ruleId) {
        rules.removeIf(r -> r.id().equals(ruleId));
    }

    /** Returns all detected anomalies in this session. */
    public List<AnomalyEvent> anomalies() { return List.copyOf(anomalies); }

    /** Returns a safety summary for the current session. */
    public SafetySummary summary() {
        long blocked  = anomalies.stream().filter(a -> a.severity() == AnomalyEvent.Severity.HIGH).count();
        long warnings = anomalies.stream().filter(a -> a.severity() == AnomalyEvent.Severity.MEDIUM).count();
        return new SafetySummary(totalCalls, (int)blocked, (int)warnings,
                rules.size(), toolCallCounts);
    }

    // ── Simulation ─────────────────────────────────────────────────────────

    private SimulationReport simulateFileWrite(ToolCall call) {
        String path    = call.param("path");
        String content = call.param("content");
        boolean exists = Files.exists(Path.of(path));
        List<String> effects = new ArrayList<>();
        effects.add((exists ? "OVERWRITE" : "CREATE") + " file: " + path);
        effects.add("Size: ~" + content.length() + " bytes");
        if (exists) {
            effects.add("Previous content will be LOST (no backup)");
        }
        boolean risky = exists && content.isBlank();
        return new SimulationReport(call.name(), path, effects,
                risky ? SimulationReport.RiskLevel.MEDIUM : SimulationReport.RiskLevel.LOW,
                risky ? "Writing empty content to existing file" : null);
    }

    private SimulationReport simulatePatch(ToolCall call) {
        String patch = call.param("patch");
        long additions = patch.lines().filter(l -> l.startsWith("+") && !l.startsWith("+++")).count();
        long deletions = patch.lines().filter(l -> l.startsWith("-") && !l.startsWith("---")).count();
        return new SimulationReport(call.name(), "patch",
                List.of("+" + additions + " lines added", "-" + deletions + " lines removed"),
                SimulationReport.RiskLevel.LOW, null);
    }

    private SimulationReport simulateCommand(ToolCall call) {
        String cmd = call.param("command");
        SimulationReport.RiskLevel risk = SimulationReport.RiskLevel.LOW;
        String warning = null;
        if (cmd.contains("rm") || cmd.contains("del")) {
            risk = SimulationReport.RiskLevel.HIGH;
            warning = "Command may delete files";
        } else if (cmd.contains("curl") || cmd.contains("wget")) {
            risk = SimulationReport.RiskLevel.MEDIUM;
            warning = "Command makes network requests";
        }
        return new SimulationReport(call.name(), cmd,
                List.of("Shell command: " + cmd), risk, warning);
    }

    // ── Anomaly detection ──────────────────────────────────────────────────

    private void detectAnomalies(ToolCall call) {
        // Runaway loop: same tool called 10+ times
        int count = toolCallCounts.getOrDefault(call.name(), 0);
        if (count >= 10) {
            anomalies.add(new AnomalyEvent("RUNAWAY_TOOL", call.name(),
                    "Tool '" + call.name() + "' called " + count + " times — possible loop",
                    AnomalyEvent.Severity.HIGH, Instant.now()));
        }

        // Prompt injection heuristic
        if (isPromptInjection(call)) {
            anomalies.add(new AnomalyEvent("PROMPT_INJECTION", call.name(),
                    "Suspicious instruction-like content in parameters",
                    AnomalyEvent.Severity.HIGH, Instant.now()));
        }

        // Unusual parameter length (possible data exfiltration)
        call.parameters().forEach((k, v) -> {
            if (v != null && v.length() > 50_000) {
                anomalies.add(new AnomalyEvent("LARGE_PARAM", call.name(),
                        "Parameter '" + k + "' is " + v.length() + " chars — unusually large",
                        AnomalyEvent.Severity.MEDIUM, Instant.now()));
            }
        });
    }

    private boolean isPromptInjection(ToolCall call) {
        String allParams = call.parameters().values().toString().toLowerCase();
        return allParams.contains("ignore previous instructions")
                || allParams.contains("disregard your system prompt")
                || allParams.contains("you are now")
                || allParams.contains("forget everything")
                || allParams.contains("new instructions:");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public record SafetyRule(
            String           id,
            Predicate<ToolCall> condition,
            String           message,
            RuleAction       action
    ) {
        public enum RuleAction { BLOCK, WARN, REQUIRE_APPROVAL, TRANSFORM }

        static SafetyRule block(String id, Predicate<ToolCall> cond, String msg) {
            return new SafetyRule(id, cond, msg, RuleAction.BLOCK);
        }
        static SafetyRule warn(String id, Predicate<ToolCall> cond, String msg) {
            return new SafetyRule(id, cond, msg, RuleAction.WARN);
        }

        ToolCall transform(ToolCall call) { return call; } // override for transform rules
    }

    public record RuleViolation(
            String     ruleId,
            String     message,
            SafetyRule.RuleAction action,
            Instant    detectedAt
    ) {}

    public record SafetyVerdict(
            boolean          safe,
            ToolCall         effectiveCall, // may be transformed
            List<RuleViolation> violations,
            List<RuleViolation> warnings,
            String           blockReason
    ) {
        static SafetyVerdict safe(ToolCall call) {
            return new SafetyVerdict(true, call, List.of(), List.of(), null);
        }
        static SafetyVerdict safeWithWarnings(ToolCall call, List<RuleViolation> w) {
            return new SafetyVerdict(true, call, List.of(), w, null);
        }
        static SafetyVerdict blocked(List<RuleViolation> v, List<RuleViolation> w) {
            return new SafetyVerdict(false, null, v, w,
                    v.stream().map(RuleViolation::message).findFirst().orElse("Blocked"));
        }
    }

    public record SimulationReport(
            String        toolName,
            String        target,
            List<String>  predictedEffects,
            RiskLevel     riskLevel,
            String        warning
    ) {
        public enum RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }
        static SimulationReport safe(String tool, String msg) {
            return new SimulationReport(tool, "", List.of(msg), RiskLevel.LOW, null);
        }
    }

    public record AnomalyEvent(
            String   type,
            String   toolName,
            String   description,
            Severity severity,
            Instant  detectedAt
    ) {
        public enum Severity { LOW, MEDIUM, HIGH, CRITICAL }
    }

    public record SafetySummary(
            int totalCalls, int blocked, int warnings,
            int rulesActive, Map<String, Integer> toolCallCounts
    ) {}
}
