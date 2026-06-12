package tech.kayys.gamelan.agent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.orchestration.AgentRequest;
import tech.kayys.gamelan.agent.orchestration.DirectCallOrchestrator;
import tech.kayys.gamelan.config.GamelanConfig;
import tech.kayys.gamelan.session.ConversationSession;

/**
 * Intelligent conversation compactor — summarises history when context fills.
 *
 * <h2>Why this matters</h2>
 * Claude Code has a {@code /compact} command that uses the LLM to summarise
 * the conversation, preserving key facts while dropping verbatim back-and-forth.
 * Without this, long sessions either hit the context limit and fail, or
 * silently drop the oldest turns (losing important context).
 *
 * <h2>How it works</h2>
 * <ol>
 *   <li>Detect when {@code session.tokenCount()} exceeds the compaction threshold</li>
 *   <li>Ask the LLM (via {@link DirectCallOrchestrator}) to summarise all turns</li>
 *   <li>Clear the session history</li>
 *   <li>Inject the summary as the first assistant turn so it's in context</li>
 * </ol>
 *
 * <h2>Compaction threshold</h2>
 * Triggers at 80% of the configured token budget to leave headroom for the
 * summary itself. Can be triggered manually via {@code /compact} in the REPL.
 */
@ApplicationScoped
public class ConversationCompactor {

    private static final Logger log = LoggerFactory.getLogger(ConversationCompactor.class);

    /** Compact when session reaches this fraction of the token budget. */
    private static final double COMPACT_THRESHOLD = 0.80;

    @Inject DirectCallOrchestrator direct;
    @Inject GamelanConfig          config;

    /**
     * Compacts the session if it has grown beyond the threshold.
     * No-op if the session is still within budget.
     *
     * @param session  the session to potentially compact
     * @param model    LLM model to use for summarisation
     * @return true if compaction was performed
     */
    public boolean compactIfNeeded(ConversationSession session, String model) {
        int budget    = config.tokenBudget();
        int current   = session.tokenCount();
        int threshold = (int) (budget * COMPACT_THRESHOLD);

        if (current < threshold) return false;

        log.info("[compact] session token count {} exceeds threshold {} — compacting",
                current, threshold);
        compact(session, model);
        return true;
    }

    /**
     * Unconditionally summarises and compacts the session history.
     *
     * @param session the conversation session
     * @param model   LLM model for summarisation
     * @return the summary text
     */
    public String compact(ConversationSession session, String model) {
        if (session.turnCount() == 0) return "(empty session)";

        String history = buildHistoryText(session);
        String compactPrompt = """
                Summarise the following conversation history concisely.
                Preserve:
                - All decisions made and reasons given
                - Code changes applied and files modified
                - Bugs found and fixes applied
                - Commands run and their outcomes
                - Any user preferences expressed
                - Remaining open tasks

                Do NOT include: verbatim code snippets, full file contents,
                or redundant back-and-forth. Be dense and factual.

                CONVERSATION HISTORY:
                """ + history;

        AgentRequest req = AgentRequest.builder(compactPrompt)
                .model(model)
                .session(new ConversationSession(null)) // fresh session for the summary call
                .stream(false)
                .build();

        try {
            var result = direct.execute(req);
            String summary = result.answer().strip();

            // Clear the session and inject the summary as context
            session.clear();
            session.addTurn("[Context compacted — summary follows]",
                    AgentResponse.builder()
                            .text("## Session Summary\n\n" + summary
                                    + "\n\n[End of compacted history — continue from here]")
                            .build());

            log.info("[compact] done — summary is ~{} chars", summary.length());
            return summary;
        } catch (Exception e) {
            log.warn("[compact] summarisation failed: {}", e.getMessage());
            // Fall back to simple clear
            session.clear();
            return "(compaction failed — history cleared)";
        }
    }

    private String buildHistoryText(ConversationSession session) {
        StringBuilder sb = new StringBuilder();
        var messages = session.toMessages();
        for (int i = 0; i < messages.size(); i += 2) {
            if (i < messages.size()) {
                sb.append("User: ").append(messages.get(i).content()).append("\n\n");
            }
            if (i + 1 < messages.size()) {
                // Truncate very long assistant messages
                String asst = messages.get(i + 1).content();
                if (asst.length() > 1000) asst = asst.substring(0, 1000) + "…";
                sb.append("Assistant: ").append(asst).append("\n\n");
            }
        }
        return sb.toString();
    }
}
