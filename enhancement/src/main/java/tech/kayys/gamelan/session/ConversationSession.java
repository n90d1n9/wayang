package tech.kayys.gamelan.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.agent.AgentResponse;
import tech.kayys.gamelan.agent.ConversationMessage;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;

/**
 * Tracks conversation history for a single agent session.
 *
 * <h2>Token budget management</h2>
 * Each turn is assigned an estimated token count (4 chars ≈ 1 token).
 * When the running total exceeds {@code maxTokenBudget}, the oldest turns
 * are dropped first. This prevents context-window overflows on long sessions
 * without hard-coding a turn limit.
 *
 * <h2>Persistence</h2>
 * When {@code persist=true}, the session is serialised to
 * {@code ~/.gamelan/sessions/<id>.json} after every turn. Sessions can be
 * resumed by passing the session ID back to the CLI.
 *
 * <h2>Thread safety</h2>
 * {@code addTurn} and {@code toMessages} are synchronised so the workflow
 * engine can write from parallel threads without corrupting history.
 */
public class ConversationSession {

    private static final Logger log = LoggerFactory.getLogger(ConversationSession.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /** Approximate token budget for history (reserve headroom for the response). */
    private static final int DEFAULT_TOKEN_BUDGET = 6_000;

    private final String id;
    private final Instant createdAt;
    private final List<Turn> turns = new ArrayList<>();
    private final boolean persist;
    private final int tokenBudget;

    public ConversationSession(String existingId) {
        this(existingId, false, DEFAULT_TOKEN_BUDGET);
    }

    public ConversationSession(String existingId, boolean persist, int tokenBudget) {
        this.id          = (existingId != null && !existingId.isBlank())
                ? existingId : "sess-" + Long.toHexString(System.currentTimeMillis());
        this.createdAt   = Instant.now();
        this.persist     = persist;
        this.tokenBudget = tokenBudget;

        if (persist) {
            tryLoad();
        }
    }

    // ── Turn management ────────────────────────────────────────────────────

    /** Appends a completed exchange and enforces the token budget. */
    public synchronized void addTurn(String userMessage, AgentResponse response) {
        int userTokens      = estimateTokens(userMessage);
        int assistantTokens = estimateTokens(response.text());
        turns.add(new Turn(userMessage, response.text(), Instant.now(),
                userTokens + assistantTokens));
        trimToTokenBudget();
        if (persist) trySave();
    }

    /**
     * Returns the flat message list (user/assistant alternation) for inference.
     * The system message is NOT included here — it's handled by PromptBuilder.
     */
    public synchronized List<ConversationMessage> toMessages() {
        List<ConversationMessage> messages = new ArrayList<>(turns.size() * 2);
        for (Turn turn : turns) {
            messages.add(ConversationMessage.user(turn.userMessage()));
            messages.add(ConversationMessage.assistant(turn.assistantResponse()));
        }
        return messages;
    }

    public synchronized void clear() { turns.clear(); }

    // ── Accessors ──────────────────────────────────────────────────────────

    public String  id()          { return id; }
    public Instant createdAt()   { return createdAt; }
    public synchronized int turnCount()   { return turns.size(); }
    public synchronized int tokenCount()  { return turns.stream().mapToInt(Turn::estimatedTokens).sum(); }

    // ── Private ────────────────────────────────────────────────────────────

    private void trimToTokenBudget() {
        while (!turns.isEmpty() && tokenCount() > tokenBudget) {
            Turn removed = turns.remove(0);
            log.debug("Trimmed session turn to fit token budget (~{} tokens removed)",
                    removed.estimatedTokens());
        }
    }

    private static int estimateTokens(String text) {
        if (text == null) return 0;
        return Math.max(1, text.length() / 4);
    }

    // ── Persistence ────────────────────────────────────────────────────────

    private Path sessionFile() {
        return Path.of(System.getProperty("user.home"), ".gamelan", "sessions", id + ".json");
    }

    private void trySave() {
        try {
            Path file = sessionFile();
            Files.createDirectories(file.getParent());
            MAPPER.writeValue(file.toFile(), new SessionData(id, createdAt, turns));
        } catch (IOException e) {
            log.warn("Cannot save session {}: {}", id, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void tryLoad() {
        Path file = sessionFile();
        if (!Files.exists(file)) return;
        try {
            SessionData data = MAPPER.readValue(file.toFile(), SessionData.class);
            if (data.turns() != null) {
                turns.addAll(data.turns());
                trimToTokenBudget();
                log.info("Resumed session {} ({} turns, ~{} tokens)",
                        id, turns.size(), tokenCount());
            }
        } catch (IOException e) {
            log.warn("Cannot load session {}: {}", id, e.getMessage());
        }
    }

    /** A single user-assistant exchange with token estimate. */
    public record Turn(String userMessage, String assistantResponse,
                       Instant timestamp, int estimatedTokens) {}

    /** JSON envelope for session persistence. */
    private record SessionData(String id, Instant createdAt, List<Turn> turns) {}
}
