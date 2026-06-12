package tech.kayys.gamelan.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import tech.kayys.gamelan.tool.ToolResult;

import java.time.Instant;
import java.util.List;

/**
 * Immutable result from one full agent loop turn.
 *
 * <p>The {@code text} field is the final, complete assistant response
 * (all iteration text concatenated). When streaming was active, this is
 * the same text that was already printed token-by-token to the terminal.
 *
 * <p>{@code hasError} is true when the LLM returned an {@code [LLM_ERROR]}
 * prefix, or when the loop was cancelled. Callers should check this before
 * using the text as authoritative output.
 *
 * <p>{@code tokenCount} is a rough estimate (text.length / 4) useful for
 * quota tracking and context window reporting. Zero means unknown.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentResponse(
        String text,
        List<String> skillsUsed,
        List<ToolResult> toolResults,
        List<String> intermediateMessages,
        boolean hasError,
        int tokenCount,
        Instant completedAt
) {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /** Clean text: strips the [LLM_ERROR] prefix if present. */
    public String cleanText() {
        if (text == null) return "";
        return text.startsWith("[LLM_ERROR]") ? text.substring("[LLM_ERROR]".length()).strip() : text;
    }

    public boolean isEmpty()      { return text == null || text.isBlank(); }
    public int     toolCallCount(){ return toolResults == null ? 0 : toolResults.size(); }

    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialization failed\"}";
        }
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String       text                = "";
        private List<String> skillsUsed          = List.of();
        private List<ToolResult> toolResults     = List.of();
        private List<String> intermediateMessages = List.of();
        private boolean      hasError            = false;

        public Builder text(String t)                    { this.text = t;                   return this; }
        public Builder skillsUsed(List<String> s)        { this.skillsUsed = s;             return this; }
        public Builder toolResults(List<ToolResult> r)   { this.toolResults = r;            return this; }
        public Builder intermediateMessages(List<String> m){ this.intermediateMessages = m; return this; }
        public Builder error(boolean e)                  { this.hasError = e;               return this; }

        public AgentResponse build() {
            int tokens = text == null ? 0 : text.length() / 4;
            return new AgentResponse(text, skillsUsed, toolResults,
                    intermediateMessages, hasError, tokens, Instant.now());
        }
    }
}
