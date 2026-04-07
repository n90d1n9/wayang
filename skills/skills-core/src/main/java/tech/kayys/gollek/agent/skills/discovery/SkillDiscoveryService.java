package tech.kayys.golok.code.skills.discovery;

import tech.kayys.golok.code.skills.manifest.SkillManifest;
import tech.kayys.golok.code.skills.manifest.SkillManifestParser;
import tech.kayys.golok.code.skills.store.ExternalSkillStore;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/**
 * Discovers skills from multiple directories following cross-client
 * conventions.
 *
 * <p>
 * Scans configurable discovery paths for SKILL.md files:
 * <ul>
 * <li>{@code ~/.golok/skills/} — primary user-level skills directory</li>
 * <li>{@code .golok/skills/} — workspace-level skills</li>
 * <li>{@code .agents/skills/} — cross-client convention (Codex, OpenCode)</li>
 * <li>{@code .claude/skills/} — Claude Code convention</li>
 * <li>Custom paths from configuration</li>
 * </ul>
 *
 * <p>
 * This mirrors how other AI coding agents discover skills:
 * 
 * <pre>
 * Claude Code  → .claude/skills/
 * Cursor       → .cursor/skills/
 * Copilot      → .copilot/skills/
 * OpenCode     → .agents/skills/, .opencode/skills/
 * Codex        → ~/.agents/skills/
 * Antigravity  → ~/.antigravity/skills/
 * golok       → ~/.golok/skills/, .golok/skills/, .agents/skills/
 * </pre>
 *
 * @author Bhangun
 */
public class SkillDiscoveryService {

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(SkillDiscoveryService.class);

    /** Default user-level discovery paths (resolved from $HOME). */
    private static final String[] USER_LEVEL_PATHS = {
            ".golok/skills",
            ".agents/skills"
    };

    /** Default workspace-level discovery paths (resolved from CWD). */
    private static final String[] WORKSPACE_LEVEL_PATHS = {
            ".golok/skills",
            ".agents/skills",
            ".claude/skills"
    };

    private final ExternalSkillStore store;
    private final List<Path> additionalPaths;

    /**
     * Create a discovery service with default paths.
     */
    public SkillDiscoveryService(ExternalSkillStore store) {
        this(store, List.of());
    }

    /**
     * Create a discovery service with additional custom paths.
     *
     * @param store           the external skill store to populate
     * @param additionalPaths extra directories to scan for skills
     */
    public SkillDiscoveryService(ExternalSkillStore store, List<Path> additionalPaths) {
        this.store = store;
        this.additionalPaths = additionalPaths != null ? additionalPaths : List.of();
    }

    /**
     * Discover and load all skills from all configured paths.
     *
     * @return total number of skills discovered
     */
    public int discoverAll() {
        int totalBefore = store.size();

        // 1. Load from primary store directory (installed via git)
        store.loadAll();

        // 2. Scan user-level directories
        Path home = Path.of(System.getProperty("user.home"));
        for (String rel : USER_LEVEL_PATHS) {
            Path dir = home.resolve(rel);
            scanAndLoad(dir, "user");
        }

        // 3. Scan workspace-level directories
        Path cwd = Path.of(System.getProperty("user.dir"));
        for (String rel : WORKSPACE_LEVEL_PATHS) {
            Path dir = cwd.resolve(rel);
            // Avoid scanning the same directory twice
            if (!dir.startsWith(home.resolve(".golok"))) {
                scanAndLoad(dir, "workspace");
            }
        }

        // 4. Scan additional paths
        for (Path path : additionalPaths) {
            scanAndLoad(path, "custom");
        }

        int totalDiscovered = store.size() - totalBefore;
        LOG.infof("Skill discovery complete: %d new skills found, %d total",
                totalDiscovered, store.size());

        return store.size();
    }

    /**
     * Discover skills from a specific directory.
     *
     * @param dir directory to scan
     * @return skills found
     */
    public List<SkillManifest> discoverFrom(Path dir) {
        if (!Files.isDirectory(dir))
            return List.of();

        List<SkillManifest> manifests = SkillManifestParser.scanDirectory(dir);
        LOG.debugf("Discovered %d skills from: %s", manifests.size(), dir);
        return manifests;
    }

    /**
     * Get all configured discovery paths.
     */
    public List<Path> getDiscoveryPaths() {
        List<Path> paths = new ArrayList<>();
        Path home = Path.of(System.getProperty("user.home"));
        Path cwd = Path.of(System.getProperty("user.dir"));

        for (String rel : USER_LEVEL_PATHS) {
            paths.add(home.resolve(rel));
        }
        for (String rel : WORKSPACE_LEVEL_PATHS) {
            paths.add(cwd.resolve(rel));
        }
        paths.addAll(additionalPaths);

        return paths;
    }

    /**
     * Get the resolved primary skills directory.
     */
    public Path getPrimarySkillsDir() {
        return store.getSkillsBaseDir();
    }

    // ── Internal ──────────────────────────────────────────────────

    private void scanAndLoad(Path dir, String source) {
        if (!Files.isDirectory(dir)) {
            LOG.debugf("Skipping non-existent %s skill path: %s", source, dir);
            return;
        }

        try (var entries = Files.list(dir)) {
            entries.filter(Files::isDirectory)
                    .forEach(repoDir -> {
                        store.loadRepo(repoDir);
                    });
        } catch (IOException e) {
            LOG.warnf("Failed to scan %s skill path: %s — %s", source, dir, e.getMessage());
        }
    }
}
