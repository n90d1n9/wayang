package tech.kayys.gamelan.config;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Typed access to all Gamelan configuration properties.
 *
 * <p>Sources (in priority order):
 * <ol>
 *   <li>Environment variables: {@code GAMELAN_DEFAULT_MODEL}, {@code GAMELAN_ENGINE_MODE}, …</li>
 *   <li>System properties: {@code -Dgamelan.default.model=…}</li>
 *   <li>User config file: {@code ~/.gamelan/config.yml} (loaded via custom ConfigSource)</li>
 *   <li>Application defaults: {@code src/main/resources/application.yml}</li>
 * </ol>
 */
@ApplicationScoped
public class GamelanConfig {

    @ConfigProperty(name = "gamelan.default.model", defaultValue = "llama3")
    String defaultModel;

    @ConfigProperty(name = "gamelan.skills.dir", defaultValue = "")
    String skillsDir;

    @ConfigProperty(name = "gamelan.history.size", defaultValue = "50")
    int historySize;

    @ConfigProperty(name = "gamelan.token.budget", defaultValue = "6000")
    int tokenBudget;

    @ConfigProperty(name = "gamelan.stream.default", defaultValue = "true")
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

    @ConfigProperty(name = "gamelan.request.timeout.seconds", defaultValue = "120")
    int requestTimeoutSeconds;

    @ConfigProperty(name = "gamelan.session.persist", defaultValue = "false")
    boolean sessionPersist;

    @ConfigProperty(name = "gamelan.tool.command.allowed-prefix", defaultValue = "")
    String allowedCommandPrefix;

    @ConfigProperty(name = "gamelan.tool.command.timeout.seconds", defaultValue = "60")
    int commandTimeoutSeconds;

    @ConfigProperty(name = "gamelan.tool.read.max-bytes", defaultValue = "200000")
    int readMaxBytes;

    @ConfigProperty(name = "gamelan.color", defaultValue = "true")
    boolean color;

    // ── Accessors ──────────────────────────────────────────────────────────

    public String defaultModel()          { return defaultModel; }
    public String skillsDir() {
        return (skillsDir != null && !skillsDir.isBlank())
                ? skillsDir
                : System.getProperty("user.home") + "/.gamelan/skills";
    }
    public int     historySize()          { return historySize; }
    public int     tokenBudget()          { return tokenBudget; }
    public boolean streamByDefault()      { return streamByDefault; }
    public double  temperature()          { return temperature; }
    public int     maxTokens()            { return maxTokens; }
    public String  engineMode()           { return engineMode; }
    public String  remoteUrl()            { return remoteUrl; }
    public String  apiKey()               { return apiKey; }
    public int     requestTimeoutSeconds(){ return requestTimeoutSeconds; }
    public boolean sessionPersist()       { return sessionPersist; }
    public String  allowedCommandPrefix() { return allowedCommandPrefix; }
    public int     commandTimeoutSeconds(){ return commandTimeoutSeconds; }
    public int     readMaxBytes()         { return readMaxBytes; }
    public boolean color()                { return color && System.getenv("NO_COLOR") == null; }
}
