package tech.kayys.wayang.agent.orchestration;

/**
 * Immutable configuration for the coding agent.
 *
 * Build with: AgentConfig.builder().apiKey("sk-ant-...").build()
 */
public record AgentConfig(
        String  apiKey,
        String  model,
        int     maxTokens,
        int     maxIterations,
        String  workingDirectory,
        boolean verbose,
        boolean streamOutput,
        int     toolTimeoutSeconds,
        int     maxRetries,
        boolean showMetrics,
        String  systemPromptExtra   // appended to the built-in system prompt
) {
    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String  apiKey;
        private String  model               = "claude-sonnet-4-20250514";
        private int     maxTokens           = 8192;
        private int     maxIterations       = 20;
        private String  workingDirectory    = System.getProperty("user.dir");
        private boolean verbose             = true;
        private boolean streamOutput        = false;   // false = batch mode
        private int     toolTimeoutSeconds  = 30;
        private int     maxRetries          = 4;
        private boolean showMetrics         = true;
        private String  systemPromptExtra   = "";

        public Builder apiKey(String v)             { this.apiKey = v; return this; }
        public Builder model(String v)              { this.model = v; return this; }
        public Builder maxTokens(int v)             { this.maxTokens = v; return this; }
        public Builder maxIterations(int v)         { this.maxIterations = v; return this; }
        public Builder workingDirectory(String v)   { this.workingDirectory = v; return this; }
        public Builder verbose(boolean v)           { this.verbose = v; return this; }
        public Builder streamOutput(boolean v)      { this.streamOutput = v; return this; }
        public Builder toolTimeoutSeconds(int v)    { this.toolTimeoutSeconds = v; return this; }
        public Builder maxRetries(int v)            { this.maxRetries = v; return this; }
        public Builder showMetrics(boolean v)       { this.showMetrics = v; return this; }
        public Builder systemPromptExtra(String v)  { this.systemPromptExtra = v; return this; }

        public AgentConfig build() {
            if (apiKey == null || apiKey.isBlank())
                throw new IllegalArgumentException("ANTHROPIC_API_KEY is required");
            return new AgentConfig(apiKey, model, maxTokens, maxIterations,
                    workingDirectory, verbose, streamOutput, toolTimeoutSeconds,
                    maxRetries, showMetrics, systemPromptExtra);
        }
    }

    // ──────────────────────────────────────────────────────────
    // File-based config
    // ──────────────────────────────────────────────────────────

    /**
     * Load AgentConfig from a JSON file (e.g. .agent.json).
     * All fields are optional — missing fields use defaults.
     *
     * Example .agent.json:
     * {
     *   "model": "claude-opus-4-20250514",
     *   "max_iterations": 30,
     *   "working_directory": "/my/project",
     *   "stream_output": true,
     *   "system_prompt_extra": "Always use records instead of POJOs."
     * }
     */
    public static AgentConfig fromFile(java.nio.file.Path path, String apiKey) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.databind.JsonNode node = mapper.readTree(java.nio.file.Files.readString(path));

        return AgentConfig.builder()
                .apiKey(apiKey)
                .model(          node.has("model")               ? node.get("model").asText()               : "claude-sonnet-4-20250514")
                .maxTokens(      node.has("max_tokens")          ? node.get("max_tokens").asInt()           : 8192)
                .maxIterations(  node.has("max_iterations")      ? node.get("max_iterations").asInt()       : 20)
                .workingDirectory(node.has("working_directory")  ? node.get("working_directory").asText()  : System.getProperty("user.dir"))
                .streamOutput(   node.has("stream_output")       && node.get("stream_output").asBoolean())
                .showMetrics(    !node.has("show_metrics")       || node.get("show_metrics").asBoolean())
                .toolTimeoutSeconds(node.has("tool_timeout_seconds") ? node.get("tool_timeout_seconds").asInt() : 30)
                .maxRetries(     node.has("max_retries")         ? node.get("max_retries").asInt()          : 4)
                .systemPromptExtra(node.has("system_prompt_extra") ? node.get("system_prompt_extra").asText() : "")
                .build();
    }

    /**
     * Save this config as a JSON file (omitting the API key for security).
     */
    public void saveToFile(java.nio.file.Path path) throws Exception {
        com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper()
                    .enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("model",                model());
        map.put("max_tokens",           maxTokens());
        map.put("max_iterations",       maxIterations());
        map.put("working_directory",    workingDirectory());
        map.put("stream_output",        streamOutput());
        map.put("show_metrics",         showMetrics());
        map.put("tool_timeout_seconds", toolTimeoutSeconds());
        map.put("max_retries",          maxRetries());
        if (!systemPromptExtra().isBlank()) map.put("system_prompt_extra", systemPromptExtra());
        java.nio.file.Files.writeString(path, mapper.writeValueAsString(map),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING);
    }

}