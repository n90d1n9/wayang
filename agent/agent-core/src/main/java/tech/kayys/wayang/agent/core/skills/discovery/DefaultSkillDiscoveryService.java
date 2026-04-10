package tech.kayys.wayang.agent.core.skills.discovery;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifestParser;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * Default implementation of skill discovery service.
 *
 * <p>
 * Scans multiple directories following AgentSkills.io and cross-client conventions.
 * Thread-safe for concurrent discovery operations.
 */
public class DefaultSkillDiscoveryService implements SkillDiscoveryService {

    private static final String[] DEFAULT_USER_PATHS = {
            ".golok/skills",
            ".agents/skills"
    };

    private static final String[] DEFAULT_WORKSPACE_PATHS = {
            ".golok/skills",
            ".agents/skills",
            ".claude/skills",
            ".cursor/skills",
            ".copilot/skills"
    };

    private final List<Path> discoveryPaths;

    /**
     * Create discovery service with default paths.
     */
    public DefaultSkillDiscoveryService() {
        this.discoveryPaths = new CopyOnWriteArrayList<>();
        initializeDefaultPaths();
    }

    /**
     * Create discovery service with custom paths.
     *
     * @param customPaths additional paths to scan
     */
    public DefaultSkillDiscoveryService(List<Path> customPaths) {
        this.discoveryPaths = new CopyOnWriteArrayList<>();
        initializeDefaultPaths();
        if (customPaths != null) {
            discoveryPaths.addAll(customPaths);
        }
    }

    @Override
    public Uni<List<SkillManifest>> discoverAll() {
        return Uni.createFrom().item(() -> {
            List<SkillManifest> allManifests = new ArrayList<>();
            for (Path path : discoveryPaths) {
                try {
                    allManifests.addAll(discoverFromDirectorySync(path));
                } catch (Exception e) {
                    // Log and continue with next path
                    System.err.println("Warning: Failed to discover skills in " + path + ": " + e.getMessage());
                }
            }
            return allManifests;
        });
    }

    @Override
    public Uni<List<SkillManifest>> discoverFromPath(Path path) {
        return Uni.createFrom().item(() -> discoverFromDirectorySync(path));
    }

    @Override
    public Uni<Map<Path, List<SkillManifest>>> discoverWithMetadata() {
        return Uni.createFrom().item(() -> {
            Map<Path, List<SkillManifest>> result = new LinkedHashMap<>();
            for (Path path : discoveryPaths) {
                try {
                    List<SkillManifest> manifests = discoverFromDirectorySync(path);
                    if (!manifests.isEmpty()) {
                        result.put(path, manifests);
                    }
                } catch (Exception e) {
                    System.err.println("Warning: Failed to discover skills in " + path + ": " + e.getMessage());
                }
            }
            return result;
        });
    }

    @Override
    public List<Path> getDiscoveryPaths() {
        return new ArrayList<>(discoveryPaths);
    }

    @Override
    public void addDiscoveryPath(Path path) {
        if (path != null && !discoveryPaths.contains(path)) {
            discoveryPaths.add(path);
        }
    }

    @Override
    public void removeDiscoveryPath(Path path) {
        discoveryPaths.remove(path);
    }

    // ── Internal ──────────────────────────────────────────────────

    private void initializeDefaultPaths() {
        String home = System.getProperty("user.home");
        String cwd = System.getProperty("user.dir");

        // Add user-level paths (under home)
        if (home != null) {
            for (String userPath : DEFAULT_USER_PATHS) {
                Path p = Paths.get(home).resolve(userPath);
                discoveryPaths.add(p);
            }
        }

        // Add workspace-level paths (under cwd)
        if (cwd != null) {
            for (String wsPath : DEFAULT_WORKSPACE_PATHS) {
                Path p = Paths.get(cwd).resolve(wsPath);
                discoveryPaths.add(p);
            }
        }
    }

    private List<SkillManifest> discoverFromDirectorySync(Path baseDir) {
        List<SkillManifest> manifests = new ArrayList<>();

        // Try skills/ subdirectory first (AgentSkills.io convention)
        Path skillsDir = baseDir.resolve("skills");
        Path scanDir = Files.isDirectory(skillsDir) ? skillsDir : baseDir;

        if (!Files.isDirectory(scanDir)) {
            return manifests;
        }

        try (Stream<Path> dirs = Files.list(scanDir)) {
            dirs.filter(Files::isDirectory)
                    .forEach(dir -> {
                        Path skillMd = dir.resolve("SKILL.md");
                        if (Files.isRegularFile(skillMd)) {
                            try {
                                SkillManifest manifest = SkillManifestParser.parse(skillMd);
                                // Validate before adding
                                var validation = SkillManifestParser.validate(manifest);
                                if (validation.isValid()) {
                                    manifests.add(manifest);
                                } else {
                                    System.err.println("Warning: Skill " + manifest.getName() +
                                            " validation failed: " + validation.getErrorMessage());
                                }
                            } catch (Exception e) {
                                System.err.println("Warning: Failed to parse " + skillMd + ": " + e.getMessage());
                            }
                        }
                    });
        } catch (IOException e) {
            // Directory doesn't exist or isn't readable
        }

        return manifests;
    }
}
