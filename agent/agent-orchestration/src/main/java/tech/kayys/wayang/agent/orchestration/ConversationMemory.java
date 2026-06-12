package tech.kayys.wayang.agent.orchestration;

import tech.kayys.wayang.agent.spi.InferenceTypes.AssistantMessage;
import tech.kayys.wayang.agent.spi.InferenceTypes.ChatMessage;
import tech.kayys.wayang.agent.spi.InferenceTypes.ToolResultMessage;
import tech.kayys.wayang.agent.spi.InferenceTypes.UserMessage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

/**
 * Conversation memory with intelligent windowing, token estimation, and disk persistence.
 */
public class ConversationMemory {

    private static final int CHARS_PER_TOKEN = 4;

    private final int     maxMessages;
    private final int     maxTokenBudget;
    private final Deque<ChatMessage> messages = new ArrayDeque<>();

    public ConversationMemory(int maxMessages) {
        this(maxMessages, 0);
    }

    public ConversationMemory(int maxMessages, int maxTokenBudget) {
        this.maxMessages    = maxMessages;
        this.maxTokenBudget = maxTokenBudget;
    }

    public void addUser(String text) {
        ChatMessage msg = new UserMessage(text);
        add(msg, estimate(text));
    }

    public void addAssistant(String content) {
        ChatMessage msg = new AssistantMessage(content);
        add(msg, estimate(content));
    }

    public void addToolResult(String toolCallId, String result) {
        String stored = result;
        if (result != null && result.length() > 6000) {
            stored = result.substring(0, 3000)
                    + "\n\n...[result compressed]...\n\n"
                    + result.substring(result.length() - 1500);
        }

        ChatMessage msg = new ToolResultMessage(toolCallId, null, stored);
        add(msg, estimate(stored));
    }

    public List<ChatMessage> getMessages() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public void clear() { messages.clear(); }
    public int size() { return messages.size(); }
    public int estimatedTokens() { 
        return messages.stream()
                .mapToInt(m -> estimate(m.content()))
                .sum(); 
    }

    private void add(ChatMessage message, int estimatedTokens) {
        messages.addLast(message);
        while (messages.size() > maxMessages && messages.size() > 1) {
            messages.pollFirst();
        }
        while (maxTokenBudget > 0 && estimatedTokens() > maxTokenBudget && messages.size() > 1) {
            messages.pollFirst();
        }
    }

    private static int estimate(String text) {
        if (text == null || text.isBlank()) return 0;
        return Math.max(1, text.length() / CHARS_PER_TOKEN);
    }
}
