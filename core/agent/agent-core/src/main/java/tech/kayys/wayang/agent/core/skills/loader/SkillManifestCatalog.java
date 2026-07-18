package tech.kayys.wayang.agent.core.skills.loader;

import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory catalog of loaded filesystem skill manifests.
 */
final class SkillManifestCatalog {

    private final Path skillsDirectory;
    private final SkillsLoaderService loaderService;
    private SkillManifestSnapshot snapshot = SkillManifestSnapshot.empty();

    SkillManifestCatalog(Path skillsDirectory, SkillsLoaderService loaderService) {
        this.skillsDirectory = Objects.requireNonNull(skillsDirectory, "skillsDirectory");
        this.loaderService = Objects.requireNonNull(loaderService, "loaderService");
    }

    Map<String, SkillManifest> loadAll() throws IOException {
        return reload().after().manifests();
    }

    SkillManifestCatalogChange reload() throws IOException {
        SkillManifestSnapshot before = snapshot;
        Map<String, SkillManifest> loaded = new LinkedHashMap<>();
        List<SkillManifest> discovered = loaderService.loadSkillsFromDirectory(skillsDirectory);
        for (SkillManifest manifest : discovered) {
            if (manifest != null && manifest.getName() != null) {
                loaded.put(manifest.getName(), manifest);
            }
        }
        snapshot = SkillManifestSnapshot.from(loaded);
        return SkillManifestCatalogChange.between(before, snapshot);
    }

    SkillManifestSnapshot snapshot() {
        return snapshot;
    }

    boolean contains(String skillName) {
        return snapshot.contains(skillName);
    }

    Optional<SkillManifest> get(String skillName) {
        return snapshot.get(skillName);
    }

    Collection<String> names() {
        return Collections.unmodifiableSet(snapshot.names());
    }
}
