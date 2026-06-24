package tech.kayys.wayang.gollek.cli;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lightweight shell-backed chat session that delegates inference to the local
 * `gollek` CLI. Best-effort: tries a small set of gollek subcommands and falls
 * back to a single-chunk response if none succeed.
 */
public final class ShellChatSession {
    private final String modelId;
    private final String providerId;
    private final boolean memory;
    private final List<Object> history = new ArrayList<>();
    private Object systemPrompt;

    public ShellChatSession(String modelId, String providerId, boolean memory) {
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
        private final ShellChatSession session;
        public Stream(String prompt, ShellChatSession s) { this.prompt = prompt; this.session = s; }
        public StreamSubscriber subscribe() { return new StreamSubscriber(prompt, session); }
    }

    public static final class StreamSubscriber {
        private final String prompt;
        private final ShellChatSession session;
        public StreamSubscriber(String prompt, ShellChatSession s) { this.prompt = prompt; this.session = s; }
        public <T> void with(Consumer<Chunk> onItem, Consumer<Throwable> onFailure, Runnable onComplete) {
            try {
                // Build a simple input payload: include system prompt + user prompt
                StringBuilder payload = new StringBuilder();
                if (session.systemPrompt != null) payload.append(session.systemPrompt.toString()).append("\n\n");
                payload.append(prompt == null ? "" : prompt);

                // Try a sequence of gollek commands that may exist on user's system.
                String[][] commands = new String[][]{
                    {"gollek", "run", session.modelId},
                    {"gollek", "infer", session.modelId},
                    {"gollek", "chat", session.modelId},
                    {"gollek", "generate", session.modelId}
                };

                boolean executed = false;
                String collected = null;
                for (String[] cmd : commands) {
                    try {
                        ProcessBuilder pb = new ProcessBuilder(cmd);
                        pb.redirectErrorStream(true);
                        Process p = pb.start();
                        // write to stdin if supported
                        try (var out = p.getOutputStream()) {
                            out.write(payload.toString().getBytes(StandardCharsets.UTF_8));
                            out.flush();
                        } catch (Exception ignored) {}

                        StringBuilder sb = new StringBuilder();
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                            String line;
                            long start = System.nanoTime();
                            while ((line = br.readLine()) != null) {
                                sb.append(line).append('\n');
                                // safeguard: time limit
                                if (Duration.ofNanos(System.nanoTime() - start).toSeconds() > 30) break;
                            }
                        }
                        int rc = p.waitFor();
                        if (rc == 0 && sb.length() > 0) {
                            executed = true;
                            collected = sb.toString().trim();
                            break;
                        }
                    } catch (Exception e) {
                        // try next command
                    }
                }

                if (!executed || collected == null) {
                    // Fallback to a helpful deterministic assistant
                    collected = "(fallback assistant — no local gollek command succeeded) I can help with: " + prompt;
                }

                // Add to history as {role,text}
                java.util.Map<String,Object> msg = java.util.Map.of(
                        "role", "assistant",
                        "text", collected,
                        "meta", java.util.Map.of("shell", true, "model", session.modelId)
                );
                session.addMessage(msg);

                if (onItem != null) onItem.accept(new Chunk(collected));
                if (onComplete != null) onComplete.run();
            } catch (Throwable t) {
                if (onFailure != null) onFailure.accept(t);
            }
        }
    }
}
