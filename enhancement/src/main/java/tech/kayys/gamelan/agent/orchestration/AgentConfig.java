package tech.kayys.gamelan.agent.orchestration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable per-project agent configuration, loaded from {@code .gamelan/agent.json}.
 *
 * <p>Adapted from the Wayang {@code AgentConfig} in the uploaded orchestration
 * sources. Provides project-level overrides that take precedence over
 * {@link tech.kayys.gamelan.config.GamelanConfig} global defaults.
 *
 * <h2>Usage</h2>
 * <pre>
 * # .gamelan/agent.json
 * {
 *   "model": "qwen2-7b",
 *   "max_iterations": 15,
 *   "temperature": 0.3,
 *   "tool_timeout_seconds": 120,
 *   "system_prompt_extra": "Always use records instead of POJOs."
 * }
 * </pre>
 *
 * <h2>Load priority</h2>
 * <ol>
 *   <li>{@code .gamelan/agent.json} — project-local (highest priority)</li>
 *   <li>{@code ~/.gamelan/agent.json} — user-global</li>
 *   <li>Application config ({@code application.yml}) defaults</li>
 * </ol>
 */
public record AgentConfig(
        String  model,
        int     maxIterations,
        double  temperature,
        int     maxTokens,
        int     toolTimeoutSeconds,
        boolean streamOutput,
        boolean showMetrics,
        String  systemPromptExtra,
        String  workingDirectory
) {
    private static final Logger log = LoggerFactory.getLogger(AgentConfig.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Defaults ───────────────────────────────────────────────────────────

    public static AgentConfig defaults() {
        return new AgentConfig(
                null,          // model — falls back to GamelanConfig
                10,            // maxIterations
                0.7,           // temperature
                4096,          // maxTokens
                60,            // toolTimeoutSeconds
                true,          // streamOutput
                true,          // showMetrics
                "",            // systemPromptExtra
                System.getProperty("user.dir")
        );
    }

    // ── File loading ───────────────────────────────────────────────────────

    /**
     * Loads agent config by searching the standard locations.
     * Returns {@link #defaults()} if no file is found.
     */
    public static AgentConfig load() {
        // 1. Project-local
        Path local = Path.of(".gamelan", "agent.json");
        if (Files.exists(local)) {
            try { return fromFile(local); }
            catch (Exception e) { log.warn("Cannot read .gamelan/agent.json: {}", e.getMessage()); }
        }

        // 2. User-global
        Path global = Path.of(System.getProperty("user.home"), ".gamelan", "agent.json");
        if (Files.exists(global)) {
            try { return fromFile(global); }
            catch (Exception e) { log.warn("Cannot read ~/.gamelan/agent.json: {}", e.getMessage()); }
        }

        return defaults();
    }

    /**
     * Loads from a specific JSON file. Missing fields use defaults.
     */
    public static AgentConfig fromFile(Path path) throws IOException {
        JsonNode node = MAPPER.readTree(Files.readString(path));
        AgentConfig def = defaults();
        return new AgentConfig(
                str(node, "model",                def.model()),
                intv(node, "max_iterations",      def.maxIterations()),
                dblv(node, "temperature",         def.temperature()),
                intv(node, "max_tokens",          def.maxTokens()),
                intv(node, "tool_timeout_seconds",def.toolTimeoutSeconds()),
                bool(node, "stream_output",       def.streamOutput()),
                bool(node, "show_metrics",        def.showMetrics()),
                str(node, "system_prompt_extra",  def.systemPromptExtra()),
                str(node, "working_directory",    def.workingDirectory())
        );
    }

    /**
     * Saves this config (without API key) to the given path.
     */
    public void saveToFile(Path path) throws IOException {
        Files.createDirectories(path.getParent());
        Map<String, Object> map = new LinkedHashMap<>();
        if (model() != null && !model().isBlank()) map.put("model", model());
        map.put("max_iterations",       maxIterations());
        map.put("temperature",          temperature());
        map.put("max_tokens",           maxTokens());
        map.put("tool_timeout_seconds", toolTimeoutSeconds());
        map.put("stream_output",        streamOutput());
        map.put("show_metrics",         showMetrics());
        if (!systemPromptExtra().isBlank()) map.put("system_prompt_extra", systemPromptExtra());
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), map);
        log.info("Agent config saved to {}", path);
    }

    // ── Merging ────────────────────────────────────────────────────────────

    /**
     * Returns a copy of this config with {@code model} overridden if the
     * argument is non-blank. Used when {@code --model} CLI flag is supplied.
     */
    public AgentConfig withModel(String override) {
        if (override == null || override.isBlank()) return this;
        return new AgentConfig(override, maxIterations, temperature, maxTokens,
                toolTimeoutSeconds, streamOutput, showMetrics, systemPromptExtra, workingDirectory);
    }

    // ── Private helpers ────────────────────────────────────────────────────

    private static String  str(JsonNode n, String key, String def)  { return n.has(key) ? n.get(key).asText(def) : def; }
    private static int     intv(JsonNode n, String key, int def)    { return n.has(key) ? n.get(key).asInt(def)  : def; }
    private static double  dblv(JsonNode n, String key, double def) { return n.has(key) ? n.get(key).asDouble(def) : def; }
    private static boolean bool(JsonNode n, String key, boolean def){ return n.has(key) ? n.get(key).asBoolean(def) : def; }
}
