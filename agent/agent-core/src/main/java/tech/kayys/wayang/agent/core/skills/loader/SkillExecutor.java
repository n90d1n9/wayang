package tech.kayys.wayang.agent.core.skills.loader;

import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.core.skills.validation.SkillValidator;

import java.io.IOException;
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

    private final SkillManifestCatalog catalog;
    private final SkillRuntime runtime;
    private final SkillValidator validator;
    private final int executionTimeoutSeconds;

    public SkillExecutor(Path skillsDirectory) {
        this(skillsDirectory, 30);
    }

    public SkillExecutor(Path skillsDirectory, int executionTimeoutSeconds) {
        this(defaultCatalog(skillsDirectory),
                new FilesystemSkillRuntime(skillsDirectory, executionTimeoutSeconds),
                new SkillValidator(),
                executionTimeoutSeconds);
    }

    SkillExecutor(SkillManifestCatalog catalog, SkillRuntime runtime, SkillValidator validator,
                  int executionTimeoutSeconds) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.validator = Objects.requireNonNull(validator, "validator");
        this.executionTimeoutSeconds = executionTimeoutSeconds;
    }

    private static SkillManifestCatalog defaultCatalog(Path skillsDirectory) {
        Objects.requireNonNull(skillsDirectory, "skillsDirectory");
        return new SkillManifestCatalog(skillsDirectory, new DefaultSkillsLoaderService(skillsDirectory));
    }

    /**
     * Load all available skills from the skills directory.
     */
    public Map<String, SkillManifest> loadAllSkills() throws IOException {
        return reloadSkills().after().manifests();
    }

    /**
     * Reload skills and return the catalog lifecycle diff.
     */
    public SkillManifestCatalogChange reloadSkills() throws IOException {
        SkillManifestCatalogChange change = catalog.reload();
        LOGGER.infof("Loaded %d skills (added=%d, updated=%d, removed=%d)",
                change.after().manifests().size(),
                change.added().size(),
                change.updated().size(),
                change.removed().size());
        return change;
    }

    /**
     * Execute a skill with the given parameters.
     */
    public SkillExecutionResult executeSkill(String skillName, Map<String, Object> parameters) {
        SkillExecutionResults results = SkillExecutionResults.started(skillName);

        // Validate skill exists
        if (!catalog.contains(skillName)) {
            return results.skillNotFound();
        }

        SkillManifest manifest = catalog.get(skillName).orElseThrow();

        try {
            // Validate parameters against manifest using unified validator
            SkillValidator.ValidationResult validation =
                    validator.validateParameters(skillName, manifest, parameters);

            if (!validation.isValid()) {
                return results.parameterValidationFailure(validation.getErrors());
            }

            // Execute the skill
            SkillProcessRunner.ProcessResult processResult = runtime.execute(skillName, parameters);

            return results.success(processResult.output(), processResult.metadata());

        } catch (IllegalArgumentException e) {
            LOGGER.warnf("Rejected skill input for %s: %s", skillName, e.getMessage());
            return results.invalidInput(e.getMessage());
        } catch (SkillExecutableResolver.SkillLayoutException e) {
            LOGGER.warnf("Skill %s has invalid filesystem layout: %s", skillName, e.getMessage());
            return results.layoutFailure(e.getMessage(), e.metadata());
        } catch (SkillProcessRunner.ProcessFailureException e) {
            LOGGER.warnf("Skill %s failed: %s", skillName, e.getMessage());
            return results.processFailure(e);
        } catch (TimeoutException e) {
            LOGGER.warnf("Skill %s timed out: %s", skillName, e.getMessage());
            return results.timeout(e.getMessage(), executionTimeoutSeconds);
        } catch (Exception e) {
            LOGGER.errorf(e, "Failed to execute skill %s: %s", skillName, e.getMessage());
            return results.executionError(e);
        }
    }

    /**
     * Get skill manifest by name.
     */
    public Optional<SkillManifest> getSkillManifest(String skillName) {
        return catalog.get(skillName);
    }

    /**
     * List all loaded skills.
     */
    public Collection<String> listSkills() {
        return catalog.names();
    }

    /**
     * Result of skill execution.
     */
    public record SkillExecutionResult(
        String skillName,
        String output,
        long executionTimeMs,
        boolean success,
        String error,
        Map<String, Object> metadata
    ) implements SkillExecutionOutcome {
        public SkillExecutionResult(String skillName, String output, long executionTimeMs, boolean success,
                                    String error) {
            this(skillName, output, executionTimeMs, success, error, Map.of());
        }

        public SkillExecutionResult {
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }
}
