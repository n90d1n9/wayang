package tech.kayys.wayang.agent.core.skills.loader;

import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.skills.loader.SkillsLoader;
import tech.kayys.wayang.agent.core.skills.SkillsLoader.SkillMetadata;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Executes skills and handles their lifecycle.
 * Skills can be shell scripts, Python programs, or other executables.
 */
public class SkillExecutor {

    private static final Logger LOGGER = Logger.getLogger(SkillExecutor.class);

    private final Path skillsDirectory;
    private final SkillsLoader skillsLoader;
    private final Map<String, SkillMetadata> loadedSkills;
    private final int executionTimeoutSeconds;

    public SkillExecutor(Path skillsDirectory) {
        this(skillsDirectory, 30);
    }

    public SkillExecutor(Path skillsDirectory, int executionTimeoutSeconds) {
        this.skillsDirectory = Objects.requireNonNull(skillsDirectory);
        this.executionTimeoutSeconds = executionTimeoutSeconds;
        this.skillsLoader = new SkillsLoader(skillsDirectory);
        this.loadedSkills = new HashMap<>();
    }

    /**
     * Load all available skills from the skills directory.
     */
    public Map<String, SkillMetadata> loadAllSkills() throws IOException {
        loadedSkills.clear();
        loadedSkills.putAll(skillsLoader.loadAllSkills());
        LOGGER.infof("Loaded %d skills", loadedSkills.size());
        return Map.copyOf(loadedSkills);
    }

    /**
     * Execute a skill with the given parameters.
     */
    public SkillExecutionResult executeSkill(String skillName, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();

        // Validate skill exists
        if (!loadedSkills.containsKey(skillName)) {
            return new SkillExecutionResult(
                    skillName,
                    null,
                    System.currentTimeMillis() - startTime,
                    false,
                    "Skill not found: " + skillName
            );
        }

        SkillMetadata metadata = loadedSkills.get(skillName);

        try {
            // Validate parameters
            SkillParameterValidator.ValidationResult validation = 
                    SkillParameterValidator.validateParameters(skillName, metadata, parameters);
            
            if (!validation.isValid()) {
                return new SkillExecutionResult(
                        skillName,
                        null,
                        System.currentTimeMillis() - startTime,
                        false,
                        "Parameter validation failed: " + validation.getErrors()
                );
            }

            // Execute the skill
            String output = executeSkillProcess(skillName, metadata, parameters);

            return new SkillExecutionResult(
                    skillName,
                    output,
                    System.currentTimeMillis() - startTime,
                    true,
                    null
            );

        } catch (Exception e) {
            LOGGER.errorf(e, "Failed to execute skill %s: %s", skillName, e.getMessage());
            return new SkillExecutionResult(
                    skillName,
                    null,
                    System.currentTimeMillis() - startTime,
                    false,
                    e.getMessage()
            );
        }
    }

    /**
     * Execute the skill process/script.
     */
    private String executeSkillProcess(String skillName, SkillMetadata metadata, 
                                      Map<String, Object> parameters) throws IOException, InterruptedException, java.util.concurrent.TimeoutException {
        
        Path skillPath = skillsDirectory.resolve(skillName);
        Path skillMd = skillPath.resolve("SKILL.md");
        
        if (!Files.exists(skillMd)) {
            throw new FileNotFoundException("SKILL.md not found for skill: " + skillName);
        }

        // Try to find an executable (script or compiled program)
        // Convention: look for run.sh, main.py, or executable with same name as skill
        Path executable = findExecutable(skillPath, skillName);
        
        if (executable == null) {
            throw new FileNotFoundException("No executable found for skill: " + skillName);
        }

        // Build command
        List<String> command = buildCommand(executable, parameters);

        // Execute with timeout
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(skillPath.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // Capture output
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        // Wait for completion with timeout
        boolean completed = process.waitFor(executionTimeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);

        if (!completed) {
            process.destroy();
            throw new TimeoutException("Skill execution timeout after " + executionTimeoutSeconds + " seconds");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Skill execution failed with exit code " + exitCode);
        }

        return output.toString().trim();
    }

    /**
     * Find executable for a skill.
     */
    private Path findExecutable(Path skillPath, String skillName) throws IOException {
        // Priority order: run.sh, main.py, executable named after skill
        Path[] candidates = {
            skillPath.resolve("run.sh"),
            skillPath.resolve("main.py"),
            skillPath.resolve(skillName),
            skillPath.resolve("index.js"),
            skillPath.resolve("main.go")
        };

        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isExecutable(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Build command with parameters.
     */
    private List<String> buildCommand(Path executable, Map<String, Object> parameters) {
        List<String> command = new ArrayList<>();

        String execName = executable.getFileName().toString();
        
        // Determine how to execute based on file type
        if (execName.endsWith(".sh")) {
            command.add("bash");
            command.add(executable.toString());
        } else if (execName.endsWith(".py")) {
            command.add("python3");
            command.add(executable.toString());
        } else if (execName.endsWith(".js")) {
            command.add("node");
            command.add(executable.toString());
        } else if (execName.endsWith(".go")) {
            command.add("go");
            command.add("run");
            command.add(executable.toString());
        } else {
            // Assume it's a compiled executable
            command.add(executable.toString());
        }

        // Add parameters as environment variables or command arguments
        parameters.forEach((key, value) -> {
            command.add("--" + key);
            command.add(String.valueOf(value));
        });

        return command;
    }

    /**
     * Get skill metadata by name.
     */
    public Optional<SkillMetadata> getSkillMetadata(String skillName) {
        return Optional.ofNullable(loadedSkills.get(skillName));
    }

    /**
     * List all loaded skills.
     */
    public Collection<String> listSkills() {
        return loadedSkills.keySet();
    }

    /**
     * Result of skill execution.
     */
    public record SkillExecutionResult(
        String skillName,
        String output,
        long executionTimeMs,
        boolean success,
        String error
    ) {}
}
