package tech.kayys.gollek.agent.skills.agent;

import tech.kayys.gollek.agent.skills.skills.cli.SkillsCommandHandler;
import tech.kayys.gollek.agent.skills.skills.discovery.SkillDiscoveryService;
import tech.kayys.gollek.agent.skills.skills.loader.GitSkillLoader;
import tech.kayys.gollek.agent.skills.skills.loader.LocalSkillLoader;
import tech.kayys.gollek.agent.skills.skills.manifest.SkillManifest;
import tech.kayys.gollek.agent.skills.skills.store.ExternalSkillAdapter;
import tech.kayys.gollek.agent.skills.skills.store.ExternalSkillStore;
import tech.kayys.wayang.tools.CodeToolRegistry;
import tech.kayys.wayang.tools.ToolRouter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Main golok Code Agent — an AI coding agent with extensible skills.
 *
 * <p>
 * Composes the skills management system:
 * <ul>
 * <li>{@link ExternalSkillStore} — in-memory index of installed skills</li>
 * <li>{@link GitSkillLoader} — git-based skill installation</li>
 * <li>{@link SkillDiscoveryService} — cross-client skill directory
 * scanning</li>
 * <li>{@link SkillsCommandHandler} — CLI command handling</li>
 * </ul>
 * 
 * <p>
 * Composes the built-in coding tools:
 * <ul>
 * <li>{@link CodeToolRegistry} — Built-in file, search, and shell tools</li>
 * <li>{@link ToolRouter} — Executes LLM tool calls</li>
 * </ul>
 *
 * <p>
 * Integrates with the existing agent-core SPI:
 * <ul>
 * <li>External SKILL.md-based skills participate alongside internal Java
 * skills</li>
 * <li>Skills are triggered by description matching against user context</li>
 * <li>Active skills inject their SKILL.md body into the LLM system prompt</li>
 * <li>Progressive disclosure loads references on demand</li>
 * </ul>
 *
 * <h3>Usage:</h3>
 * 
 * <pre>{@code
 * // Create agent with defaults
 * AgentSkillUtil agent = AgentSkillUtil.create();
 *
 * // Install skills
 * agent.installSkills("https://github.com/samber/cc-skills", null);
 *
 * // Build context for a request
 * String skillContext = agent.buildSkillContext("How to structure Go CLI apps?");
 * }</pre>
 *
 * @author Bhangun
 */
public class AgentSkillUtil {

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(AgentSkillUtil.class);

    private final AgentSkillConfig config;
    private final ExternalSkillStore store;
    private final GitSkillLoader gitLoader;
    private final LocalSkillLoader localLoader;
    private final SkillDiscoveryService discoveryService;
    private final SkillsCommandHandler commandHandler;

    private final CodeToolRegistry toolRegistry;
    private final ToolRouter toolRouter;

    private boolean initialized = false;

    private AgentSkillUtil(AgentSkillConfig config) {
        this.config = config;
        this.store = new ExternalSkillStore(config.getSkillsDir());
        this.gitLoader = new GitSkillLoader(config.getSkillsDir());
        this.localLoader = new LocalSkillLoader(config.getSkillsDir());
        this.discoveryService = new SkillDiscoveryService(store, config.getAdditionalSkillPaths());
        this.commandHandler = new SkillsCommandHandler(gitLoader, localLoader, store);
        this.toolRegistry = CodeToolRegistry.withDefaults();
        this.toolRouter = new ToolRouter(this.toolRegistry);
    }

    /**
     * Create an agent with default configuration.
     */
    public static AgentSkillUtil create() {
        return create(AgentSkillConfig.defaults());
    }

    /**
     * Create an agent from environment configuration.
     */
    public static AgentSkillUtil createFromEnvironment() {
        return create(AgentSkillConfig.fromEnvironment());
    }

    /**
     * Create an agent with explicit configuration.
     */
    public static AgentSkillUtil create(AgentSkillConfig config) {
        AgentSkillUtil agent = new AgentSkillUtil(config);
        if (config.isAutoDiscover()) {
            agent.initialize();
        }
        return agent;
    }

    /**
     * Initialize the agent — discover and load all skills.
     */
    public void initialize() {
        if (initialized)
            return;

        LOG.info("Initializing golok Code Agent...");
        LOG.infof("Skills directory: %s", config.getSkillsDir());

        if (config.isEnableExternalSkills()) {
            int total = discoveryService.discoverAll();
            LOG.infof("golok Code Agent initialized: %d external skills loaded", total);
        }

        initialized = true;
    }

    // ── Skill Installation ────────────────────────────────────────

    /**
     * Install skills from a git repository.
     *
     * @param repoUrl     git repo URL
     * @param skillFilter optional glob filter (null = all)
     */
    public void installSkills(String repoUrl, String skillFilter) {
        commandHandler.add(repoUrl, skillFilter);
    }

    /**
     * Remove an installed skill repo.
     */
    public void removeSkills(String repoName) {
        commandHandler.remove(repoName);
    }

    /**
     * Update installed skill repos.
     */
    public void updateSkills(String repoName) {
        commandHandler.update(repoName);
    }

    /**
     * List all installed skills.
     */
    public void listSkills() {
        commandHandler.list();
    }

    /**
     * Show info about a specific skill.
     */
    public void skillInfo(String skillName) {
        commandHandler.info(skillName);
    }

    // ── Skill Context Building ────────────────────────────────────

    /**
     * Build skill context for a user query by finding and loading relevant skills.
     *
     * <p>
     * This is the core function that injects external skill knowledge into
     * the LLM prompt. It:
     * <ol>
     * <li>Scores all installed skills against the query</li>
     * <li>Selects top-N skills within the token budget</li>
     * <li>Builds context from each skill's SKILL.md body</li>
     * </ol>
     *
     * @param query user's request or conversation context
     * @return formatted skill context to inject into system prompt
     */
    public String buildSkillContext(String query) {
        if (!config.isEnableExternalSkills() || store.size() == 0) {
            return "";
        }

        // Score all skills against the query
        List<ScoredSkill> scored = store.listAll().stream()
                .map(m -> new ScoredSkill(m, new ExternalSkillAdapter(m).matchScore(query)))
                .filter(s -> s.score > 0.1) // threshold
                .sorted(Comparator.comparingDouble(ScoredSkill::score).reversed())
                .limit(config.getMaxLoadedSkills())
                .collect(Collectors.toList());

        if (scored.isEmpty())
            return "";

        // Build context within token budget
        StringBuilder ctx = new StringBuilder();
        int tokenBudget = config.getMaxSkillContextTokens();
        int tokensUsed = 0;

        ctx.append("## Active Skills\n\n");

        for (ScoredSkill ss : scored) {
            ExternalSkillAdapter adapter = new ExternalSkillAdapter(ss.manifest);
            int estTokens = adapter.estimatedTokens();

            if (tokensUsed + estTokens > tokenBudget) {
                LOG.debugf("Skipping skill %s (would exceed token budget: %d + %d > %d)",
                        ss.manifest.getName(), tokensUsed, estTokens, tokenBudget);
                continue;
            }

            ctx.append(adapter.buildSkillContext()).append("\n---\n\n");
            tokensUsed += estTokens;
        }

        LOG.debugf("Built skill context: %d skills, ~%d tokens", scored.size(), tokensUsed);
        return ctx.toString();
    }

    /**
     * Get context for a specific skill by name.
     *
     * @param skillName name of the skill
     * @return skill context or empty string if not found
     */
    public String getSkillContext(String skillName) {
        return store.findSkill(skillName)
                .map(m -> new ExternalSkillAdapter(m).buildSkillContext())
                .orElse("");
    }

    /**
     * Get extended context for a specific skill including references.
     */
    public String getExtendedSkillContext(String skillName, List<String> references) {
        return store.findSkill(skillName)
                .map(m -> new ExternalSkillAdapter(m).buildExtendedContext(references))
                .orElse("");
    }

    // ── Introspection ─────────────────────────────────────────────

    /**
     * Get the skills command handler for CLI integration.
     */
    public SkillsCommandHandler getCommandHandler() {
        return commandHandler;
    }

    /**
     * Get the external skill store.
     */
    public ExternalSkillStore getStore() {
        return store;
    }

    /**
     * Get the skill discovery service.
     */
    public SkillDiscoveryService getDiscoveryService() {
        return discoveryService;
    }

    /**
     * Get the git skill loader.
     */
    public GitSkillLoader getGitLoader() {
        return gitLoader;
    }

    /**
     * Get the local skill loader.
     */
    public LocalSkillLoader getLocalLoader() {
        return localLoader;
    }

    /**
     * Get the configuration.
     */
    public AgentSkillConfig getConfig() {
        return config;
    }

    /**
     * Get the code tool registry (built-in coding tools).
     */
    public CodeToolRegistry getToolRegistry() {
        return toolRegistry;
    }

    /**
     * Get the tool router for executing tools.
     */
    public ToolRouter getToolRouter() {
        return toolRouter;
    }

    /**
     * Check if the agent is initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    // ── Internal ──────────────────────────────────────────────────

    private record ScoredSkill(SkillManifest manifest, double score) {
    }

    @Override
    public String toString() {
        return String.format("AgentSkillUtil{skills=%d, repos=%d, initialized=%s}",
                store.size(), store.repoCount(), initialized);
    }
}
