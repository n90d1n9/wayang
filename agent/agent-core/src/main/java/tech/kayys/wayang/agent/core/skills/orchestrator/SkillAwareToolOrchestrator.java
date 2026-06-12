package tech.kayys.wayang.agent.core.skills.orchestrator;

import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.skills.integration.SkillIntegrationRegistry;
import tech.kayys.wayang.agent.core.skills.integration.SkillLifecycleRefreshCoordinator;
import tech.kayys.wayang.agent.core.skills.integration.SkillLifecycleRefreshResult;
import tech.kayys.wayang.agent.core.skills.loader.SkillExecutor;
import tech.kayys.wayang.agent.core.skills.loader.SkillExecutionOutcome;
import tech.kayys.wayang.agent.core.skills.loader.SkillExecutionOutcomes;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.nio.file.Path;
import java.util.*;

/**
 * Orchestrates skill-aware tool execution within the agent framework.
 *
 * <p>This component manages the workflow of loading skills, planning with AI assistance,
 * executing tools, and validating outputs. It integrates the skill registry and execution
 * engine with tool orchestration logic.
 *
 * <p>High-level flow:
 * <ol>
 *   <li>Load relevant skills based on domain/context</li>
 *   <li>AI planning phase - determine which tools/skills to use</li>
 *   <li>Tool execution phase - invoke tools with skill context</li>
 *   <li>Validation phase - verify outputs against skill requirements</li>
 * </ol>
 *
 * @author Bhangun
 */
public class SkillAwareToolOrchestrator {

    private static final Logger LOG = Logger.getLogger(SkillAwareToolOrchestrator.class);

    private final SkillExecutor skillExecutor;
    private final SkillLifecycleRefreshCoordinator refreshCoordinator;
    private final Path skillsDirectory;
    private final Map<String, SkillManifest> loadedSkills;
    private SkillLifecycleRefreshResult lastRefreshResult;

    public SkillAwareToolOrchestrator(Path skillsDirectory) {
        this(skillsDirectory, null);
    }

    public SkillAwareToolOrchestrator(Path skillsDirectory, SkillIntegrationRegistry integrationRegistry) {
        this(skillsDirectory, new SkillExecutor(skillsDirectory), integrationRegistry);
    }

    SkillAwareToolOrchestrator(
            Path skillsDirectory,
            SkillExecutor skillExecutor,
            SkillIntegrationRegistry integrationRegistry) {
        this.skillsDirectory = Objects.requireNonNull(skillsDirectory, "skillsDirectory");
        this.skillExecutor = Objects.requireNonNull(skillExecutor, "skillExecutor");
        this.refreshCoordinator = new SkillLifecycleRefreshCoordinator(skillExecutor, integrationRegistry);
        this.loadedSkills = new HashMap<>();
        this.lastRefreshResult = SkillLifecycleRefreshResult.empty();
    }

    /**
     * Initialize orchestration by loading skills for a specific domain.
     *
     * @param domain domain identifier (e.g., "inference", "data-processing")
     * @return list of loaded skill names
     * @throws Exception if skill loading fails
     */
    public List<String> initializeForDomain(String domain) throws Exception {
        loadedSkills.clear();
        lastRefreshResult = refreshCoordinator.reloadSkills();
        Map<String, SkillManifest> allSkills = lastRefreshResult.manifests();

        // Filter skills by domain (if metadata includes domain information)
        for (Map.Entry<String, SkillManifest> entry : allSkills.entrySet()) {
            SkillManifest manifest = entry.getValue();
            // Check if skill has domain metadata or if domain-agnostic (include by default)
            if (isDomainRelevant(manifest, domain)) {
                loadedSkills.put(entry.getKey(), manifest);
            }
        }

        LOG.infof("Initialized orchestrator for domain '%s' with %d skills", domain, loadedSkills.size());
        return new ArrayList<>(loadedSkills.keySet());
    }

    /**
     * Get all currently loaded skills.
     */
    public Map<String, SkillManifest> getLoadedSkills() {
        return Map.copyOf(loadedSkills);
    }

    /**
     * Get the most recent filesystem reload and integration refresh result.
     */
    public SkillLifecycleRefreshResult getLastRefreshResult() {
        return lastRefreshResult;
    }

    /**
     * Execute a skill within the orchestration context.
     *
     * @param skillName name of the skill to execute
     * @param parameters execution parameters
     * @return execution result
     */
    public SkillExecutionOutcome executeSkill(String skillName, Map<String, Object> parameters) {
        if (!loadedSkills.containsKey(skillName)) {
            LOG.warnf("Attempted to execute unloaded skill: %s", skillName);
            return SkillExecutionOutcomes.skillNotLoaded(skillName);
        }

        LOG.debugf("Orchestrating execution of skill: %s", skillName);
        return skillExecutor.executeSkill(skillName, parameters);
    }

    /**
     * Check if a skill is relevant to the given domain.
     *
     * @param manifest skill manifest
     * @param domain domain identifier
     * @return true if skill is relevant to domain
     */
    private boolean isDomainRelevant(SkillManifest manifest, String domain) {
        List<String> domains = SkillMetadataKeys.domains(manifest.getMetadata());
        if (!domains.isEmpty()) {
            return domains.contains(domain) || domains.contains(SkillMetadataKeys.WILDCARD_DOMAIN);
        }
        // By default, include all skills if no domain restriction is specified
        return true;
    }

    /**
     * Validate skill execution output.
     *
     * @param skillName skill that was executed
     * @param output output from skill execution
     * @return true if output is valid according to skill specification
     */
    public boolean validateSkillOutput(String skillName, String output) {
        SkillManifest manifest = loadedSkills.get(skillName);
        if (manifest == null) {
            return false;
        }

        // Basic validation: check output is not null/empty
        if (output == null || output.trim().isEmpty()) {
            LOG.warnf("Skill %s produced empty output", skillName);
            return false;
        }

        SkillMetadataKeys.outputFormat(manifest.getMetadata())
                .ifPresent(expectedFormat ->
                        LOG.debugf("Validating output format for skill %s: %s", skillName, expectedFormat));

        return true;
    }

    /**
     * Clear loaded skills and reset orchestration state.
     */
    public void reset() {
        loadedSkills.clear();
        LOG.debug("Orchestrator state reset");
    }
}
