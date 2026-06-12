package tech.kayys.gamelan.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Reads and writes persistent configuration to {@code ~/.gamelan/config.yml}.
 *
 * <p>Supported keys:
 * <pre>
 *   model            → gamelan.default.model
 *   skills.dir       → gamelan.skills.dir
 *   temperature      → gamelan.temperature
 *   max.tokens       → gamelan.max.tokens
 *   stream           → gamelan.stream.default
 *   engine.mode      → gamelan.engine.mode
 *   remote.url       → gamelan.remote.url
 *   history.size     → gamelan.history.size
 * </pre>
 */
@ApplicationScoped
public class GamelanConfigStore {

    private static final Logger log = LoggerFactory.getLogger(GamelanConfigStore.class);

    private static final Path CONFIG_FILE = Path.of(
            System.getProperty("user.home"), ".gamelan", "config.yml");

    // Map of user-facing key → config file key
    private static final Map<String, String> KEY_MAP = Map.of(
            "model",       "default-model",
            "skills.dir",  "skills-dir",
            "temperature", "temperature",
            "max.tokens",  "max-tokens",
            "stream",      "stream-default",
            "engine.mode", "engine-mode",
            "remote.url",  "remote-url",
            "history.size","history-size"
    );

    private static final Map<String, String> DEFAULTS = Map.of(
            "default-model",    "llama3",
            "skills-dir",       "",
            "temperature",      "0.7",
            "max-tokens",       "4096",
            "stream-default",   "true",
            "engine-mode",      "auto",
            "remote-url",       "",
            "history-size",     "50"
    );

    /**
     * Gets a configuration value by user-facing key.
     *
     * @param key user-facing key (e.g. "model")
     * @return current value, or null if not set and no default
     */
    public String get(String key) {
        String configKey = KEY_MAP.getOrDefault(key, key);
        Map<String, Object> config = load();
        Object value = config.get(configKey);
        if (value != null) return value.toString();
        return DEFAULTS.get(configKey);
    }

    /**
     * Sets a configuration value and writes to disk.
     *
     * @param key   user-facing key
     * @param value new value
     */
    public void set(String key, String value) throws IOException {
        String configKey = KEY_MAP.getOrDefault(key, key);
        if (!KEY_MAP.containsKey(key) && !DEFAULTS.containsKey(key)) {
            throw new IllegalArgumentException("Unknown config key: " + key
                    + ". Valid keys: " + String.join(", ", KEY_MAP.keySet()));
        }
        Map<String, Object> config = new LinkedHashMap<>(load());
        config.put(configKey, parseValue(value));
        save(config);
        log.info("Config set: {} = {}", configKey, value);
    }

    /**
     * Resets all configuration to defaults.
     */
    public void reset() throws IOException {
        save(new LinkedHashMap<>(DEFAULTS));
    }

    // ── Private ────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> load() {
        if (!Files.exists(CONFIG_FILE)) return new LinkedHashMap<>();
        try {
            String content = Files.readString(CONFIG_FILE);
            Map<String, Object> result = new Yaml().load(content);
            return result != null ? result : new LinkedHashMap<>();
        } catch (IOException e) {
            log.warn("Cannot read config file: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private void save(Map<String, Object> config) throws IOException {
        Files.createDirectories(CONFIG_FILE.getParent());
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        String yaml = "# Gamelan CLI Configuration\n# Edit this file or use: gamelan config set <key> <value>\n\n"
                + new Yaml(options).dump(config);
        Files.writeString(CONFIG_FILE, yaml);
    }

    private Object parseValue(String value) {
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException ignored) {}
        try { return Double.parseDouble(value); }
        catch (NumberFormatException ignored) {}
        return value;
    }
}
