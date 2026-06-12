package tech.kayys.wayang.agent.core.skills.integration;

import tech.kayys.wayang.agent.core.skills.loader.SkillManifestCatalogChange;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Synchronizes filesystem skill manifests into the runtime skill registry.
 */
public final class SkillManifestRegistrySync {

    private final SkillRegistry skillRegistry;
    private final SkillManifestRuntimeSkillMapper mapper;

    public SkillManifestRegistrySync(SkillRegistry skillRegistry) {
        this(skillRegistry, new SkillManifestRuntimeSkillMapper());
    }

    public SkillManifestRegistrySync(
            SkillRegistry skillRegistry,
            SkillManifestRuntimeSkillMapper mapper) {
        this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    public SkillManifestRegistrySyncResult synchronize(SkillManifestCatalogChange change) {
        if (change == null || !change.hasChanges()) {
            return SkillManifestRegistrySyncResult.empty();
        }

        List<String> removed = new ArrayList<>();
        change.removed().forEach(skillName -> {
            if (skillRegistry.unregisterSkill(skillName)) {
                removed.add(skillName);
            }
        });

        List<String> registered = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        change.changedNames().stream()
                .filter(skillName -> !change.removed().contains(skillName))
                .forEach(skillName -> {
                    SkillManifest manifest = change.after().get(skillName).orElse(null);
                    if (manifest == null) {
                        skipped.add(skillName);
                        return;
                    }
                    try {
                        skillRegistry.registerSkill(mapper.toSkillDefinition(manifest));
                        registered.add(skillName);
                    } catch (RuntimeException error) {
                        skipped.add(skillName);
                    }
                });

        return new SkillManifestRegistrySyncResult(registered, removed, skipped);
    }
}
