package tech.kayys.gamelan.session;

import tech.kayys.gamelan.agent.AgentResponse;
import tech.kayys.gamelan.agent.ConversationMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Manages a single conversation session with history, persistence, and token tracking.
 *
 * <p>A session:
 * <ul>
 *   <li>Tracks all messages in the conversation</li>
 *   <li>Estimates token usage</li>
 *   <li>Can be persisted to disk and resumed</li>
 *   <li>Supports clearing and truncating history</li>
 * </ul>
 */
public class ConversationSession {

    private final String id;
    private final Instant startedAt;
    private final List<ConversationMessage> messages = new CopyOnWriteArrayList<>();
    private final List<Turn> turns = new CopyOnWriteArrayList<>();
    private int totalTokenEstimate = 0;

    /**
     * Creates a new session.
     *
     * @param sessionId optional ID; if null a random UUID is generated
     */
    public ConversationSession(String sessionId) {
        this.id = sessionId != null && !sessionId.isBlank() ? sessionId : UUID.randomUUID().toString().substring(0, 8);
        this.startedAt = Instant.now();
    }

    public String id() { return id; }
    public Instant startedAt() { return startedAt; }

    /**
     * Returns all messages suitable for building an inference request.
     */
    public List<ConversationMessage> toMessages() {
        return List.copyOf(messages);
    }

    public List<Turn> turns() { return List.copyOf(turns); }
    public int turnCount() { return turns.size(); }

    /**
     * Approximate token count (1 token ≈ 4 chars for English text).
     */
    public int estimatedTokenCount() {
        return totalTokenEstimate;
    }

    public Duration duration() {
        return Duration.between(startedAt, Instant.now());
    }

    /**
     * Adds a user message and corresponding agent response to the session.
     */
    public void addTurn(String userInput, AgentResponse response) {
        messages.add(ConversationMessage.user(userInput));
        if (response != null && response.text() != null) {
            messages.add(ConversationMessage.assistant(response.text()));
            totalTokenEstimate += estimateTokens(userInput) + estimateTokens(response.text());
        }
        turns.add(new Turn(userInput, response, Instant.now()));
    }

    /**
     * Clears all messages but keeps the session ID and metadata.
     */
    public void clear() {
        messages.clear();
        turns.clear();
        totalTokenEstimate = 0;
    }

    /**
     * Truncates the message history to the last N turns (keeps system messages).
     */
    public void truncateToLast(int n) {
        if (n <= 0) {
            clear();
            return;
        }
        int toRemove = messages.size() - (n * 2); // each turn = user + assistant
        if (toRemove > 0) {
            for (int i = 0; i < toRemove && !messages.isEmpty(); i++) {
                messages.remove(0);
            }
        }
    }

    /**
     * Saves the session to a JSON file for later resumption.
     */
    public void save(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": \"").append(escapeJson(id)).append("\",\n");
        json.append("  \"startedAt\": \"").append(startedAt).append("\",\n");
        json.append("  \"tokenEstimate\": ").append(totalTokenEstimate).append(",\n");
        json.append("  \"messages\": [\n");
        for (int i = 0; i < messages.size(); i++) {
            ConversationMessage m = messages.get(i);
            json.append("    {\"role\": \"").append(escapeJson(m.role()))
                .append("\", \"content\": \"").append(escapeJson(m.content())).append("\"}");
            if (i < messages.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ]\n");
        json.append("}\n");
        Files.writeString(path, json.toString());
    }

    /**
     * Loads a session from a JSON file.
     */
    public static ConversationSession load(Path path) throws IOException {
        if (!Files.exists(path)) {
            return new ConversationSession(null);
        }
        String content = Files.readString(path);
        // Simple JSON parsing (in production, use Jackson)
        String id = extractJsonValue(content, "id");
        String startedAtStr = extractJsonValue(content, "startedAt");

        ConversationSession session = new ConversationSession(id);

        // Parse messages
        int msgsStart = content.indexOf("\"messages\": [");
        if (msgsStart >= 0) {
            int arrayStart = content.indexOf("[", msgsStart);
            int arrayEnd = content.lastIndexOf("]");
            if (arrayStart >= 0 && arrayEnd > arrayStart) {
                String msgsJson = content.substring(arrayStart + 1, arrayEnd);
                String[] msgBlocks = msgsJson.split("\\},\\s*\\{");
                for (String block : msgBlocks) {
                    String cleaned = block.replace("{", "").replace("}", "").trim();
                    String role = extractSimpleValue(cleaned, "role");
                    String msgContent = extractSimpleValue(cleaned, "content");
                    if (role != null && msgContent != null) {
                        session.messages.add(new ConversationMessage(role, msgContent));
                    }
                }
            }
        }

        return session;
    }

    // ── Turn record ────────────────────────────────────────────────────────

    public record Turn(String userInput, AgentResponse response, Instant timestamp) {}

    // ── Internal helpers ───────────────────────────────────────────────────

    private int estimateTokens(String text) {
        if (text == null) return 0;
        // Rough estimate: ~4 characters per token for English
        return text.length() / 4;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int idx = json.indexOf(searchKey);
        if (idx < 0) return "";
        int colonIdx = json.indexOf(":", idx);
        if (colonIdx < 0) return "";
        int valueStart = json.indexOf("\"", colonIdx);
        if (valueStart < 0) return "";
        int valueEnd = json.indexOf("\"", valueStart + 1);
        if (valueEnd < 0) return "";
        return json.substring(valueStart + 1, valueEnd);
    }

    private static String extractSimpleValue(String block, String key) {
        String searchKey = "\"" + key + "\"";
        int idx = block.indexOf(searchKey);
        if (idx < 0) return null;
        int colonIdx = block.indexOf(":", idx);
        if (colonIdx < 0) return null;
        int valueStart = block.indexOf("\"", colonIdx);
        if (valueStart < 0) return null;
        int valueEnd = block.indexOf("\"", valueStart + 1);
        if (valueEnd < 0) return null;
        return block.substring(valueStart + 1, valueEnd)
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
