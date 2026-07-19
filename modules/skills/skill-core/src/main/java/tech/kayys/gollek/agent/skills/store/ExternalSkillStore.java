package tech.kayys.gollek.agent.skills.skills.store;

import tech.kayys.gollek.agent.skills.skills.manifest.SkillManifest;
import tech.kayys.gollek.agent.skills.skills.manifest.SkillManifestParser;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Persistent store for externally installed skills.
 *
 * <p>
 * Manages the index of installed skill repositories and their manifests.
 * Skills are stored on disk and indexed in-memory for fast lookup.
 *
 * <p>
 * Directory layout:
 * 
 * <pre>
 * ~/.wayang/skills/
 *   ├── cc-skills/           # cloned repo
 *   │   └── skills/
 *   │       ├── conventional-git/SKILL.md
 *   │       └── promql-cli/SKILL.md
 *   ├── cc-skills-golang/    # another cloned repo
 *   │   └── skills/
 *   │       ├── golang-cli/SKILL.md
 *   │       └── golang-code-style/SKILL.md
 *   └── index.json           # installation metadata
 * </pre>
 *
 * @author Bhangun
 */
public class ExternalSkillStore {

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(ExternalSkillStore.class);

    private final Path skillsBaseDir;

    /** In-memory index: skill name → manifest */
    private final ConcurrentHashMap<String, SkillManifest> skillIndex = new ConcurrentHashMap<>();

    /** Repo name → list of skill names installed from that repo */
    private final ConcurrentHashMap<String, List<String>> repoIndex = new ConcurrentHashMap<>();

    public ExternalSkillStore(Path skillsBaseDir) {
        this.skillsBaseDir = skillsBaseDir;
    }

    /**
     * Load all skills from the skills base directory into the in-memory index.
     */
    public void loadAll() {
        skillIndex.clear();
        repoIndex.clear();

        if (!Files.isDirectory(skillsBaseDir)) {
            LOG.debugf("Skills directory does not exist: %s", skillsBaseDir);
            return;
        }

        try (var repos = Files.list(skillsBaseDir)) {
            repos.filter(Files::isDirectory)
                    .filter(d -> !d.getFileName().toString().startsWith("."))
                    .forEach(this::loadRepo);
        } catch (IOException e) {
            LOG.warnf(e, "Failed to scan skills directory: %s", skillsBaseDir);
        }

        LOG.infof("External skill store loaded: %d skills from %d repos",
                skillIndex.size(), repoIndex.size());
    }

    /**
     * Register skills from a specific repo directory.
     */
    public void loadRepo(Path repoDir) {
        String repoName = repoDir.getFileName().toString();
        List<SkillManifest> manifests = SkillManifestParser.scanDirectory(repoDir);
        List<String> skillNames = new ArrayList<>();

        for (SkillManifest manifest : manifests) {
            skillIndex.put(manifest.getName(), manifest);
            skillNames.add(manifest.getName());
        }

        if (!skillNames.isEmpty()) {
            repoIndex.put(repoName, skillNames);
            LOG.infof("Loaded %d skills from repo: %s", skillNames.size(), repoName);
        }
    }

    /**
     * Unregister all skills from a repo.
     */
    public void unloadRepo(String repoName) {
        List<String> skills = repoIndex.remove(repoName);
        if (skills != null) {
            skills.forEach(skillIndex::remove);
            LOG.infof("Unloaded %d skills from repo: %s", skills.size(), repoName);
        }
    }

    // ── Query ─────────────────────────────────────────────────────

    /**
     * Find a skill by name.
     */
    public Optional<SkillManifest> findSkill(String name) {
        return Optional.ofNullable(skillIndex.get(name));
    }

    /**
     * Find skills matching a description query (simple keyword match).
     */
    public List<SkillManifest> searchSkills(String query) {
        if (query == null || query.isBlank())
            return listAll();

        String lowerQuery = query.toLowerCase();
        return skillIndex.values().stream()
                .filter(m -> m.getName().toLowerCase().contains(lowerQuery) ||
                        m.getDescription().toLowerCase().contains(lowerQuery))
                .sorted(Comparator.comparing(SkillManifest::getName))
                .collect(Collectors.toList());
    }

    /**
     * List all installed skills.
     */
    public List<SkillManifest> listAll() {
        return skillIndex.values().stream()
                .sorted(Comparator.comparing(SkillManifest::getName))
                .collect(Collectors.toList());
    }

    /**
     * List all installed repos with their skills.
     */
    public Map<String, List<String>> listRepos() {
        return Collections.unmodifiableMap(repoIndex);
    }

    /**
     * Get skills by repo name.
     */
    public List<SkillManifest> getSkillsByRepo(String repoName) {
        List<String> names = repoIndex.get(repoName);
        if (names == null)
            return List.of();
        return names.stream()
                .map(skillIndex::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Check if a skill is installed.
     */
    public boolean isInstalled(String skillName) {
        return skillIndex.containsKey(skillName);
    }

    /**
     * Total number of installed skills.
     */
    public int size() {
        return skillIndex.size();
    }

    /**
     * Total number of installed repos.
     */
    public int repoCount() {
        return repoIndex.size();
    }

    public Path getSkillsBaseDir() {
        return skillsBaseDir;
    }
}
