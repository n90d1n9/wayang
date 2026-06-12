package tech.kayys.gamelan.context.dual_memory;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.ConversationMessage;
import tech.kayys.gamelan.agent.orchestration.*;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;
import tech.kayys.gamelan.session.ConversationSession;

import java.util.*;
import java.util.concurrent.atomic.*;

/**
 * DualMemoryManager — episodic long-range + verbatim working memory for bounded thinking contexts.
 *
 * <h2>From the OPENDEV paper (§2.3.3)</h2>
 * The thinking phase requires conversation context for strategic reasoning, but the full
 * conversation history can grow to hundreds of thousands of tokens. Providing the thinking model
 * with unbounded history is infeasible. Providing only recent messages loses strategic context,
 * causing the agent to "forget" its overall goals. The solution: a dual-memory architecture
 * inspired by human cognitive science, separating compressed long-range context from detailed
 * short-range context.
 *
 * <h2>Two memory tiers</h2>
 * <pre>
 * EPISODIC MEMORY — LLM-generated summary of the full conversation history.
 *   Captures strategic, long-range context: decisions made, overall goals, key findings,
 *   important file paths. Generated periodically (every 5 new messages) from the FULL
 *   history (NOT by iterative compression of prior summaries — that accumulates distortion).
 *   Max length: 500 chars (fits in ~125 tokens).
 *
 * WORKING MEMORY — Last N message pairs verbatim (default: last 6 exchanges).
 *   Contains fine-grained operational detail: exact file contents, specific error messages,
 *   precise line numbers, recent tool call outcomes. Summarization would destroy exactly the
 *   details that matter most for the next action.
 * </pre>
 *
 * <h2>Design evolution from the paper</h2>
 * Early attempts used pure summarization: critical identifiers (file paths, variable names)
 * were lost, causing the agent to reference non-existent files. The opposite extreme (only recent
 * messages) lost long-range goals after 10 turns. Iterative summarization (summary-of-summary)
 * accumulates errors — periodic regeneration from the full history corrects this drift.
 */
@ApplicationScoped
public class DualMemoryManager {

    private static final Logger log = LoggerFactory.getLogger(DualMemoryManager.class);

    // Paper constants (§2.3.3)
    private static final int EPISODIC_MAX_CHARS      = 500;   // ~125 tokens
    private static final int EPISODIC_REGEN_INTERVAL = 5;     // messages between regenerations
    private static final int WORKING_MEMORY_PAIRS    = 6;     // recent exchanges to keep verbatim

    @Inject GamelanConfig            config;
    @Inject AgentTelemetry           telemetry;
    @Inject SingleAgentOrchestrator  orchestrator;

    private final AtomicInteger messagesSinceRegen = new AtomicInteger(0);
    private volatile String     episodicSummary    = "";
    private volatile int        lastFullHistoryHash = 0;

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Assembles the context for a thinking LLM call.
     * Returns: [episodic summary + working memory messages + current query].
     *
     * <p>The thinking token budget remains bounded regardless of conversation length,
     * since the episodic summary has a fixed max length and the working memory window is constant.
     *
     * @param fullHistory  the complete conversation history
     * @param currentQuery the current user query
     * @return bounded context list suitable for the thinking model
     */
    public List<ConversationMessage> assembleThinkingContext(
            List<ConversationMessage> fullHistory, String currentQuery) {

        maybeRegenerateEpisodic(fullHistory);

        List<ConversationMessage> context = new ArrayList<>();

        // 1. Episodic memory — compressed long-range context
        if (!episodicSummary.isBlank()) {
            context.add(ConversationMessage.system(
                    "## Episodic Memory (Long-Range Context)\n" + episodicSummary));
        }

        // 2. Working memory — verbatim recent exchanges
        List<ConversationMessage> working = workingMemory(fullHistory);
        context.addAll(working);

        // 3. Current query
        context.add(ConversationMessage.user(currentQuery));

        int estimatedTokens = context.stream()
                .mapToInt(m -> m.content().length() / 4).sum();
        log.debug("[dual-mem] thinking context: {} messages, ~{}t (episodic={}t, working={}t)",
                context.size(), estimatedTokens,
                episodicSummary.length() / 4,
                working.stream().mapToInt(m -> m.content().length() / 4).sum());
        telemetry.gauge("dual_memory.context_tokens", estimatedTokens);

        return context;
    }

    /**
     * Signals that a new message was added to the conversation.
     * Triggers episodic memory regeneration if the interval has been reached.
     *
     * @param fullHistory the complete updated history
     */
    public void onNewMessage(List<ConversationMessage> fullHistory) {
        int count = messagesSinceRegen.incrementAndGet();
        if (count >= EPISODIC_REGEN_INTERVAL) {
            regenerateEpisodic(fullHistory);
            messagesSinceRegen.set(0);
        }
    }

    /** Returns the current episodic summary (may be empty if not yet generated). */
    public String episodicSummary()   { return episodicSummary; }

    /** Returns the current working memory window size. */
    public int workingMemorySize()    { return WORKING_MEMORY_PAIRS; }

    /** Forces immediate regeneration of the episodic summary. */
    public void forceRegenerate(List<ConversationMessage> fullHistory) {
        regenerateEpisodic(fullHistory);
    }

    /** Clears both memory tiers (call on /clear). */
    public void clear() {
        episodicSummary = "";
        messagesSinceRegen.set(0);
        lastFullHistoryHash = 0;
        log.debug("[dual-mem] cleared");
    }

    // ── Private ────────────────────────────────────────────────────────────

    private void maybeRegenerateEpisodic(List<ConversationMessage> history) {
        int currentHash = history.hashCode();
        if (episodicSummary.isBlank() && !history.isEmpty() && currentHash != lastFullHistoryHash) {
            regenerateEpisodic(history);
        }
        lastFullHistoryHash = currentHash;
    }

    private void regenerateEpisodic(List<ConversationMessage> fullHistory) {
        if (fullHistory.isEmpty()) return;

        // Build the full history text (exclude the working memory tail — it's already verbatim)
        int tailSize = WORKING_MEMORY_PAIRS * 2;
        List<ConversationMessage> historyForSummary = fullHistory.size() > tailSize
                ? fullHistory.subList(0, fullHistory.size() - tailSize)
                : fullHistory;

        if (historyForSummary.isEmpty()) return;

        String historyText = historyForSummary.stream()
                .limit(80) // cap for summarization LLM call
                .map(m -> "[" + m.role() + "]: " +
                          (m.content().length() > 300 ? m.content().substring(0, 300) + "…" : m.content()))
                .reduce("", (a, b) -> a + "\n" + b);

        String prompt = """
                Summarize this conversation history in UNDER 120 words.
                PRESERVE (never omit):
                - File paths, function names, class names, variable names
                - Error messages and their root causes
                - Decisions and their rationale
                - The user's primary objective and any refinements
                - Current task state (what is done vs. what remains)
                
                OMIT: Greetings, filler, redundant tool outputs, procedural details.
                
                History:
                """ + historyText;

        try {
            OrchestratorResult result = orchestrator.execute(
                    AgentRequest.builder(prompt)
                            .model(config.defaultModel())
                            .session(new ConversationSession(null, false, 4000))
                            .stream(false).maxSteps(1).build());

            if (result.success() && !result.answer().isBlank()) {
                // Enforce max length
                String summary = result.answer().strip();
                if (summary.length() > EPISODIC_MAX_CHARS * 4) {
                    summary = summary.substring(0, EPISODIC_MAX_CHARS * 4) + "…";
                }
                episodicSummary = summary;
                telemetry.count("dual_memory.episodic.regenerated");
                log.debug("[dual-mem] episodic regenerated: {}t", episodicSummary.length() / 4);
            }
        } catch (Exception e) {
            log.warn("[dual-mem] episodic generation failed: {}", e.getMessage());
            // Graceful degradation: keep old summary or empty — thinking still works
        }
    }

    /** Returns the last WORKING_MEMORY_PAIRS * 2 messages verbatim. */
    private List<ConversationMessage> workingMemory(List<ConversationMessage> fullHistory) {
        int keep = WORKING_MEMORY_PAIRS * 2; // each exchange = user + assistant = 2 messages
        if (fullHistory.size() <= keep) return new ArrayList<>(fullHistory);
        return new ArrayList<>(fullHistory.subList(fullHistory.size() - keep, fullHistory.size()));
    }
}
