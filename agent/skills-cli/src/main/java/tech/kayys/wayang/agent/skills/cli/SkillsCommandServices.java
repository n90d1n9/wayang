package tech.kayys.wayang.agent.skills.cli;

import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceConfig;
import tech.kayys.wayang.agent.skills.management.SkillManagementServiceFactory;

import java.util.Objects;

/**
 * Composes the services used by the skills CLI command handler.
 */
record SkillsCommandServices(
        SkillsDefinitionCommandService definitionCommandService,
        SkillsDefinitionQueryService definitionQueryService,
        SkillsDefinitionInfoCommandService definitionInfoCommandService,
        SkillsLifecycleCommandService lifecycleCommandService,
        SkillsPersistenceStatusService statusService,
        SkillsPersistenceProfileCatalogService profileCatalogService,
        SkillsPersistenceProfileInspectService profileInspectService,
        SkillsPersistenceConfigValidationService configValidationService,
        SkillsPersistenceConfigResolveService configResolveService,
        SkillsConfigSampleService configSampleService,
        SkillsConfigSampleCatalogService configSampleCatalogService,
        SkillsConfigCatalogService configCatalogService) {

    SkillsCommandServices {
        definitionCommandService = Objects.requireNonNull(definitionCommandService, "definitionCommandService");
        definitionQueryService = Objects.requireNonNull(definitionQueryService, "definitionQueryService");
        definitionInfoCommandService =
                Objects.requireNonNull(definitionInfoCommandService, "definitionInfoCommandService");
        lifecycleCommandService = Objects.requireNonNull(lifecycleCommandService, "lifecycleCommandService");
        statusService = Objects.requireNonNull(statusService, "statusService");
        profileCatalogService = Objects.requireNonNull(profileCatalogService, "profileCatalogService");
        profileInspectService = Objects.requireNonNull(profileInspectService, "profileInspectService");
        configValidationService = Objects.requireNonNull(configValidationService, "configValidationService");
        configResolveService = Objects.requireNonNull(configResolveService, "configResolveService");
        configSampleService = Objects.requireNonNull(configSampleService, "configSampleService");
        configSampleCatalogService =
                Objects.requireNonNull(configSampleCatalogService, "configSampleCatalogService");
        configCatalogService = Objects.requireNonNull(configCatalogService, "configCatalogService");
    }

    static SkillsCommandServices from(
            SkillManagementService managementService,
            SkillManagementServiceConfig managementConfig,
            SkillManagementServiceFactory preflightFactory) {
        SkillManagementService resolvedManagementService =
                Objects.requireNonNull(managementService, "managementService");
        SkillManagementServiceConfig resolvedManagementConfig = managementConfig == null
                ? SkillManagementServiceConfig.defaults()
                : managementConfig;
        SkillsPersistenceStatusService statusService = new SkillsPersistenceStatusService(
                resolvedManagementConfig,
                Objects.requireNonNull(preflightFactory, "preflightFactory"));
        SkillsPersistenceConfigResolutionService resolutionService =
                new SkillsPersistenceConfigResolutionService(resolvedManagementConfig);
        SkillsDefinitionQueryService definitionQueryService =
                new SkillsDefinitionQueryService(resolvedManagementService);
        return new SkillsCommandServices(
                new SkillsDefinitionCommandService(resolvedManagementService),
                definitionQueryService,
                new SkillsDefinitionInfoCommandService(definitionQueryService),
                new SkillsLifecycleCommandService(resolvedManagementService),
                statusService,
                new SkillsPersistenceProfileCatalogService(),
                new SkillsPersistenceProfileInspectService(statusService),
                new SkillsPersistenceConfigValidationService(resolutionService),
                new SkillsPersistenceConfigResolveService(resolutionService),
                new SkillsConfigSampleService(),
                new SkillsConfigSampleCatalogService(),
                new SkillsConfigCatalogService());
    }
}
