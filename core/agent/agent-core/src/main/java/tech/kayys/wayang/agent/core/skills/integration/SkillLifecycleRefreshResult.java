package tech.kayys.wayang.agent.core.skills.integration;

import tech.kayys.wayang.agent.core.skills.loader.SkillManifestCatalogChange;
import tech.kayys.wayang.agent.core.skills.loader.SkillManifestSnapshot;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;

import java.util.Map;

/**
 * Combined filesystem catalog and integration-cache refresh result.
 */
public record SkillLifecycleRefreshResult(
        SkillManifestCatalogChange catalogChange,
        SkillManifestRegistrySyncResult registrySync,
        SkillIntegrationRefreshResult integrationRefresh) {

    public SkillLifecycleRefreshResult {
        catalogChange = catalogChange == null
                ? SkillManifestCatalogChange.between(SkillManifestSnapshot.empty(), SkillManifestSnapshot.empty())
                : catalogChange;
        registrySync = registrySync == null
                ? SkillManifestRegistrySyncResult.empty()
                : registrySync;
        integrationRefresh = integrationRefresh == null
                ? SkillIntegrationRefreshResult.empty()
                : integrationRefresh;
    }

    public SkillLifecycleRefreshResult(
            SkillManifestCatalogChange catalogChange,
            SkillIntegrationRefreshResult integrationRefresh) {
        this(catalogChange, SkillManifestRegistrySyncResult.empty(), integrationRefresh);
    }

    public static SkillLifecycleRefreshResult empty() {
        return new SkillLifecycleRefreshResult(
                null,
                SkillManifestRegistrySyncResult.empty(),
                SkillIntegrationRefreshResult.empty());
    }

    public Map<String, SkillManifest> manifests() {
        return catalogChange.after().manifests();
    }

    public boolean hasCatalogChanges() {
        return catalogChange.hasChanges();
    }

    public boolean hasIntegrationWork() {
        return integrationRefresh.hasIntegrationWork();
    }

    public boolean hasRegistryWork() {
        return registrySync.hasWork();
    }
}
