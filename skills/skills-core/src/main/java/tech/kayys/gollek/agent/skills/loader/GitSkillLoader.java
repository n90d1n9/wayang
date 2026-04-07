package tech.kayys.golok.code.skills.loader;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import tech.kayys.golok.code.skills.manifest.SkillManifest;
import tech.kayys.golok.code.skills.manifest.SkillManifestParser;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Loads skills from remote git repositories.
 *
 * <p>
 * Handles the {@code golok skills add <url>} workflow:
 * <ol>
 * <li>Clone the repository into the skills directory</li>
 * <li>Scan for SKILL.md files</li>
 * <li>Parse and validate each skill manifest</li>
 * <li>Register skills in the store</li>
 * </ol>
 *
 * <p>
 * Follows the same conventions as other agents:
 * 
 * <pre>
 * # Claude Code
 * /plugin install cc-skills@samber
 *
 * # Gemini CLI
 * gemini extensions install https://github.com/samber/cc-skills
 *
 * # golok
 * golok skills add https://github.com/samber/cc-skills
 * </pre>
 *
 * @author Bhangun
 */
public class GitSkillLoader {

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(GitSkillLoader.class);

    private final Path skillsBaseDir;

    /**
     * Create a loader with the default skills directory {@code ~/.golok/skills/}.
     */
    public GitSkillLoader() {
        this(Path.of(System.getProperty("user.home"), ".golok", "skills"));
    }

    /**
     * Create a loader with a custom skills directory.
     *
     * @param skillsBaseDir base directory for storing cloned skill repos
     */
    public GitSkillLoader(Path skillsBaseDir) {
        this.skillsBaseDir = skillsBaseDir;
    }

    /**
     * Install skills from a git repository URL.
     *
     * @param repoUrl     git repository URL (HTTPS or SSH)
     * @param skillFilter optional glob filter for specific skills (null or "*" =
     *                    all)
     * @return installation result
     */
    public SkillInstallResult install(String repoUrl, String skillFilter) {
        Objects.requireNonNull(repoUrl, "repoUrl");
        String repoName = extractRepoName(repoUrl);
        Path repoDir = skillsBaseDir.resolve(repoName);
        List<String> errors = new ArrayList<>();
        boolean updated = false;

        try {
            // Ensure base directory exists
            Files.createDirectories(skillsBaseDir);

            if (Files.isDirectory(repoDir.resolve(".git"))) {
                // Already cloned — do a git pull
                LOG.infof("Updating existing skill repo: %s", repoName);
                try (Git git = Git.open(repoDir.toFile())) {
                    git.pull().call();
                }
                updated = true;
            } else {
                // Fresh clone
                LOG.infof("Cloning skill repo: %s -> %s", repoUrl, repoDir);
                Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(repoDir.toFile())
                        .setDepth(1) // shallow clone
                        .call()
                        .close();
            }

            // Scan and parse skills
            List<SkillManifest> manifests = SkillManifestParser.scanDirectory(repoDir);

            // Apply skill filter
            List<SkillManifest> filtered = filterSkills(manifests, skillFilter);

            // Set source repo on each manifest
            List<String> installed = new ArrayList<>();
            for (SkillManifest manifest : filtered) {
                installed.add(manifest.getName());
                LOG.infof("  Found skill: %s v%s — %s",
                        manifest.getName(), manifest.getVersion(),
                        truncate(manifest.getDescription(), 60));
            }

            if (installed.isEmpty()) {
                errors.add("No skills found in repository" +
                        (skillFilter != null ? " matching filter: " + skillFilter : ""));
            }

            return new SkillInstallResult(repoUrl, repoDir.toString(), installed, errors, updated);

        } catch (GitAPIException e) {
            errors.add("Git error: " + e.getMessage());
            LOG.errorf(e, "Failed to clone/pull repository: %s", repoUrl);
            return new SkillInstallResult(repoUrl, repoDir.toString(), List.of(), errors, false);

        } catch (IOException e) {
            errors.add("IO error: " + e.getMessage());
            LOG.errorf(e, "Failed to access skill directory: %s", repoDir);
            return new SkillInstallResult(repoUrl, repoDir.toString(), List.of(), errors, false);
        }
    }

    /**
     * Update a previously installed skill repo.
     *
     * @param repoName name of the repo directory under skills base
     * @return update result
     */
    public SkillInstallResult update(String repoName) {
        Path repoDir = skillsBaseDir.resolve(repoName);
        List<String> errors = new ArrayList<>();

        if (!Files.isDirectory(repoDir.resolve(".git"))) {
            errors.add("Not a git repository: " + repoName);
            return new SkillInstallResult(null, repoDir.toString(), List.of(), errors, false);
        }

        try (Git git = Git.open(repoDir.toFile())) {
            git.pull().call();

            List<SkillManifest> manifests = SkillManifestParser.scanDirectory(repoDir);
            List<String> names = manifests.stream()
                    .map(SkillManifest::getName)
                    .collect(Collectors.toList());

            String remoteUrl = git.getRepository().getConfig()
                    .getString("remote", "origin", "url");

            return new SkillInstallResult(remoteUrl, repoDir.toString(), names, errors, true);

        } catch (GitAPIException | IOException e) {
            errors.add("Update failed: " + e.getMessage());
            return new SkillInstallResult(null, repoDir.toString(), List.of(), errors, false);
        }
    }

    /**
     * Remove an installed skill repo.
     *
     * @param repoName name of the repo directory under skills base
     * @return true if successfully removed
     */
    public boolean remove(String repoName) {
        Path repoDir = skillsBaseDir.resolve(repoName);
        if (!Files.isDirectory(repoDir)) {
            return false;
        }
        try {
            deleteRecursive(repoDir);
            LOG.infof("Removed skill repo: %s", repoName);
            return true;
        } catch (IOException e) {
            LOG.errorf(e, "Failed to remove skill repo: %s", repoName);
            return false;
        }
    }

    /**
     * List all installed skill repo directories.
     */
    public List<String> listInstalledRepos() {
        if (!Files.isDirectory(skillsBaseDir))
            return List.of();
        try (var dirs = Files.list(skillsBaseDir)) {
            return dirs.filter(Files::isDirectory)
                    .filter(d -> Files.isDirectory(d.resolve(".git")) ||
                            Files.isRegularFile(d.resolve("skills").resolve("SKILL.md")) ||
                            hasSkillMdRecursive(d))
                    .map(d -> d.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    /**
     * Get the base directory where skills are stored.
     */
    public Path getSkillsBaseDir() {
        return skillsBaseDir;
    }

    // ── Internal ──────────────────────────────────────────────────

    private List<SkillManifest> filterSkills(List<SkillManifest> manifests, String filter) {
        if (filter == null || filter.isBlank() || "*".equals(filter)) {
            return manifests;
        }

        // Convert glob to regex pattern
        String regex = filter.replace("*", ".*").replace("?", ".");
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

        return manifests.stream()
                .filter(m -> pattern.matcher(m.getName()).matches())
                .collect(Collectors.toList());
    }

    static String extractRepoName(String url) {
        String clean = url.replaceAll("\\.git$", "").replaceAll("/$", "");
        int lastSlash = clean.lastIndexOf('/');
        return lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null)
            return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    private static void deleteRecursive(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var entries = Files.list(path)) {
                for (Path entry : entries.toList()) {
                    deleteRecursive(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    private static boolean hasSkillMdRecursive(Path dir) {
        try (var walk = Files.walk(dir, 3)) {
            return walk.anyMatch(p -> p.getFileName().toString().equals("SKILL.md"));
        } catch (IOException e) {
            return false;
        }
    }
}
