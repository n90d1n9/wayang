package tech.kayys.golok.code.skills.loader;

import tech.kayys.golok.code.skills.manifest.SkillManifest;
import tech.kayys.golok.code.skills.manifest.SkillManifestParser;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Loads skills from a local directory.
 *
 * <p>
 * Copies the contents of a local directory into the skills base directory
 * so that it can be discovered and used by the agent.
 *
 * @author Bhangun
 */
public class LocalSkillLoader {

    private static final org.jboss.logging.Logger LOG = org.jboss.logging.Logger.getLogger(LocalSkillLoader.class);

    private final Path skillsBaseDir;

    public LocalSkillLoader(Path skillsBaseDir) {
        this.skillsBaseDir = skillsBaseDir;
    }

    /**
     * Install skills from a local directory by copying it.
     */
    public SkillInstallResult install(String localPathStr, String skillFilter) {
        Objects.requireNonNull(localPathStr, "localPathStr");
        Path sourceDir = Path.of(localPathStr).normalize().toAbsolutePath();
        List<String> errors = new ArrayList<>();

        if (!Files.isDirectory(sourceDir)) {
            errors.add("Not a valid directory: " + localPathStr);
            return new SkillInstallResult(localPathStr, null, List.of(), errors, false);
        }

        String repoName = sourceDir.getFileName().toString();
        Path destDir = skillsBaseDir.resolve(repoName);
        boolean updated = false;

        try {
            Files.createDirectories(skillsBaseDir);

            if (Files.exists(destDir)) {
                LOG.infof("Updating existing local skill repo: %s", repoName);
                updated = true;
                // Delete existing contents before copying
                deleteRecursive(destDir);
            } else {
                LOG.infof("Copying local skill repo: %s -> %s", sourceDir, destDir);
            }

            copyDirectory(sourceDir, destDir);

            // Write a metadata file to remember the source path for updates
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

            return new SkillInstallResult(sourceDir.toString(), destDir.toString(), installed, errors, updated);

        } catch (IOException e) {
            errors.add("IO error: " + e.getMessage());
            LOG.errorf(e, "Failed to copy local skill directory: %s", sourceDir);
            return new SkillInstallResult(sourceDir.toString(), destDir.toString(), List.of(), errors, false);
        }
    }

    /**
     * Update a previously installed local skill repo.
     */
    public SkillInstallResult update(String repoName) {
        Path repoDir = skillsBaseDir.resolve(repoName);
        List<String> errors = new ArrayList<>();

        Path sourceFile = repoDir.resolve(".local_source");
        if (!Files.isRegularFile(sourceFile)) {
            errors.add("Not a locally installed repository: " + repoName);
            return new SkillInstallResult(null, repoDir.toString(), List.of(), errors, false);
        }

        try {
            String sourcePathStr = Files.readString(sourceFile).trim();
            Path sourceDir = Path.of(sourcePathStr);

            if (!Files.isDirectory(sourceDir)) {
                errors.add("Original source directory no longer exists at: " + sourcePathStr);
                return new SkillInstallResult(sourcePathStr, repoDir.toString(), List.of(), errors, false);
            }

            // To update, we just re-copy the files from source to destination
            // We need to keep the .local_source file, though
            String sourceMetadata = Files.readString(sourceFile);
            deleteRecursive(repoDir);
            copyDirectory(sourceDir, repoDir);
            Files.writeString(repoDir.resolve(".local_source"), sourceMetadata);

            List<SkillManifest> manifests = SkillManifestParser.scanDirectory(repoDir);
            List<String> names = manifests.stream()
                    .map(SkillManifest::getName)
                    .collect(Collectors.toList());

            return new SkillInstallResult(sourcePathStr, repoDir.toString(), names, errors, true);

        } catch (IOException e) {
            errors.add("Update failed: " + e.getMessage());
            return new SkillInstallResult(null, repoDir.toString(), List.of(), errors, false);
        }
    }

    // ── Internal ──────────────────────────────────────────────────

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

    private void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.getFileName().toString().equals(".git")) {
                    return FileVisitResult.SKIP_SUBTREE; // ignore .git
                }
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
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
}
