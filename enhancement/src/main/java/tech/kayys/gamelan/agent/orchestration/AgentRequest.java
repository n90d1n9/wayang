package tech.kayys.gamelan.agent.orchestration;

import tech.kayys.gamelan.session.ConversationSession;

import java.util.List;
import java.util.Map;

/**
 * Immutable input to any agent orchestrator tier.
 *
 * <p>All three tiers share this request type so the caller can switch
 * strategy without changing how it builds requests.
 *
 * <h2>Builder default for session</h2>
 * The default session in the builder uses {@code DEFAULT_TOKEN_BUDGET = 6000}
 * which matches {@link ConversationSession#DEFAULT_TOKEN_BUDGET}. Callers that
 * want to honour user config should pass a pre-built session via
 * {@link Builder#session(ConversationSession)}.
 *
 * @param task         the user's raw task string
 * @param model        LLM model ID (null → use configured default)
 * @param session      conversation history and context
 * @param stream       whether to stream tokens to stdout
 * @param maxSteps     iteration limit for the ReAct loop (default: 10)
 * @param systemExtra  extra text appended to the system prompt
 * @param params       strategy-specific parameters
 * @param allowedTools optional tool name allowlist (null → all tools allowed)
 */
public record AgentRequest(
        String              task,
        String              model,
        ConversationSession session,
        boolean             stream,
        int                 maxSteps,
        String              systemExtra,
        Map<String, Object> params,
        List<String>        allowedTools
) {
    public static Builder builder(String task) { return new Builder(task); }

    public boolean hasToolFilter() {
        return allowedTools != null && !allowedTools.isEmpty();
    }

    @SuppressWarnings("unchecked")
    public <T> T param(String key, T defaultValue) {
        if (params == null) return defaultValue;
        Object v = params.get(key);
        if (v == null) return defaultValue;
        try { return (T) v; } catch (ClassCastException e) { return defaultValue; }
    }

    public static final class Builder {
        private final String task;
        private String              model        = null;
        private ConversationSession session      = null; // resolved lazily in build()
        private boolean             stream       = false;
        private int                 maxSteps     = 10;
        private String              systemExtra  = "";
        private Map<String, Object> params       = Map.of();
        private List<String>        allowedTools = null;

        Builder(String task) { this.task = task; }

        public Builder model(String m)               { this.model = m; return this; }
        public Builder session(ConversationSession s) { this.session = s; return this; }
        public Builder stream(boolean s)             { this.stream = s; return this; }
        public Builder maxSteps(int n)               { this.maxSteps = n; return this; }
        public Builder systemExtra(String s)         { this.systemExtra = s != null ? s : ""; return this; }
        public Builder params(Map<String, Object> p) { this.params = p != null ? p : Map.of(); return this; }
        public Builder allowedTools(List<String> t)  { this.allowedTools = t; return this; }

        public AgentRequest build() {
            // Null-safe session: callers that don't set one get a fresh default session.
            // This avoids the default field calling ConversationSession constructor at
            // class-load time when CDI may not have initialised all beans yet.
            ConversationSession sess = session != null
                    ? session
                    : new ConversationSession(null);
            return new AgentRequest(task, model, sess, stream,
                    maxSteps, systemExtra, params, allowedTools);
        }
    }
}
