package tech.kayys.wayang.agent.orchestration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Conversation memory with intelligent windowing, token estimation, and disk persistence.
 */
public class ConversationMemory {

    private static final Logger log = LoggerFactory.getLogger(ConversationMemory.class);
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final int CHARS_PER_TOKEN = 4;

    private final int     maxMessages;
    private final int     maxTokenBudget;
    private final Deque<Turn> turns = new ArrayDeque<>();

    public record Turn(
            String role,
            Object content,
            long   timestampMs,
            int    estimatedTokens
    ) {
        @SuppressWarnings("unchecked")
        public List<Map<String, Object>> messages() {
            if (content instanceof List) return (List<Map<String, Object>>) content;
            return List.of(Map.of("role", role, "content", content));
        }
    }

    public ConversationMemory(int maxMessages) {
        this(maxMessages, 0);
    }

    public ConversationMemory(int maxMessages, int maxTokenBudget) {
        this.maxMessages    = maxMessages;
        this.maxTokenBudget = maxTokenBudget;
    }

    public void addUser(String text) {
        add("user", List.of(Map.of("type", "text", "text", text)), estimate(text));
    }

    public void addAssistant(List<Map<String, Object>> contentBlocks) {
        int tokens = contentBlocks.stream()
                .mapToInt(b -> estimate(b.toString()))
                .sum();
        add("assistant", contentBlocks, tokens);
    }

    public void addToolResult(String toolUseId, String result) {
        String stored = result;
        if (result != null && result.length() > 6000) {
            stored = result.substring(0, 3000)
                    + "\n\n...[result compressed]...\n\n"
                    + result.substring(result.length() - 1500);
        }

        List<Map<String, Object>> content = List.of(Map.of(
                "type",        "tool_result",
                "tool_use_id", toolUseId,
                "content",     stored != null ? stored : ""
        ));
        add("user", content, estimate(stored));
    }

    public List<Map<String, Object>> getMessages() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Turn t : turns) {
            Map<String, Object> msg = new LinkedHashMap<>();
            msg.put("role",    t.role());
            msg.put("content", t.content());
            result.add(msg);
        }
        return Collections.unmodifiableList(result);
    }

    public void clear() { turns.clear(); }
    public int size() { return turns.size(); }
    public int estimatedTokens() { return turns.stream().mapToInt(Turn::estimatedTokens).sum(); }

    private void add(String role, Object content, int estimatedTokens) {
        Turn turn = new Turn(role, content, System.currentTimeMillis(), estimatedTokens);
        turns.addLast(turn);
        while (turns.size() > maxMessages && turns.size() > 1) {
            turns.pollFirst();
        }
    }

    private static int estimate(String text) {
        if (text == null || text.isBlank()) return 0;
        return Math.max(1, text.length() / CHARS_PER_TOKEN);
    }
}
