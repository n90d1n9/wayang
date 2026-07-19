package tech.kayys.gamelan.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persistent configuration store backed to {@code ~/.gamelan/config.yml}.
 */
@ApplicationScoped
public class GamelanConfigStore {

    private static final String CONFIG_DIR_NAME = ".gamelan";
    private static final String CONFIG_FILE_NAME = "config.yml";

    // Config keys for approval/trust
    private static final String KEY_APPROVAL_MODE = "approval.mode";
    private static final String KEY_SANDBOX = "sandbox.enabled";
    private static final String KEY_TRUSTED_TOOLS_PREFIX = "trusted.tool.";

    private final Path configPath;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final Object lock = new Object();

    public GamelanConfigStore() {
        this.configPath = resolveConfigPath();
        load();
    }

    public String get(String key) { return cache.get(key); }
    public String get(String key, String defaultValue) { return cache.getOrDefault(key, defaultValue); }

    public void set(String key, String value) throws IOException {
        synchronized (lock) { cache.put(key, value); save(); }
    }

    public void remove(String key) throws IOException {
        synchronized (lock) { cache.remove(key); save(); }
    }

    public Map<String, String> getAll() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(cache));
    }

    public void reset() throws IOException {
        synchronized (lock) { cache.clear(); save(); }
    }

    public Path getConfigPath() { return configPath; }

    // ── Approval / Trust API ───────────────────────────────────────────────

    public void setApprovalMode(ApprovalMode mode) {
        cache.put(KEY_APPROVAL_MODE, mode.name());
        trySave();
    }

    public ApprovalMode getApprovalMode() {
        String val = cache.get(KEY_APPROVAL_MODE);
        if (val == null) return ApprovalMode.TRUSTED_TOOLS;
        try { return ApprovalMode.valueOf(val); } catch (IllegalArgumentException e) { return ApprovalMode.TRUSTED_TOOLS; }
    }

    public void addTrustedTool(String toolName, boolean all) {
        cache.put(KEY_TRUSTED_TOOLS_PREFIX + toolName, all ? "all" : "exact");
        trySave();
    }

    public boolean removeTrustedTool(String toolName) {
        boolean existed = cache.containsKey(KEY_TRUSTED_TOOLS_PREFIX + toolName);
        cache.remove(KEY_TRUSTED_TOOLS_PREFIX + toolName);
        if (existed) trySave();
        return existed;
    }

    public Set<String> getTrustedTools() {
        Set<String> result = new LinkedHashSet<>();
        cache.forEach((k, v) -> {
            if (k.startsWith(KEY_TRUSTED_TOOLS_PREFIX)) {
                result.add(k.substring(KEY_TRUSTED_TOOLS_PREFIX.length()));
            }
        });
        return Collections.unmodifiableSet(result);
    }

    public boolean isSandboxEnabled() {
        return Boolean.parseBoolean(cache.getOrDefault(KEY_SANDBOX, "false"));
    }

    // ── Internal ───────────────────────────────────────────────────────────

    private Path resolveConfigPath() {
        String configDir = System.getProperty("gamelan.config.dir");
        if (configDir != null && !configDir.isBlank()) return Path.of(configDir, CONFIG_FILE_NAME);
        return Path.of(System.getProperty("user.home"), CONFIG_DIR_NAME, CONFIG_FILE_NAME);
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(configPath)) return;
        try (InputStream is = Files.newInputStream(configPath)) {
            Map<String, Object> data = new Yaml().load(is);
            if (data != null) data.forEach((k, v) -> cache.put(k, v != null ? v.toString() : ""));
        } catch (IOException ignored) {}
    }

    private void trySave() {
        try { save(); } catch (IOException e) { /* best-effort */ }
    }

    private void save() throws IOException {
        Files.createDirectories(configPath.getParent());
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(configPath), StandardCharsets.UTF_8)) {
            yaml.dump(new LinkedHashMap<>(cache), writer);
        }
    }
}
