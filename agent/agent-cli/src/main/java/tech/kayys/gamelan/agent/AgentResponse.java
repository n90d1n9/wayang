package tech.kayys.gamelan.agent;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import tech.kayys.gamelan.tool.ToolResult;

import java.util.List;

/**
 * The assembled response from a single agent loop turn.
 *
 * <p>Captures the final text, which skills were activated, which tools were called,
 * and any intermediate reasoning messages from multi-step turns.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AgentResponse(
        String text,
        List<String> skillsUsed,
        List<ToolResult> toolResults,
        List<String> intermediateMessages,
        boolean hasError,
        int inputTokens,
        int outputTokens
) {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    public static Builder builder() { return new Builder(); }

    public String toJson() {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            return "{\"error\": \"serialization failed\"}";
        }
    }

    public int totalTokens() {
        return inputTokens + outputTokens;
    }

    public static class Builder {
        private String text = "";
        private List<String> skillsUsed = List.of();
        private List<ToolResult> toolResults = List.of();
        private List<String> intermediateMessages = List.of();
        private boolean hasError = false;
        private int inputTokens = 0;
        private int outputTokens = 0;

        public Builder text(String text) { this.text = text; return this; }
        public Builder skillsUsed(List<String> skills) { this.skillsUsed = skills; return this; }
        public Builder toolResults(List<ToolResult> results) { this.toolResults = results; return this; }
        public Builder intermediateMessages(List<String> msgs) { this.intermediateMessages = msgs; return this; }
        public Builder error(boolean error) { this.hasError = error; return this; }
        public Builder inputTokens(int tokens) { this.inputTokens = tokens; return this; }
        public Builder outputTokens(int tokens) { this.outputTokens = tokens; return this; }

        public AgentResponse build() {
            return new AgentResponse(text, skillsUsed, toolResults, intermediateMessages, hasError, inputTokens, outputTokens);
        }
    }
}
