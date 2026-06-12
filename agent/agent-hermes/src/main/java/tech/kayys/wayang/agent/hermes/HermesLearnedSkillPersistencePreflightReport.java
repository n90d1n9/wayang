package tech.kayys.wayang.agent.hermes;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Read-only readiness report for Hermes learned-skill persistence.
 */
public record HermesLearnedSkillPersistencePreflightReport(
        boolean ready,
        boolean targetPlanReady,
        boolean fileSystemOnly,
        boolean dedicatedServiceSupported,
        boolean dedicatedServiceAvailable,
        boolean usesProvidedSkillManagement,
        boolean dataSourceRequired,
        boolean dataSourceAvailable,
        boolean objectStorageRequired,
        boolean objectStorageAvailable,
        String adapterResolution,
        List<String> missingResources,
        List<HermesSkillPersistenceValidationIssue> validationIssues,
        List<String> attention,
        HermesSkillPersistenceTargetPlan targetPlan,
        HermesLearnedSkillPersistenceAdapterResolverOptions options) {

    public HermesLearnedSkillPersistencePreflightReport {
        adapterResolution = HermesText.trimOr(adapterResolution, "provided-skill-management");
        missingResources = HermesCollections.copyNonNull(missingResources);
        validationIssues = HermesCollections.copyNonNull(validationIssues);
        attention = HermesCollections.copyNonNull(attention);
        targetPlan = targetPlan == null ? HermesSkillPersistencePlan.from(null).targetPlan() : targetPlan;
        options = options == null ? HermesLearnedSkillPersistenceAdapterResolverOptions.defaults() : options;
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ready", ready);
        values.put("targetPlanReady", targetPlanReady);
        values.put("adapterResolution", adapterResolution);
        values.put("targetSummary", targetPlan.targetSummary());
        values.put("definitionBackendId", backendId(targetPlan.definitions()));
        values.put("definitionStorageFamily", storageFamily(targetPlan.definitions()));
        values.put("artifactBackendId", backendId(targetPlan.artifacts()));
        values.put("artifactStorageFamily", storageFamily(targetPlan.artifacts()));
        values.put("fileSystemOnly", fileSystemOnly);
        values.put("dedicatedServiceSupported", dedicatedServiceSupported);
        values.put("dedicatedServiceAvailable", dedicatedServiceAvailable);
        values.put("usesProvidedSkillManagement", usesProvidedSkillManagement);
        values.put("dataSourceRequired", dataSourceRequired);
        values.put("dataSourceAvailable", dataSourceAvailable);
        values.put("objectStorageRequired", objectStorageRequired);
        values.put("objectStorageAvailable", objectStorageAvailable);
        values.put("missingResources", missingResources);
        values.put("validationIssueCount", validationIssues.size());
        values.put("validationIssues", validationIssues.stream()
                .map(HermesSkillPersistenceValidationIssue::toMetadata)
                .toList());
        values.put("attention", attention);
        values.put("fileSystemRoot", normalizedPath(options.fileSystemRoot()));
        values.put("fileSystemDefinitionDirectory", normalizedPath(options.fileSystemDefinitionDirectory()));
        values.put("fileSystemArtifactDirectory", normalizedPath(options.fileSystemArtifactDirectory()));
        values.put("objectStorageRootPrefix", options.objectStorageRootPrefix());
        values.put("objectStorageDefinitionPrefix", options.objectStorageDefinitionPrefix());
        values.put("objectStorageArtifactPrefix", options.objectStorageArtifactPrefix());
        values.put("jdbcDefinitionTableName", options.jdbcDefinitionTableName());
        values.put("jdbcArtifactTableName", options.jdbcArtifactTableName());
        values.put("jdbcInitializeSchema", options.jdbcInitializeSchema());
        values.put("targetPlan", targetPlan.toMetadata());
        return Map.copyOf(values);
    }

    private static String backendId(HermesSkillPersistenceTarget target) {
        return target == null ? "" : target.selectedBackendId().orElse("");
    }

    private static String storageFamily(HermesSkillPersistenceTarget target) {
        return target == null
                ? ""
                : target.selectedProfile()
                        .map(HermesSkillPersistenceBackendProfile::storageFamily)
                        .orElse("");
    }

    private static String normalizedPath(Path path) {
        return path == null ? "" : path.toAbsolutePath().normalize().toString();
    }
}
