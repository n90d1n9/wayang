package tech.kayys.gamelan.agent.loop;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ToolCall;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * DoomLoopDetector — fingerprint-based repeated tool call detection with two-tier escalation.
 *
 * <h2>From the OPENDEV paper (§2.2.6)</h2>
 * <blockquote>
 * Doom-loop detection: each tool call is fingerprinted as an MD5 hash of the tool name and its
 * arguments, and fingerprints are tracked in a sliding window of the 20 most recent calls.
 * If any fingerprint appears 3 or more times, the system injects a [SYSTEM WARNING] message
 * into the conversation. If the same fingerprint recurs after the warning, the system escalates
 * to an approval-based pause.
 * </blockquote>
 *
 * <h2>Why this is better than iteration caps</h2>
 * Pre-existing safeguards (iteration caps, consecutive-read counters) are too coarse: they trigger
 * on any repeated tool type rather than on identical (tool, arguments) pairs, and they activate
 * only after many more iterations. Fingerprint-based detection catches stuck loops within 3
 * repetitions.
 *
 * <h2>Two-tier escalation</h2>
 * <pre>
 * Tier 1 (repeat ≥ 3):  Inject a WARNING system message into the conversation.
 *                        The agent can still proceed — it gets one more chance to change.
 * Tier 2 (repeat after warning): Escalate to ApprovalManager pause.
 *                        The agent CANNOT bypass a genuine execution halt.
 * </pre>
 */
@ApplicationScoped
public class DoomLoopDetector {

    private static final Logger log = LoggerFactory.getLogger(DoomLoopDetector.class);

    // Paper constants (§2.2.6, Table 9)
    private static final int    WINDOW_SIZE          = 20;  // sliding window of recent fingerprints
    private static final int    WARN_THRESHOLD       = 3;   // warn at 3 identical calls
    private static final int    ESCALATE_THRESHOLD   = 4;   // escalate at 4 (after warning)

    @Inject AgentTelemetry telemetry;
    @Inject GamelanConfig  config;

    // Sliding window — bounded Deque, thread-safe via synchronization
    private final Deque<String>          window        = new ArrayDeque<>(WINDOW_SIZE);
    private final Map<String, Integer>   counts        = new LinkedHashMap<>();
    // Track which fingerprints have already received a warning this session
    private final Set<String>            warned        = new HashSet<>();
    // One-shot guard: after escalation approval, allow the call once before re-arming
    private final Set<String>            oneShotAllow  = new HashSet<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Evaluates a batch of tool calls against the doom-loop detector.
     *
     * @param toolCalls the tool calls the LLM wants to execute this iteration
     * @return a {@link DoomLoopAssessment} indicating the safety action to take
     */
    public synchronized DoomLoopAssessment assess(List<ToolCall> toolCalls) {
        if (toolCalls.isEmpty()) return DoomLoopAssessment.clean();

        List<String> fingerprints = toolCalls.stream()
                .map(this::fingerprint).toList();

        // Add to sliding window
        fingerprints.forEach(fp -> {
            if (window.size() >= WINDOW_SIZE) {
                String evicted = window.pollFirst();
                counts.merge(evicted, -1, Integer::sum);
                if (counts.getOrDefault(evicted, 0) <= 0) counts.remove(evicted);
            }
            window.addLast(fp);
            counts.merge(fp, 1, Integer::sum);
        });

        // Find the highest-count fingerprint
        Optional<Map.Entry<String, Integer>> worst = counts.entrySet().stream()
                .max(Map.Entry.comparingByValue());
        if (worst.isEmpty()) return DoomLoopAssessment.clean();

        String topFp    = worst.get().getKey();
        int    topCount = worst.get().getValue();

        // One-shot allow: user approved continuation for this fingerprint once
        if (oneShotAllow.remove(topFp)) {
            log.debug("[doom] one-shot allow consumed for {}", topFp.substring(0, 8));
            return DoomLoopAssessment.clean();
        }

        if (topCount >= ESCALATE_THRESHOLD && warned.contains(topFp)) {
            // Tier 2: escalate to approval halt
            telemetry.count("doom_loop.escalation");
            log.warn("[doom] ESCALATION: fingerprint {} repeated {} times after warning", topFp.substring(0,8), topCount);
            return DoomLoopAssessment.escalate(topFp, topCount,
                    buildEscalationMessage(toolCalls, topCount));
        }

        if (topCount >= WARN_THRESHOLD) {
            // Tier 1: inject warning into conversation
            warned.add(topFp);
            telemetry.count("doom_loop.warning");
            log.warn("[doom] WARNING: fingerprint {} repeated {} times", topFp.substring(0,8), topCount);
            return DoomLoopAssessment.warn(topFp, topCount,
                    buildWarningMessage(toolCalls, topCount));
        }

        return DoomLoopAssessment.clean();
    }

    /**
     * Called after the user approves continuation during an escalation pause.
     * Arms a one-shot guard that allows the next call for the given fingerprint.
     */
    public synchronized void approveOnce(String fingerprint) {
        oneShotAllow.add(fingerprint);
        log.info("[doom] one-shot approval granted for {}", fingerprint.substring(0, 8));
    }

    /** Resets the detector (e.g., after /clear or when the task changes). */
    public synchronized void reset() {
        window.clear();
        counts.clear();
        warned.clear();
        oneShotAllow.clear();
    }

    // ── Private ────────────────────────────────────────────────────────────

    /**
     * Computes an MD5 fingerprint of (toolName, sortedArgs).
     * Sorting the args ensures that parameter ordering differences don't create false negatives.
     */
    private String fingerprint(ToolCall call) {
        String key = call.name() + ":" +
                call.parameters().entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .reduce("", (a, b) -> a + "|" + b);
        try {
            byte[] hash = MessageDigest.getInstance("MD5").digest(key.getBytes());
            StringBuilder sb = new StringBuilder(32);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(key.hashCode());
        }
    }

    private String buildWarningMessage(List<ToolCall> calls, int count) {
        String callDesc = calls.stream()
                .findFirst()
                .map(c -> c.name() + "(" + String.join(", ",
                        c.parameters().entrySet().stream()
                                .map(e -> e.getKey() + "=" + truncate(e.getValue(), 30))
                                .limit(3).toList()) + ")")
                .orElse("unknown");
        return "[SYSTEM WARNING] The agent has called `" + callDesc + "` with identical arguments " +
                count + " times. This may indicate a stuck loop. " +
                "Try a different approach, search for alternative solutions, or ask the user for guidance.";
    }

    private String buildEscalationMessage(List<ToolCall> calls, int count) {
        return "Agent is repeating the same action (" + count +
                " times). The identical tool call has been blocked. " +
                "Revise your approach: try a different tool, different arguments, " +
                "or ask the user how to proceed.";
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : (s != null ? s : "");
    }

    // ── Data types ─────────────────────────────────────────────────────────

    public enum DoomLoopAction { NONE, WARN, ESCALATE }

    public record DoomLoopAssessment(
            DoomLoopAction action,
            String         fingerprint,
            int            repeatCount,
            String         message
    ) {
        static DoomLoopAssessment clean()    { return new DoomLoopAssessment(DoomLoopAction.NONE, "", 0, ""); }
        static DoomLoopAssessment warn(String fp, int n, String msg)     { return new DoomLoopAssessment(DoomLoopAction.WARN, fp, n, msg); }
        static DoomLoopAssessment escalate(String fp, int n, String msg) { return new DoomLoopAssessment(DoomLoopAction.ESCALATE, fp, n, msg); }
        public boolean isClean()    { return action == DoomLoopAction.NONE; }
        public boolean needsWarn()  { return action == DoomLoopAction.WARN; }
        public boolean needsHalt()  { return action == DoomLoopAction.ESCALATE; }
    }
}
