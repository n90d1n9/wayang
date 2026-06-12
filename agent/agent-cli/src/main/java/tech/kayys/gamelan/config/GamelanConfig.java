package tech.kayys.gamelan.config;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Central configuration for the Gamelan CLI.
 *
 * <p>Configuration sources (in priority order):
 * <ol>
 *   <li>CLI command-line flags</li>
 *   <li>{@code ~/.gamelan/config.yml}</li>
 *   <li>MicroProfile config (application.properties, env vars)</li>
 *   <li>Hard-coded defaults</li>
 * </ol>
 */
@ApplicationScoped
public class GamelanConfig {

    @ConfigProperty(name = "gamelan.model", defaultValue = "qwen2.5-0.5b-instruct")
    String defaultModel;

    @ConfigProperty(name = "gamelan.skills.dir", defaultValue = "")
    String skillsDir;

    @ConfigProperty(name = "gamelan.history.size", defaultValue = "1000")
    int historySize;

    @ConfigProperty(name = "gamelan.stream", defaultValue = "true")
    boolean streamByDefault;

    @ConfigProperty(name = "gamelan.temperature", defaultValue = "0.7")
    double temperature;

    @ConfigProperty(name = "gamelan.max.tokens", defaultValue = "4096")
    int maxTokens;

    @ConfigProperty(name = "gamelan.engine.mode", defaultValue = "auto")
    String engineMode;

    @ConfigProperty(name = "gamelan.remote.url", defaultValue = "")
    String remoteUrl;

    @ConfigProperty(name = "gamelan.api.key", defaultValue = "community")
    String apiKey;

    @ConfigProperty(name = "gamelan.approve.mode", defaultValue = "auto")
    String approveMode;

    @ConfigProperty(name = "gamelan.sandbox.enabled", defaultValue = "false")
    boolean sandboxEnabled;

    @ConfigProperty(name = "gamelan.max.iterations", defaultValue = "20")
    int maxIterations;

    @ConfigProperty(name = "gamelan.context.window", defaultValue = "32768")
    int contextWindow;

    @ConfigProperty(name = "gamelan.session.persist", defaultValue = "true")
    boolean sessionPersist;

    public String defaultModel() { return defaultModel; }
    public String skillsDir() { return skillsDir; }
    public int historySize() { return historySize; }
    public boolean streamByDefault() { return streamByDefault; }
    public double temperature() { return temperature; }
    public int maxTokens() { return maxTokens; }
    public String engineMode() { return engineMode; }
    public String remoteUrl() { return remoteUrl; }
    public String apiKey() { return apiKey; }
    public String approveMode() { return approveMode; }
    public boolean sandboxEnabled() { return sandboxEnabled; }
    public int maxIterations() { return maxIterations; }
    public int contextWindow() { return contextWindow; }
    public boolean sessionPersist() { return sessionPersist; }
}
