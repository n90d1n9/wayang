package tech.kayys.wayang.agent.core.skills.loader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves filesystem skill layout into the executable process entrypoint.
 */
final class SkillExecutableResolver {

    private static final String MANIFEST_FILE = "SKILL.md";
    private static final List<String> ENTRYPOINT_CANDIDATES = List.of(
            "run.sh",
            "main.py",
            "%s",
            "index.js",
            "main.go");

    private final Path skillsDirectory;

    SkillExecutableResolver(Path skillsDirectory) {
        this.skillsDirectory = Objects.requireNonNull(skillsDirectory, "skillsDirectory");
    }

    ResolvedExecutable resolve(String skillName) throws IOException {
        Path skillPath = resolveSkillPath(skillName);
        Path manifestPath = skillPath.resolve(MANIFEST_FILE);

        if (!Files.isRegularFile(manifestPath)) {
            throw new SkillLayoutException(
                    "SKILL.md not found for skill: " + skillName,
                    "missing_manifest");
        }

        Path executable = findExecutable(skillPath, skillName)
                .orElseThrow(() -> new SkillLayoutException(
                        "No executable found for skill: " + skillName,
                        "missing_executable"));

        return new ResolvedExecutable(skillPath, manifestPath, executable);
    }

    private Path resolveSkillPath(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            throw new IllegalArgumentException("Skill name must not be blank");
        }

        Path relative = Path.of(skillName);
        if (relative.isAbsolute() || relative.getNameCount() != 1) {
            throw new IllegalArgumentException("Skill name must identify a direct skill directory: " + skillName);
        }

        Path root = skillsDirectory.toAbsolutePath().normalize();
        Path skillPath = skillsDirectory.resolve(relative).normalize();
        Path absoluteSkillPath = skillPath.toAbsolutePath().normalize();
        if (!absoluteSkillPath.startsWith(root)) {
            throw new IllegalArgumentException("Skill name escapes the skills directory: " + skillName);
        }
        return skillPath;
    }

    private Optional<Path> findExecutable(Path skillPath, String skillName) {
        for (String candidateName : ENTRYPOINT_CANDIDATES) {
            Path candidate = skillPath.resolve(candidateName.formatted(skillName));
            if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                return Optional.of(candidate);
            }
        }
        return Optional.empty();
    }

    record ResolvedExecutable(Path skillPath, Path manifestPath, Path executable) {
        ResolvedExecutable {
            Objects.requireNonNull(skillPath, "skillPath");
            Objects.requireNonNull(manifestPath, "manifestPath");
            Objects.requireNonNull(executable, "executable");
        }
    }

    static final class SkillLayoutException extends IOException {
        private final String layoutError;

        SkillLayoutException(String message, String layoutError) {
            super(message);
            this.layoutError = Objects.requireNonNull(layoutError, "layoutError");
        }

        Map<String, Object> metadata() {
            return SkillExecutionOutcomes.failureMetadata(
                    SkillFailureType.SKILL_LAYOUT,
                    SkillExecutionMetadata.KEY_LAYOUT_ERROR,
                    layoutError);
        }
    }
}
