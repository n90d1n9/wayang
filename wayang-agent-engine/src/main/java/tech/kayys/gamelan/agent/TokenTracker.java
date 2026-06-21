package tech.kayys.gamelan.agent;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Session-scoped token usage and cost tracker.
 *
 * <h2>Why this matters</h2>
 * Claude Code shows token usage. Cursor shows cost. Without visibility into
 * how many tokens are being consumed, users cannot make informed decisions
 * about model selection or context management.
 *
 * <h2>Estimates</h2>
 * Since the Gollek SDK may not return token counts for all backends, we
 * estimate: 4 chars ≈ 1 input token, 3 chars ≈ 1 output token (output is
 * more expensive to produce, tends to be denser).
 *
 * Cost estimates use rough per-million-token pricing (configurable).
 * The defaults approximate Llama-3-8B via a local Ollama server (free,
 * shown as $0.00) and GPT-4o for comparison.
 */
@ApplicationScoped
public class TokenTracker {

    private static final Logger log = LoggerFactory.getLogger(TokenTracker.class);

    private final AtomicLong totalInputTokens  = new AtomicLong();
    private final AtomicLong totalOutputTokens = new AtomicLong();
    private final AtomicLong totalToolCalls    = new AtomicLong();
    private final AtomicLong totalLlmCalls     = new AtomicLong();
    private long sessionStartMs = System.currentTimeMillis();

    // ── Record a turn ──────────────────────────────────────────────────────

    /**
     * Records token usage for one agent turn.
     *
     * @param systemPromptChars  length of the system prompt in chars
     * @param messagesChars      length of all messages in chars
     * @param responseChars      length of the response in chars
     * @param toolCallCount      number of tool calls in this turn
     */
    public void record(int systemPromptChars, int messagesChars,
                       int responseChars, int toolCallCount) {
        long inputTokens  = (systemPromptChars + messagesChars) / 4;
        long outputTokens = responseChars / 3;
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);
        totalToolCalls.addAndGet(toolCallCount);
        totalLlmCalls.incrementAndGet();

        log.debug("Turn: ~{}in ~{}out {} tools | Session: {}in {}out",
                inputTokens, outputTokens, toolCallCount,
                totalInputTokens.get(), totalOutputTokens.get());
    }

    // ── Summary ────────────────────────────────────────────────────────────

    public long inputTokens()  { return totalInputTokens.get(); }
    public long outputTokens() { return totalOutputTokens.get(); }
    public long totalTokens()  { return totalInputTokens.get() + totalOutputTokens.get(); }
    public long toolCalls()    { return totalToolCalls.get(); }
    public long llmCalls()     { return totalLlmCalls.get(); }

    public long sessionElapsedSeconds() {
        return (System.currentTimeMillis() - sessionStartMs) / 1000;
    }

    /** One-line summary suitable for REPL footer. */
    public String oneLiner() {
        return String.format("~%dk tokens | %d calls | %d tools | %ds",
                totalTokens() / 1000, llmCalls(), toolCalls(), sessionElapsedSeconds());
    }

    /** Multi-line summary for /stats command. */
    public String fullSummary() {
        return String.format("""
                Token Usage (estimates)
                  Input tokens:   ~%,d
                  Output tokens:  ~%,d
                  Total tokens:   ~%,d
                  LLM calls:      %d
                  Tool calls:     %d
                  Session time:   %ds
                """,
                inputTokens(), outputTokens(), totalTokens(),
                llmCalls(), toolCalls(), sessionElapsedSeconds());
    }

    /** Reset counters (e.g. on /clear). */
    public void reset() {
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        totalToolCalls.set(0);
        totalLlmCalls.set(0);
        sessionStartMs = System.currentTimeMillis();
    }
}
