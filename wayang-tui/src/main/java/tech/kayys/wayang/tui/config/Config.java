package tech.kayys.wayang.tui.config;
import tech.kayys.wayang.tools.spi.Tool;
import tech.kayys.wayang.sdk.json.Json;
import tech.kayys.wayang.sdk.json.JsonValue;

import tech.kayys.wayang.sdk.json.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * Application configuration: which AI providers are available, the
 * active profile (provider + model + system prompt + UI mode), and
 * agent-mode settings (tool permissions). Loaded from
 * ~/.agentic-tui/config.json, with environment variables as a
 * fallback/override for API keys so secrets don't have to live on disk.
 */
public final class Config {

    public enum UiMode { REPL, PANEL }
    public enum AgentMode { CHAT, AGENT }

    public List<ProviderConfig> providers = new ArrayList<>();
    public String activeProfile = "default";
    public List<Profile> profiles = new ArrayList<>();

    public static final class ProviderConfig {
        public String name;          // e.g. "anthropic", "openai", "ollama"
        public String type;          // which Provider implementation to use
        public String baseUrl;
        public String apiKeyEnv;     // name of env var holding the key
        public String apiKey;        // literal key (discouraged, but supported)
    }

    public static final class Profile {
        public String name;
        public String provider;
        public String model;
        public String systemPrompt = "";
        public UiMode uiMode = UiMode.REPL;
        public AgentMode agentMode = AgentMode.CHAT;
        public double temperature = 1.0;
        public int maxTokens = 4096;
        public boolean autoApproveTools = false; // if true, skip permission prompts
        public List<String> allowedTools = new ArrayList<>(); // empty = all tools allowed
    }

    public static Path defaultConfigDir() {
        String home = System.getProperty("user.home");
        return Paths.get(home, ".agentic-tui");
    }

    public static Path defaultConfigPath() {
        return defaultConfigDir().resolve("config.json");
    }

    /** Loads config from disk, or returns a sensible built-in default if none exists. */
    public static Config load(Path path) {
        if (Files.exists(path)) {
            try {
                String text = Files.readString(path, StandardCharsets.UTF_8);
                return fromJson(Json.parse(text));
            } catch (IOException e) {
                System.err.println("Warning: failed to read config at " + path + ": " + e.getMessage());
            }
        }
        return defaultConfig();
    }

    public void save(Path path) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, Json.writePretty(toJson()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.err.println("Warning: failed to save config: " + e.getMessage());
        }
    }

    public Profile activeProfile() {
        for (Profile p : profiles) {
            if (p.name.equals(activeProfile)) return p;
        }
        return profiles.isEmpty() ? null : profiles.get(0);
    }

    public ProviderConfig provider(String name) {
        for (ProviderConfig p : providers) {
            if (p.name.equals(name)) return p;
        }
        return null;
    }

    /** Resolves the API key for a provider: literal config value first, then env var. */
    public static String resolveApiKey(ProviderConfig pc) {
        if (pc.apiKey != null && !pc.apiKey.isBlank()) return pc.apiKey;
        if (pc.apiKeyEnv != null) {
            String v = System.getenv(pc.apiKeyEnv);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    public static Config defaultConfig() {
        Config c = new Config();

        ProviderConfig anthropic = new ProviderConfig();
        anthropic.name = "anthropic";
        anthropic.type = "anthropic";
        anthropic.baseUrl = "https://api.anthropic.com";
        anthropic.apiKeyEnv = "ANTHROPIC_API_KEY";
        c.providers.add(anthropic);

        ProviderConfig openai = new ProviderConfig();
        openai.name = "openai";
        openai.type = "openai";
        openai.baseUrl = "https://api.openai.com";
        openai.apiKeyEnv = "OPENAI_API_KEY";
        c.providers.add(openai);

        ProviderConfig ollama = new ProviderConfig();
        ollama.name = "ollama";
        ollama.type = "openai"; // ollama exposes an OpenAI-compatible endpoint
        ollama.baseUrl = "http://localhost:11434";
        ollama.apiKeyEnv = null;
        c.providers.add(ollama);

        ProviderConfig demo = new ProviderConfig();
        demo.name = "demo";
        demo.type = "demo";
        demo.baseUrl = "";
        c.providers.add(demo);

        Profile def = new Profile();
        def.name = "default";
        def.provider = "anthropic";
        def.model = "claude-sonnet-4-6";
        def.systemPrompt = "You are a helpful coding assistant running in a terminal UI.";
        def.uiMode = UiMode.REPL;
        def.agentMode = AgentMode.AGENT;
        c.profiles.add(def);
        c.activeProfile = "default";

        return c;
    }

    // ---------- JSON (de)serialization ----------

    public JsonValue toJson() {
        JsonValue root = JsonValue.object();
        JsonValue provArr = JsonValue.array();
        for (ProviderConfig pc : providers) {
            JsonValue p = JsonValue.object();
            p.put("name", pc.name);
            p.put("type", pc.type);
            p.put("baseUrl", pc.baseUrl);
            if (pc.apiKeyEnv != null) p.put("apiKeyEnv", pc.apiKeyEnv);
            if (pc.apiKey != null) p.put("apiKey", pc.apiKey);
            provArr.add(p);
        }
        root.put("providers", provArr);
        root.put("activeProfile", activeProfile);

        JsonValue profArr = JsonValue.array();
        for (Profile pr : profiles) {
            JsonValue p = JsonValue.object();
            p.put("name", pr.name);
            p.put("provider", pr.provider);
            p.put("model", pr.model);
            p.put("systemPrompt", pr.systemPrompt);
            p.put("uiMode", pr.uiMode.name());
            p.put("agentMode", pr.agentMode.name());
            p.put("temperature", pr.temperature);
            p.put("maxTokens", pr.maxTokens);
            p.put("autoApproveTools", pr.autoApproveTools);
            JsonValue tools = JsonValue.array();
            for (String t : pr.allowedTools) tools.add(JsonValue.of(t));
            p.put("allowedTools", tools);
            profArr.add(p);
        }
        root.put("profiles", profArr);
        return root;
    }

    public static Config fromJson(JsonValue root) {
        Config c = new Config();
        for (JsonValue p : root.get("providers").asArray()) {
            ProviderConfig pc = new ProviderConfig();
            pc.name = p.get("name").asString();
            pc.type = p.get("type").asString();
            pc.baseUrl = p.get("baseUrl").asString();
            pc.apiKeyEnv = p.has("apiKeyEnv") ? p.get("apiKeyEnv").asString() : null;
            pc.apiKey = p.has("apiKey") ? p.get("apiKey").asString() : null;
            c.providers.add(pc);
        }
        c.activeProfile = root.get("activeProfile").asString("default");
        for (JsonValue p : root.get("profiles").asArray()) {
            Profile pr = new Profile();
            pr.name = p.get("name").asString();
            pr.provider = p.get("provider").asString();
            pr.model = p.get("model").asString();
            pr.systemPrompt = p.get("systemPrompt").asString("");
            pr.uiMode = UiMode.valueOf(p.get("uiMode").asString("REPL"));
            pr.agentMode = AgentMode.valueOf(p.get("agentMode").asString("CHAT"));
            pr.temperature = p.has("temperature") ? p.get("temperature").asDouble() : 1.0;
            pr.maxTokens = p.has("maxTokens") ? p.get("maxTokens").asInt() : 4096;
            pr.autoApproveTools = p.get("autoApproveTools").asBoolean(false);
            for (JsonValue t : p.get("allowedTools").asArray()) pr.allowedTools.add(t.asString());
            c.profiles.add(pr);
        }
        if (c.profiles.isEmpty()) {
            return defaultConfig();
        }
        return c;
    }
}
