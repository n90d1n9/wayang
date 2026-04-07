package tech.kayys.golok.code.agent;

import java.nio.file.Path;
import java.util.List;

/**
 * Configuration for the Agent Skill.
 *
 * <p>
 * Extends the base agent configuration with code-agent-specific settings:
 * skill discovery paths, external skill management, and context budget.
 *
 * <p>
 * Configuration values can come from:
 * <ul>
 * <li>{@code application.yaml} under the {@code golok.code} prefix</li>
 * <li>Environment variables ({@code GOLLEK_AGENT_SKILLS_DIR})</li>
 * <li>System properties</li>
 * </ul>
 *
 * @author Bhangun
 */
public class AgentSkillConfig {

    // ── Defaults ──────────────────────────────────────────────────

    private static final Path DEFAULT_SKILLS_DIR = Path.of(System.getProperty("user.home"), ".golok", "skills");
    private static final int DEFAULT_MAX_SKILL_CONTEXT_TOKENS = 10_000;
    private static final int DEFAULT_MAX_LOADED_SKILLS = 4;
    private static final int DEFAULT_GIT_TIMEOUT_SECONDS = 60;

    // ── Fields ────────────────────────────────────────────────────

    private Path skillsDir;
    private List<Path> additionalSkillPaths;
    private int maxSkillContextTokens;
    private int maxLoadedSkills;
    private int gitTimeoutSeconds;
    private boolean autoDiscover;
    private boolean enableExternalSkills;
    private String defaultStrategy;

    private AgentSkillConfig(Builder b) {
        this.skillsDir = b.skillsDir != null ? b.skillsDir : DEFAULT_SKILLS_DIR;
        this.additionalSkillPaths = b.additionalSkillPaths != null ? b.additionalSkillPaths : List.of();
        this.maxSkillContextTokens = b.maxSkillContextTokens > 0 ? b.maxSkillContextTokens
                : DEFAULT_MAX_SKILL_CONTEXT_TOKENS;
        this.maxLoadedSkills = b.maxLoadedSkills > 0 ? b.maxLoadedSkills : DEFAULT_MAX_LOADED_SKILLS;
        this.gitTimeoutSeconds = b.gitTimeoutSeconds > 0 ? b.gitTimeoutSeconds : DEFAULT_GIT_TIMEOUT_SECONDS;
        this.autoDiscover = b.autoDiscover;
        this.enableExternalSkills = b.enableExternalSkills;
        this.defaultStrategy = b.defaultStrategy != null ? b.defaultStrategy : "react";
    }

    // ── Accessors ─────────────────────────────────────────────────

    /** Primary directory for installed skills (default: ~/.golok/skills/) */
    public Path getSkillsDir() {
        return skillsDir;
    }

    /** Additional directories to scan for skills */
    public List<Path> getAdditionalSkillPaths() {
        return additionalSkillPaths;
    }

    /** Maximum total tokens of loaded skill context (default: 10,000) */
    public int getMaxSkillContextTokens() {
        return maxSkillContextTokens;
    }

    /** Maximum skills loaded simultaneously (default: 4) */
    public int getMaxLoadedSkills() {
        return maxLoadedSkills;
    }

    /** Git clone/pull timeout in seconds (default: 60) */
    public int getGitTimeoutSeconds() {
        return gitTimeoutSeconds;
    }

    /** Whether to auto-discover skills at startup (default: true) */
    public boolean isAutoDiscover() {
        return autoDiscover;
    }

    /** Whether external SKILL.md skills are enabled (default: true) */
    public boolean isEnableExternalSkills() {
        return enableExternalSkills;
    }

    /** Default orchestration strategy (default: "react") */
    public String getDefaultStrategy() {
        return defaultStrategy;
    }

    // ── Builder ───────────────────────────────────────────────────

    public static Builder builder() {
        return new Builder();
    }

    /** Create a config with all defaults. */
    public static AgentSkillConfig defaults() {
        return builder()
                .autoDiscover(true)
                .enableExternalSkills(true)
                .build();
    }

    /**
     * Create a config from environment variables and system properties.
     */
    public static AgentSkillConfig fromEnvironment() {
        Builder b = builder()
                .autoDiscover(true)
                .enableExternalSkills(true);

        // Override from environment
        String skillsDirEnv = System.getenv("GOLLEK_AGENT_SKILLS_DIR");
        if (skillsDirEnv != null && !skillsDirEnv.isBlank()) {
            b.skillsDir(Path.of(skillsDirEnv));
        }

        String maxTokensEnv = System.getenv("GOLLEK_AGENT_MAX_SKILL_TOKENS");
        if (maxTokensEnv != null) {
            try {
                b.maxSkillContextTokens(Integer.parseInt(maxTokensEnv));
            } catch (NumberFormatException ignored) {
            }
        }

        String strategyEnv = System.getenv("GOLLEK_AGENT_DEFAULT_STRATEGY");
        if (strategyEnv != null && !strategyEnv.isBlank()) {
            b.defaultStrategy(strategyEnv);
        }

        return b.build();
    }

    public static final class Builder {
        private Path skillsDir;
        private List<Path> additionalSkillPaths;
        private int maxSkillContextTokens;
        private int maxLoadedSkills;
        private int gitTimeoutSeconds;
        private boolean autoDiscover = true;
        private boolean enableExternalSkills = true;
        private String defaultStrategy;

        public Builder skillsDir(Path v) {
            skillsDir = v;
            return this;
        }

        public Builder additionalSkillPaths(List<Path> v) {
            additionalSkillPaths = v;
            return this;
        }

        public Builder maxSkillContextTokens(int v) {
            maxSkillContextTokens = v;
            return this;
        }

        public Builder maxLoadedSkills(int v) {
            maxLoadedSkills = v;
            return this;
        }

        public Builder gitTimeoutSeconds(int v) {
            gitTimeoutSeconds = v;
            return this;
        }

        public Builder autoDiscover(boolean v) {
            autoDiscover = v;
            return this;
        }

        public Builder enableExternalSkills(boolean v) {
            enableExternalSkills = v;
            return this;
        }

        public Builder defaultStrategy(String v) {
            defaultStrategy = v;
            return this;
        }

        public AgentSkillConfig build() {
            return new AgentSkillConfig(this);
        }
    }

    @Override
    public String toString() {
        return String.format("GolokCodeConfig{skillsDir=%s, maxTokens=%d, maxLoaded=%d, autoDiscover=%s}",
                skillsDir, maxSkillContextTokens, maxLoadedSkills, autoDiscover);
    }
}
