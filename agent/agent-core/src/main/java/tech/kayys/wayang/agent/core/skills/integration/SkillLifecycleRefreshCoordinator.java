package tech.kayys.wayang.agent.core.skills.integration;

import tech.kayys.wayang.agent.core.skills.loader.SkillExecutor;
import tech.kayys.wayang.agent.core.skills.loader.SkillManifestCatalogChange;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Coordinates filesystem skill reloads with integration-cache refresh hooks.
 */
public final class SkillLifecycleRefreshCoordinator {

    private final SkillExecutor skillExecutor;
    private final SkillIntegrationRegistry integrationRegistry;
    private final SkillManifestRegistrySync registrySync;

    public SkillLifecycleRefreshCoordinator(SkillExecutor skillExecutor) {
        this(skillExecutor, null);
    }

    public SkillLifecycleRefreshCoordinator(
            SkillExecutor skillExecutor,
            SkillIntegrationRegistry integrationRegistry) {
        this.skillExecutor = Objects.requireNonNull(skillExecutor, "skillExecutor");
        this.integrationRegistry = integrationRegistry;
        this.registrySync = integrationRegistry == null
                ? null
                : new SkillManifestRegistrySync(integrationRegistry.skillRegistry());
    }

    public SkillLifecycleRefreshResult reloadSkills() throws IOException {
        SkillManifestCatalogChange change = skillExecutor.reloadSkills();
        return refreshIntegrations(change);
    }

    public SkillLifecycleRefreshResult refreshIntegrations(SkillManifestCatalogChange change) {
        SkillManifestRegistrySyncResult syncResult = syncRegistry(change);
        SkillIntegrationRefreshResult refresh = Optional.ofNullable(integrationRegistry)
                .map(registry -> registry.refreshSkillIntegrations(change).await().indefinitely())
                .orElseGet(() -> new SkillIntegrationRefreshResult(
                        SkillIntegrationRefreshRequest.from(change),
                        List.of()));
        return new SkillLifecycleRefreshResult(change, syncResult, refresh);
    }

    public SkillExecutor skillExecutor() {
        return skillExecutor;
    }

    private SkillManifestRegistrySyncResult syncRegistry(SkillManifestCatalogChange change) {
        return Optional.ofNullable(registrySync)
                .map(sync -> sync.synchronize(change))
                .orElseGet(SkillManifestRegistrySyncResult::empty);
    }
}
