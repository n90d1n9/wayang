package tech.kayys.wayang.agent.core.skills.discovery;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Service for discovering and loading skills from multiple sources.
 *
 * <p>
 * Scans configurable discovery paths following AgentSkills.io and cross-client conventions:
 * <ul>
 * <li>{@code ~/.golok/skills/} — primary user-level skills directory</li>
 * <li>{@code .golok/skills/} — workspace-level skills</li>
 * <li>{@code .agents/skills/} — cross-client convention</li>
 * <li>{@code .claude/skills/} — Claude Code convention</li>
 * <li>Custom paths from configuration</li>
 * </ul>
 *
 * <p>
 * Mirrors how other AI coding agents discover skills:
 * <pre>
 * Claude Code  → .claude/skills/
 * Cursor       → .cursor/skills/
 * Copilot      → .copilot/skills/
 * OpenCode     → .agents/skills/, .opencode/skills/
 * Codex        → ~/.agents/skills/
 * Antigravity  → ~/.antigravity/skills/
 * </pre>
 */
public interface SkillDiscoveryService {

    /**
     * Discover all skills from configured paths.
     *
     * @return list of discovered skill manifests
     */
    Uni<List<SkillManifest>> discoverAll();

    /**
     * Discover skills from a specific directory.
     *
     * @param path the directory to scan
     * @return list of discovered manifests in that directory
     */
    Uni<List<SkillManifest>> discoverFromPath(Path path);

    /**
     * Discover skills from all configured paths with metadata.
     *
     * @return map of path → discovered manifests
     */
    Uni<Map<Path, List<SkillManifest>>> discoverWithMetadata();

    /**
     * Get all configured discovery paths (user + workspace + custom).
     *
     * @return list of discovery paths
     */
    List<Path> getDiscoveryPaths();

    /**
     * Add a custom discovery path.
     *
     * @param path the path to add
     */
    void addDiscoveryPath(Path path);

    /**
     * Remove a discovery path.
     *
     * @param path the path to remove
     */
    void removeDiscoveryPath(Path path);
}
