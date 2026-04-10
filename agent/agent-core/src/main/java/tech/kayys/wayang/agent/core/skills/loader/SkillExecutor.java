package tech.kayys.wayang.agent.core.skills.loader;

import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.core.skills.validation.SkillValidator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Executes skills and handles their lifecycle.
 * 
 * <p>Skills can be shell scripts, Python programs, or other executables.
 * This executor:
 * <ul>
 *   <li>Loads skill manifests from SKILL.md files</li>
 *   <li>Validates skill parameters against manifest specifications</li>
 *   <li>Executes skill scripts with specified parameters</li>
 *   <li>Captures output and handles timeouts</li>
 *   <li>Reports execution results with timing information</li>
 * </ul>
 *
 * @author Bhangun
 */
public class SkillExecutor {

    private static final Logger LOGGER = Logger.getLogger(SkillExecutor.class);

    private final Path skillsDirectory;
    private final SkillsLoaderService loaderService;
    private final SkillValidator validator;
    private final Map<String, SkillManifest> loadedSkills;
    private final int executionTimeoutSeconds;

    public SkillExecutor(Path skillsDirectory) {
        this(skillsDirectory, 30);
    }

    public SkillExecutor(Path skillsDirectory, int executionTimeoutSeconds) {
        this.skillsDirectory = Objects.requireNonNull(skillsDirectory, "skillsDirectory");
        this.executionTimeoutSeconds = executionTimeoutSeconds;
        this.loaderService = new DefaultSkillsLoaderService(skillsDirectory);
        this.validator = new SkillValidator();
        this.loadedSkills = new HashMap<>();
    }

    /**
     * Load all available skills from the skills directory.
     */
    public Map<String, SkillManifest> loadAllSkills() throws IOException {
        loadedSkills.clear();
        List<SkillManifest> manifests = loaderService.loadSkillsFromDirectory(skillsDirectory);
        for (SkillManifest manifest : manifests) {
            loadedSkills.put(manifest.getName(), manifest);
        }
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

        SkillManifest manifest = loadedSkills.get(skillName);

        try {
            // Validate parameters against manifest using unified validator
            SkillValidator.ValidationResult validation = 
                    validator.validateParameters(skillName, manifest, parameters);
            
            if (!validation.isValid()) {
                return new SkillExecutionResult(
                        skillName,
                        null,
                        System.currentTimeMillis() - startTime,
                        false,
                        "Parameter validation failed: " + String.join("; ", validation.getErrors())
                );
            }

            // Execute the skill
            String output = executeSkillProcess(skillName, manifest, parameters);

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
    private String executeSkillProcess(String skillName, SkillManifest manifest, 
                                      Map<String, Object> parameters) 
            throws IOException, InterruptedException, TimeoutException {
        
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
     * Get skill manifest by name.
     */
    public Optional<SkillManifest> getSkillManifest(String skillName) {
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
