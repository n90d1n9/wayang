package tech.kayys.wayang.agent.core.skills.loader;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifestParser;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Default implementation of SkillsLoaderService.
 *
 * <p>Consolidates functionality from LocalSkillLoader and GitSkillLoader:
 * <ul>
 *   <li>Local directory installation with copying</li>
 *   <li>Git repository cloning and pulling</li>
 *   <li>Update management (source tracking)</li>
 *   <li>Skill discovery and parsing</li>
 * </ul>
 *
 * <p>All skills are stored under a base directory with convention-based organization:
 * <pre>
 * ~/.agent/skills/
 *   ├── repo-name-1/      (git or local)
 *   │   ├── .git/         (git repos only)
 *   │   ├── .local_source (local repos only - stores original path)
 *   │   ├── SKILL.md
 *   │   └── ...
 *   └── repo-name-2/
 *       └── ...
 * </pre>
 *
 * @author Bhangun
 */
public class DefaultSkillsLoaderService implements SkillsLoaderService {

    private static final org.jboss.logging.Logger LOG =
            org.jboss.logging.Logger.getLogger(DefaultSkillsLoaderService.class);

    private final Path skillsBaseDir;

    /**
     * Create a loader with the default skills directory (~/.agent/skills/).
     */
    public DefaultSkillsLoaderService() {
        this(Path.of(System.getProperty("user.home"), ".agent", "skills"));
    }

    /**
     * Create a loader with a custom skills directory.
     *
     * @param skillsBaseDir base directory for storing skill repositories
     */
    public DefaultSkillsLoaderService(Path skillsBaseDir) {
        this.skillsBaseDir = Objects.requireNonNull(skillsBaseDir, "skillsBaseDir");
    }

    @Override
    public SkillLoaderResult installLocal(String localPath, String skillFilter) {
        Objects.requireNonNull(localPath, "localPath");
        Path sourceDir = Path.of(localPath).normalize().toAbsolutePath();
        List<String> errors = new ArrayList<>();

        if (!Files.isDirectory(sourceDir)) {
            errors.add("Not a valid directory: " + localPath);
            return new SkillLoaderResult(localPath, null, List.of(), errors, false);
        }

        String repoName = sourceDir.getFileName().toString();
        Path destDir = skillsBaseDir.resolve(repoName);
        boolean updated = Files.exists(destDir);

        try {
            Files.createDirectories(skillsBaseDir);

            if (updated) {
                LOG.infof("Updating existing local skill repository: %s", repoName);
                deleteRecursive(destDir);
            } else {
                LOG.infof("Installing skills from local directory: %s -> %s", sourceDir, destDir);
            }

            // Copy directory recursively, excluding .git
            copyDirectoryRecursive(sourceDir, destDir);

            // Store source path for future updates
            Files.writeString(destDir.resolve(".local_source"), sourceDir.toString());

            // Scan and parse skills
            List<SkillManifest> manifests = SkillManifestParser.scanDirectory(destDir);
            List<SkillManifest> filtered = filterSkills(manifests, skillFilter);

            List<String> installed = new ArrayList<>();
            for (SkillManifest manifest : filtered) {
                installed.add(manifest.getName());
                LOG.infof("  Found skill: %s v%s — %s",
                        manifest.getName(), manifest.getVersion(),
                        truncate(manifest.getDescription(), 60));
            }

            if (installed.isEmpty()) {
                errors.add("No skills found in local directory" +
                        (skillFilter != null ? " matching filter: " + skillFilter : ""));
            }

            return new SkillLoaderResult(sourceDir.toString(), destDir.toString(), installed, errors, updated);

        } catch (IOException e) {
            errors.add("IO error: " + e.getMessage());
            LOG.errorf(e, "Failed to install skills from local directory: %s", sourceDir);
            return new SkillLoaderResult(sourceDir.toString(), destDir.toString(), List.of(), errors, false);
        }
    }

    @Override
    public SkillLoaderResult installGit(String repoUrl, String skillFilter) {
        Objects.requireNonNull(repoUrl, "repoUrl");
        String repoName = extractRepoName(repoUrl);
        Path repoDir = skillsBaseDir.resolve(repoName);
        List<String> errors = new ArrayList<>();
        boolean updated = false;

        try {
            Files.createDirectories(skillsBaseDir);

            if (Files.isDirectory(repoDir.resolve(".git"))) {
                // Already cloned — do a git pull
                LOG.infof("Updating existing git repository: %s", repoName);
                try (Git git = Git.open(repoDir.toFile())) {
                    git.pull().call();
                }
                updated = true;
            } else {
                // Fresh clone with shallow depth
                LOG.infof("Cloning git repository: %s -> %s", repoUrl, repoDir);
                Git.cloneRepository()
                        .setURI(repoUrl)
                        .setDirectory(repoDir.toFile())
                        .setDepth(1)
                        .call()
                        .close();
            }

            // Scan and parse skills
            List<SkillManifest> manifests = SkillManifestParser.scanDirectory(repoDir);
            List<SkillManifest> filtered = filterSkills(manifests, skillFilter);

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

            return new SkillLoaderResult(repoUrl, repoDir.toString(), installed, errors, updated);

        } catch (GitAPIException e) {
            errors.add("Git error: " + e.getMessage());
            LOG.errorf(e, "Failed to clone/pull git repository: %s", repoUrl);
            return new SkillLoaderResult(repoUrl, repoDir.toString(), List.of(), errors, false);

        } catch (IOException e) {
            errors.add("IO error: " + e.getMessage());
            LOG.errorf(e, "Failed to access repository directory: %s", repoDir);
            return new SkillLoaderResult(repoUrl, repoDir.toString(), List.of(), errors, false);
        }
    }

    @Override
    public SkillLoaderResult update(String repoName) {
        Path repoDir = skillsBaseDir.resolve(repoName);
        List<String> errors = new ArrayList<>();

        // Check if it's a git repo or local repo
        Path gitDir = repoDir.resolve(".git");
        Path localSourceFile = repoDir.resolve(".local_source");

        if (Files.isDirectory(gitDir)) {
            // Git repository — pull
            return updateGitRepository(repoDir, repoName, errors);
        } else if (Files.isRegularFile(localSourceFile)) {
            // Local repository — re-copy from source
            return updateLocalRepository(repoDir, localSourceFile, errors);
        } else {
            errors.add("Not a managed repository (no .git or .local_source): " + repoName);
            return new SkillLoaderResult(null, repoDir.toString(), List.of(), errors, false);
        }
    }

    @Override
    public boolean remove(String repoName) {
        Path repoDir = skillsBaseDir.resolve(repoName);
        if (!Files.isDirectory(repoDir)) {
            return false;
        }
        try {
            deleteRecursive(repoDir);
            LOG.infof("Removed skill repository: %s", repoName);
            return true;
        } catch (IOException e) {
            LOG.errorf(e, "Failed to remove skill repository: %s", repoName);
            return false;
        }
    }

    @Override
    public List<String> listInstalledRepos() {
        if (!Files.isDirectory(skillsBaseDir)) {
            return List.of();
        }
        try (var dirs = Files.list(skillsBaseDir)) {
            return dirs.filter(Files::isDirectory)
                    .filter(d -> isSkillRepository(d))
                    .map(d -> d.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOG.debugf(e, "Failed to list installed repositories");
            return List.of();
        }
    }

    @Override
    public List<SkillManifest> loadSkillsFromDirectory(Path directory) {
        return SkillManifestParser.scanDirectory(directory);
    }

    @Override
    public Path getSkillsBaseDir() {
        return skillsBaseDir;
    }

    // ── Internal ──────────────────────────────────────────────────

    private SkillLoaderResult updateGitRepository(Path repoDir, String repoName, List<String> errors) {
        try (Git git = Git.open(repoDir.toFile())) {
            git.pull().call();

            List<SkillManifest> manifests = SkillManifestParser.scanDirectory(repoDir);
            List<String> names = manifests.stream()
                    .map(SkillManifest::getName)
                    .collect(Collectors.toList());

            String remoteUrl = git.getRepository().getConfig()
                    .getString("remote", "origin", "url");

            return new SkillLoaderResult(remoteUrl, repoDir.toString(), names, errors, true);

        } catch (GitAPIException | IOException e) {
            errors.add("Update failed: " + e.getMessage());
            return new SkillLoaderResult(null, repoDir.toString(), List.of(), errors, false);
        }
    }

    private SkillLoaderResult updateLocalRepository(Path repoDir, Path localSourceFile, List<String> errors) {
        try {
            String sourcePathStr = Files.readString(localSourceFile).trim();
            Path sourceDir = Path.of(sourcePathStr);

            if (!Files.isDirectory(sourceDir)) {
                errors.add("Original source directory no longer exists: " + sourcePathStr);
                return new SkillLoaderResult(sourcePathStr, repoDir.toString(), List.of(), errors, false);
            }

            // Re-copy files from source
            String sourceMetadata = Files.readString(localSourceFile);
            deleteRecursive(repoDir);
            copyDirectoryRecursive(sourceDir, repoDir);
            Files.writeString(repoDir.resolve(".local_source"), sourceMetadata);

            List<SkillManifest> manifests = SkillManifestParser.scanDirectory(repoDir);
            List<String> names = manifests.stream()
                    .map(SkillManifest::getName)
                    .collect(Collectors.toList());

            return new SkillLoaderResult(sourcePathStr, repoDir.toString(), names, errors, true);

        } catch (IOException e) {
            errors.add("Update failed: " + e.getMessage());
            return new SkillLoaderResult(null, repoDir.toString(), List.of(), errors, false);
        }
    }

    private boolean isSkillRepository(Path dir) {
        // Check if it's a git repo
        if (Files.isDirectory(dir.resolve(".git"))) {
            return true;
        }
        // Check if it's a local repo (has .local_source)
        if (Files.isRegularFile(dir.resolve(".local_source"))) {
            return true;
        }
        // Check if it has any SKILL.md files
        try (var walk = Files.walk(dir, 3)) {
            return walk.anyMatch(p -> p.getFileName().toString().equals("SKILL.md"));
        } catch (IOException e) {
            return false;
        }
    }

    private List<SkillManifest> filterSkills(List<SkillManifest> manifests, String filter) {
        if (filter == null || filter.isBlank() || "*".equals(filter)) {
            return manifests;
        }

        String regex = filter.replace("*", ".*").replace("?", ".");
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

        return manifests.stream()
                .filter(m -> pattern.matcher(m.getName()).matches())
                .collect(Collectors.toList());
    }

    private void copyDirectoryRecursive(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Skip .git directories
                if (dir.getFileName().toString().equals(".git")) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)),
                        StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String extractRepoName(String url) {
        String clean = url.replaceAll("\\.git$", "").replaceAll("/$", "");
        int lastSlash = clean.lastIndexOf('/');
        return lastSlash >= 0 ? clean.substring(lastSlash + 1) : clean;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "";
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
}
