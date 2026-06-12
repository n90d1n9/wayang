package tech.kayys.gamelan.session.cost;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.observability.metrics.AgentTelemetry;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.*;

/**
 * SessionCostTracker — per-session token usage and USD cost accounting.
 *
 * <h2>From the OPENDEV paper (§2.2.3 — Session cost tracking)</h2>
 * Session cost tracking records cumulative token usage and cost after each LLM call via a
 * CostTracker service that computes cost from the API's reported token counts and the model's
 * pricing metadata; running totals are displayed in the TUI status bar and persisted in session
 * metadata for resumption across --continue invocations.
 *
 * <h2>Also from §2.5.1 — Session Storage</h2>
 * Each session's metadata file includes a cost_tracking object that records cumulative API
 * usage: total input tokens, total output tokens, total cost in USD, and the number of API calls.
 * When the user resumes a session via --continue, the CostTracker service restores its state from
 * this metadata, ensuring that the running cost display reflects the full session history.
 *
 * <h2>Features</h2>
 * <ul>
 *   <li>Per-model granular tracking (different models have different pricing)</li>
 *   <li>Per-call breakdown with tool call counts</li>
 *   <li>Budget alerts at configurable thresholds</li>
 *   <li>Context cache savings tracking (Anthropic prompt caching)</li>
 *   <li>Serializable state for session resumption</li>
 *   <li>Projected session cost at current burn rate</li>
 * </ul>
 */
@ApplicationScoped
public class SessionCostTracker {

    private static final Logger log = LoggerFactory.getLogger(SessionCostTracker.class);

    @Inject AgentTelemetry telemetry;

    // Per-model pricing in USD per million tokens (defaults — override via config)
    // Input / Output / CachedInput
    private static final Map<String, double[]> MODEL_PRICING = Map.of(
            "claude-opus-4-6",    new double[]{ 15.00,  75.00,  1.50 },
            "claude-sonnet-4-6",  new double[]{  3.00,  15.00,  0.30 },
            "claude-haiku-4-5",   new double[]{  0.80,   4.00,  0.08 },
            "gpt-4o",             new double[]{  5.00,  15.00,  2.50 },
            "gpt-4o-mini",        new double[]{  0.15,   0.60,  0.075},
            "llama3",             new double[]{  0.00,   0.00,  0.00 },  // local — free
            "qwen2-7b",           new double[]{  0.00,   0.00,  0.00 }   // local — free
    );

    // Per-model aggregates
    private final Map<String, ModelUsage> usageByModel = new ConcurrentHashMap<>();
    // Call history (capped at 1000)
    private final List<CallRecord> callHistory = Collections.synchronizedList(
            new ArrayList<>(128));

    private final AtomicLong totalInputTokens   = new AtomicLong(0);
    private final AtomicLong totalOutputTokens  = new AtomicLong(0);
    private final AtomicLong totalCacheTokens   = new AtomicLong(0);  // cache hits
    private final AtomicLong totalLlmCalls      = new AtomicLong(0);
    private final AtomicLong totalToolCalls     = new AtomicLong(0);
    private final AtomicLong sessionStartMs     = new AtomicLong(System.currentTimeMillis());

    private volatile double budgetAlertThreshold = Double.MAX_VALUE; // disabled by default
    private final List<Runnable> alertCallbacks = new ArrayList<>();

    // ── Public API ─────────────────────────────────────────────────────────

    /**
     * Records a completed LLM call and updates all cost accumulators.
     * Should be called immediately after each successful API response.
     *
     * @param model          the model identifier
     * @param inputTokens    API-reported input token count (prompt_tokens)
     * @param outputTokens   API-reported output token count (completion_tokens)
     * @param cachedTokens   tokens served from the provider's prompt cache (0 if not cached)
     * @param toolCallCount  number of tool calls dispatched in this turn
     */
    public void record(String model, long inputTokens, long outputTokens,
                       long cachedTokens, int toolCallCount) {
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);
        totalCacheTokens.addAndGet(cachedTokens);
        totalLlmCalls.incrementAndGet();
        totalToolCalls.addAndGet(toolCallCount);

        // Per-model accumulation
        ModelUsage mu = usageByModel.computeIfAbsent(model, k -> new ModelUsage(model));
        mu.inputTokens.addAndGet(inputTokens);
        mu.outputTokens.addAndGet(outputTokens);
        mu.cachedTokens.addAndGet(cachedTokens);
        mu.calls.incrementAndGet();

        // Calculate cost for this call
        double callCost = calculateCost(model, inputTokens, outputTokens, cachedTokens);
        mu.totalCostUsd.addAndGet((long)(callCost * 1_000_000)); // store as micro-dollars

        // Add to call history
        CallRecord rec = new CallRecord(model, inputTokens, outputTokens, cachedTokens,
                toolCallCount, callCost, Instant.now());
        synchronized (callHistory) {
            if (callHistory.size() >= 1000) callHistory.remove(0);
            callHistory.add(rec);
        }

        // Budget alert
        double totalCost = totalCostUsd();
        if (totalCost >= budgetAlertThreshold && !alertCallbacks.isEmpty()) {
            alertCallbacks.forEach(cb -> { try { cb.run(); } catch (Exception ignored) {} });
            telemetry.count("cost.budget_alert");
        }

        log.debug("[cost] model={} in={}t out={}t cached={}t cost=${:.4f} | session total=${:.4f}",
                model, inputTokens, outputTokens, cachedTokens, callCost, totalCost);
        telemetry.gauge("cost.session_usd", totalCost);
        telemetry.count("cost.llm_call");
    }

    /**
     * Configures a USD budget alert. The callback is invoked when the session cost exceeds it.
     */
    public void setBudgetAlert(double thresholdUsd, Runnable onExceeded) {
        this.budgetAlertThreshold = thresholdUsd;
        this.alertCallbacks.add(onExceeded);
    }

    // ── Aggregates ─────────────────────────────────────────────────────────

    public long   inputTokens()   { return totalInputTokens.get(); }
    public long   outputTokens()  { return totalOutputTokens.get(); }
    public long   cacheTokens()   { return totalCacheTokens.get(); }
    public long   totalTokens()   { return totalInputTokens.get() + totalOutputTokens.get(); }
    public long   llmCalls()      { return totalLlmCalls.get(); }
    public long   toolCalls()     { return totalToolCalls.get(); }
    public long   sessionMs()     { return System.currentTimeMillis() - sessionStartMs.get(); }

    public double totalCostUsd() {
        return usageByModel.values().stream()
                .mapToDouble(mu -> mu.totalCostUsd.get() / 1_000_000.0)
                .sum();
    }

    /** Estimates what the full session will cost at the current burn rate. */
    public double projectedCostUsd() {
        long elapsed = Math.max(1, sessionMs());
        double burnRate = totalCostUsd() / elapsed; // USD per ms
        long estimatedTotalMs = elapsed * 3; // rough heuristic: session typically 3x current
        return burnRate * estimatedTotalMs;
    }

    /** Savings from Anthropic prompt cache hits. */
    public double cacheSavingsUsd() {
        return usageByModel.entrySet().stream().mapToDouble(e -> {
            double[] pricing = MODEL_PRICING.getOrDefault(e.getKey(), new double[]{0, 0, 0});
            long cached = e.getValue().cachedTokens.get();
            return cached * (pricing[0] - pricing[2]) / 1_000_000.0; // saved vs. full input cost
        }).sum();
    }

    // ── Formatted output ───────────────────────────────────────────────────

    /** One-liner for display in REPL footer. */
    public String oneLiner() {
        return String.format("~%,dt | %d calls | %d tools | $%.4f | %.1fs",
                totalTokens(), llmCalls(), toolCalls(), totalCostUsd(), sessionMs() / 1000.0);
    }

    /** Full summary for /stats command. */
    public String fullSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("┌─── Session Cost Summary ────────────────────────┐\n");
        sb.append(String.format("  Input tokens   : %,d%n", inputTokens()));
        sb.append(String.format("  Output tokens  : %,d%n", outputTokens()));
        sb.append(String.format("  Cached tokens  : %,d%n", cacheTokens()));
        sb.append(String.format("  Total tokens   : %,d%n", totalTokens()));
        sb.append(String.format("  LLM calls      : %d%n",  llmCalls()));
        sb.append(String.format("  Tool calls     : %d%n",  toolCalls()));
        sb.append(String.format("  Session time   : %.1fs%n", sessionMs() / 1000.0));
        sb.append(String.format("  Total cost     : $%.6f%n", totalCostUsd()));
        sb.append(String.format("  Cache savings  : $%.6f%n", cacheSavingsUsd()));
        sb.append(String.format("  Projected cost : $%.6f%n", projectedCostUsd()));
        if (usageByModel.size() > 1) {
            sb.append("\n  Per-model breakdown:\n");
            usageByModel.forEach((model, mu) ->
                    sb.append(String.format("    %-22s %,8dt  $%.6f%n",
                            model, mu.inputTokens.get() + mu.outputTokens.get(),
                            mu.totalCostUsd.get() / 1_000_000.0)));
        }
        sb.append("└─────────────────────────────────────────────────┘");
        return sb.toString();
    }

    /** Returns the last N call records for audit/display. */
    public List<CallRecord> recentCalls(int n) {
        synchronized (callHistory) {
            int from = Math.max(0, callHistory.size() - n);
            return new ArrayList<>(callHistory.subList(from, callHistory.size()));
        }
    }

    /** Exports state for session persistence / resumption. */
    public CostState exportState() {
        return new CostState(inputTokens(), outputTokens(), cacheTokens(),
                llmCalls(), toolCalls(), totalCostUsd(), sessionMs());
    }

    /** Restores state from a persisted session (--continue). */
    public void restoreState(CostState state) {
        totalInputTokens.set(state.inputTokens());
        totalOutputTokens.set(state.outputTokens());
        totalCacheTokens.set(state.cacheTokens());
        totalLlmCalls.set(state.llmCalls());
        totalToolCalls.set(state.toolCalls());
        // Adjust session start so elapsed time is accurate
        sessionStartMs.set(System.currentTimeMillis() - state.sessionMs());
        log.info("[cost] restored session state: {} tokens, ${:.4f}", totalTokens(), totalCostUsd());
    }

    /** Resets all counters. */
    public void reset() {
        totalInputTokens.set(0);
        totalOutputTokens.set(0);
        totalCacheTokens.set(0);
        totalLlmCalls.set(0);
        totalToolCalls.set(0);
        usageByModel.clear();
        synchronized (callHistory) { callHistory.clear(); }
        sessionStartMs.set(System.currentTimeMillis());
    }

    // ── Private ────────────────────────────────────────────────────────────

    private double calculateCost(String model, long input, long output, long cached) {
        double[] pricing = MODEL_PRICING.getOrDefault(model, new double[]{0, 0, 0});
        double inputCost  = (input - cached) * pricing[0] / 1_000_000.0;
        double cacheCost  = cached           * pricing[2] / 1_000_000.0;
        double outputCost = output           * pricing[1] / 1_000_000.0;
        return inputCost + cacheCost + outputCost;
    }

    // ── Data types ─────────────────────────────────────────────────────────

    private static final class ModelUsage {
        final String     model;
        final AtomicLong inputTokens  = new AtomicLong();
        final AtomicLong outputTokens = new AtomicLong();
        final AtomicLong cachedTokens = new AtomicLong();
        final AtomicLong calls        = new AtomicLong();
        final AtomicLong totalCostUsd = new AtomicLong(); // micro-dollars

        ModelUsage(String model) { this.model = model; }
    }

    public record CallRecord(
            String  model,
            long    inputTokens,
            long    outputTokens,
            long    cachedTokens,
            int     toolCalls,
            double  costUsd,
            Instant timestamp
    ) {}

    public record CostState(
            long   inputTokens,
            long   outputTokens,
            long   cacheTokens,
            long   llmCalls,
            long   toolCalls,
            double totalCostUsd,
            long   sessionMs
    ) {}
}
