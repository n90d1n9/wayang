package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.skills.management.SkillManagementService;
import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves the learned-skill persistence adapter implied by the target plan.
 */
public final class HermesLearnedSkillPersistenceAdapterResolver {

    private static final String FILE_SYSTEM_BACKEND = "file-system";

    private HermesLearnedSkillPersistenceAdapterResolver() {
    }

    public static HermesLearnedSkillPersistenceAdapter resolve(
            SkillManagementService skillManagementService,
            HermesAgentModeConfig config) {
        return resolve(skillManagementService, config, Optional.empty(), Optional.empty());
    }

    public static HermesLearnedSkillPersistenceAdapter resolve(
            SkillManagementService skillManagementService,
            HermesAgentModeConfig config,
            Optional<ObjectStorageService> objectStorageService,
            Optional<DataSource> dataSource) {
        HermesAgentModeConfig effective = config == null ? HermesAgentModeConfig.defaults() : config;
        return resolve(
                skillManagementService,
                effective.skillPersistenceStrategy().routePlan().targetPlan(),
                HermesLearnedSkillPersistenceAdapterResolverOptions.fromHints(effective.persistenceHints()),
                HermesPersistenceResources.of(objectStorageService, dataSource));
    }

    public static HermesLearnedSkillPersistenceAdapter resolve(
            SkillManagementService skillManagementService,
            HermesSkillPersistenceTargetPlan targetPlan) {
        return resolve(skillManagementService, targetPlan, HermesLearnedSkillPersistenceAdapterResolverOptions.defaults());
    }

    public static HermesLearnedSkillPersistenceAdapter resolve(
            SkillManagementService skillManagementService,
            HermesSkillPersistenceTargetPlan targetPlan,
            HermesLearnedSkillPersistenceAdapterResolverOptions options) {
        return resolve(skillManagementService, targetPlan, options, HermesPersistenceResources.empty());
    }

    static HermesLearnedSkillPersistenceAdapter resolve(
            SkillManagementService skillManagementService,
            HermesSkillPersistenceTargetPlan targetPlan,
            HermesLearnedSkillPersistenceAdapterResolverOptions options,
            HermesPersistenceResources resources) {
        HermesSkillPersistenceTargetPlan effectivePlan = targetPlan == null
                ? HermesSkillPersistencePlan.from(null).targetPlan()
                : targetPlan;
        HermesLearnedSkillPersistenceAdapterResolverOptions effectiveOptions =
                options == null ? HermesLearnedSkillPersistenceAdapterResolverOptions.defaults() : options;
        if (fileSystemOnly(effectivePlan)) {
            return new FileSystemHermesLearnedSkillPersistenceAdapter(
                    effectiveOptions.fileSystemDefinitionDirectory(),
                    effectiveOptions.fileSystemArtifactDirectory());
        }
        Optional<SkillManagementService> dedicatedService =
                HermesLearnedSkillManagementServiceFactory.createIfAvailable(
                        effectivePlan,
                        effectiveOptions,
                        resources);
        if (dedicatedService.isPresent()) {
            return SkillManagementHermesLearnedSkillPersistenceAdapter.from(
                    dedicatedService.orElseThrow(),
                    effectivePlan);
        }
        return SkillManagementHermesLearnedSkillPersistenceAdapter.from(
                Objects.requireNonNull(skillManagementService, "skillManagementService"),
                effectivePlan);
    }

    public static boolean fileSystemOnly(HermesSkillPersistenceTargetPlan targetPlan) {
        HermesSkillPersistenceTargetPlan effectivePlan = targetPlan == null
                ? HermesSkillPersistencePlan.from(null).targetPlan()
                : targetPlan;
        return selectedBackendIs(effectivePlan.definitions(), FILE_SYSTEM_BACKEND)
                && selectedBackendIs(effectivePlan.artifacts(), FILE_SYSTEM_BACKEND);
    }

    private static boolean selectedBackendIs(HermesSkillPersistenceTarget target, String backendId) {
        String expected = HermesSkillPersistenceBackendProfile.normalizeBackendId(backendId);
        return target != null
                && target.selectedBackendId()
                        .map(HermesSkillPersistenceBackendProfile::normalizeBackendId)
                        .filter(expected::equals)
                        .isPresent();
    }
}
