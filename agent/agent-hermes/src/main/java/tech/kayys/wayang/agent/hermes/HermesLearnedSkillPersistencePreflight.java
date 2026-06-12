package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Preflights learned-skill persistence targets without creating live stores.
 */
public final class HermesLearnedSkillPersistencePreflight {

    private HermesLearnedSkillPersistencePreflight() {
    }

    public static HermesLearnedSkillPersistencePreflightReport inspect(HermesAgentModeConfig config) {
        return inspect(config, Optional.empty(), Optional.empty());
    }

    public static HermesLearnedSkillPersistencePreflightReport inspect(
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        HermesAgentModeConfig effectiveConfig = config == null ? HermesAgentModeConfig.defaults() : config;
        return inspect(
                effectiveConfig.skillPersistenceStrategy().routePlan().targetPlan(),
                HermesLearnedSkillPersistenceAdapterResolverOptions.fromHints(effectiveConfig.persistenceHints()),
                objectStorageService,
                dataSource);
    }

    public static HermesLearnedSkillPersistencePreflightReport inspect(
            HermesSkillPersistenceTargetPlan targetPlan,
            HermesLearnedSkillPersistenceAdapterResolverOptions options,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        HermesSkillPersistenceTargetPlan effectivePlan = targetPlan == null
                ? HermesSkillPersistencePlan.from(null).targetPlan()
                : targetPlan;
        HermesLearnedSkillPersistenceAdapterResolverOptions effectiveOptions =
                options == null ? HermesLearnedSkillPersistenceAdapterResolverOptions.defaults() : options;
        HermesPersistenceResources resources = HermesPersistenceResources.of(objectStorageService, dataSource);
        boolean targetPlanReady = effectivePlan.ready();
        boolean fileSystemOnly = HermesLearnedSkillPersistenceAdapterResolver.fileSystemOnly(effectivePlan);
        boolean dedicatedServiceSupported =
                HermesLearnedSkillStoreConfigs.canUseDedicatedSkillManagementService(effectivePlan);
        boolean dataSourceRequired = HermesLearnedSkillStoreConfigs.requiresDataSource(effectivePlan);
        boolean objectStorageRequired = HermesLearnedSkillStoreConfigs.requiresObjectStorage(effectivePlan);
        boolean dataSourceAvailable = resources.dataSource().isPresent();
        boolean objectStorageAvailable = resources.objectStorageService().isPresent();
        List<String> missingResources = missingResources(
                dataSourceRequired,
                dataSourceAvailable,
                objectStorageRequired,
                objectStorageAvailable);
        List<HermesSkillPersistenceValidationIssue> validationIssues =
                HermesSkillPersistencePreflightValidator.validate(
                        effectivePlan,
                        dataSourceRequired,
                        dataSourceAvailable,
                        objectStorageRequired,
                        objectStorageAvailable);
        boolean dedicatedServiceAvailable =
                dedicatedServiceSupported
                        && targetPlanReady
                        && missingResources.isEmpty()
                        && validationIssues.isEmpty();
        String adapterResolution = adapterResolution(
                fileSystemOnly,
                dedicatedServiceAvailable);
        boolean usesProvidedSkillManagement = "provided-skill-management".equals(adapterResolution);
        List<String> attention = attention(
                targetPlanReady,
                dedicatedServiceSupported,
                usesProvidedSkillManagement,
                missingResources,
                validationIssues);
        return new HermesLearnedSkillPersistencePreflightReport(
                targetPlanReady && missingResources.isEmpty() && validationIssues.isEmpty(),
                targetPlanReady,
                fileSystemOnly,
                dedicatedServiceSupported,
                dedicatedServiceAvailable,
                usesProvidedSkillManagement,
                dataSourceRequired,
                dataSourceAvailable,
                objectStorageRequired,
                objectStorageAvailable,
                adapterResolution,
                missingResources,
                validationIssues,
                attention,
                effectivePlan,
                effectiveOptions);
    }

    private static List<String> missingResources(
            boolean dataSourceRequired,
            boolean dataSourceAvailable,
            boolean objectStorageRequired,
            boolean objectStorageAvailable) {
        List<String> resources = new ArrayList<>();
        if (dataSourceRequired && !dataSourceAvailable) {
            resources.add("DataSource");
        }
        if (objectStorageRequired && !objectStorageAvailable) {
            resources.add("ObjectStorageService");
        }
        return List.copyOf(resources);
    }

    private static String adapterResolution(
            boolean fileSystemOnly,
            boolean dedicatedServiceAvailable) {
        if (fileSystemOnly) {
            return "file-system";
        }
        if (dedicatedServiceAvailable) {
            return "dedicated-skill-management";
        }
        return "provided-skill-management";
    }

    private static List<String> attention(
            boolean targetPlanReady,
            boolean dedicatedServiceSupported,
            boolean usesProvidedSkillManagement,
            List<String> missingResources,
            List<HermesSkillPersistenceValidationIssue> validationIssues) {
        List<String> messages = new ArrayList<>();
        if (!targetPlanReady) {
            messages.add("Learned-skill persistence target plan has unresolved backends");
        }
        if (!missingResources.isEmpty()) {
            messages.add("Missing learned-skill persistence resources: " + String.join(", ", missingResources));
        }
        if (validationIssues != null && !validationIssues.isEmpty()) {
            validationIssues.stream()
                    .map(HermesSkillPersistenceValidationIssue::reason)
                    .distinct()
                    .forEach(messages::add);
        }
        if (!dedicatedServiceSupported && usesProvidedSkillManagement) {
            messages.add("Learned-skill persistence will use the provided SkillManagementService");
        } else if (!missingResources.isEmpty() && usesProvidedSkillManagement) {
            messages.add("Configured learned-skill persistence target is unavailable; resolver will use the provided SkillManagementService");
        }
        return List.copyOf(messages);
    }
}
