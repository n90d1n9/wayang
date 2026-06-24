package tech.kayys.wayang.gollek.cli;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Minimal in-process chat session used when Gollek SDK is not available.
 * Supports the subset of methods used by the CLI: getModelId/getProviderId,
 * reset, addMessage, getHistory, setSystemPrompt and a simple streaming API.
 */
public final class NoopChatSession {
    private String modelId;
    private String providerId;
    private final boolean memory;
    private final List<Object> history = new ArrayList<>();
    private Object systemPrompt;

    public NoopChatSession(String modelId, String providerId, boolean memory) {
        this.modelId = modelId == null ? "unknown-model" : modelId;
        this.providerId = providerId;
        this.memory = memory;
    }

    public void reset() { history.clear(); }
    public void addMessage(Object m) { if (m != null) history.add(m); }
    public List<Object> getHistory() { return List.copyOf(history); }
    public String getProviderId() { return providerId; }
    public String getModelId() { return modelId; }
    public void setSystemPrompt(Object prompt) { this.systemPrompt = prompt; }

    public Stream stream(String userPrompt) {
        return new Stream(userPrompt, this);
    }

    public static final class Chunk {
        private final String delta;
        public Chunk(String delta) { this.delta = delta; }
        public String getDelta() { return delta; }
    }

    public static final class Stream {
        private final String prompt;
        private final NoopChatSession session;
        public Stream(String prompt, NoopChatSession s) { this.prompt = prompt; this.session = s; }
        public StreamSubscriber subscribe() { return new StreamSubscriber(prompt, session); }
    }

    public static final class StreamSubscriber {
        private final String prompt;
        private final NoopChatSession session;
        public StreamSubscriber(String prompt, NoopChatSession s) { this.prompt = prompt; this.session = s; }
        public <T> void with(Consumer<Chunk> onItem, Consumer<Throwable> onFailure, Runnable onComplete) {
            try {
                // Simulated assistant response that mimics normal assistant format.
                String base = session.getModelId() == null ? "simulated-model" : session.getModelId();
                String replyText = "(simulated assistant — model=" + base + ") I can help with: " + (prompt == null ? "<empty>" : prompt);
                // Add to history as a simple map-like object so persistence won't rely on gollek.Message
                java.util.Map<String,Object> msg = java.util.Map.of(
                        "role", "assistant",
                        "text", replyText,
                        "meta", java.util.Map.of("simulated", true, "model", base)
                );
                session.addMessage(msg);
                if (onItem != null) onItem.accept(new Chunk(replyText));
                if (onComplete != null) onComplete.run();
            } catch (Throwable t) {
                if (onFailure != null) onFailure.accept(t);
            }
        }
    }
}
