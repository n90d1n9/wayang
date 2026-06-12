package tech.kayys.wayang.agent.core.skills.loader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillManifestCatalogContractTest {

    @TempDir
    Path skillsDir;

    @Test
    void loadsImmutableSnapshotsAndReplacesCatalogOnReload() throws Exception {
        FakeSkillsLoaderService loaderService = new FakeSkillsLoaderService(skillsDir);
        SkillManifestCatalog catalog = new SkillManifestCatalog(skillsDir, loaderService);
        loaderService.nextLoad(manifest("alpha"));

        Map<String, SkillManifest> firstSnapshot = catalog.loadAll();

        assertTrue(catalog.contains("alpha"));
        assertEquals("alpha", catalog.get("alpha").orElseThrow().getName());
        assertEquals(List.of("alpha"), catalog.names().stream().toList());
        assertThrows(UnsupportedOperationException.class, () -> firstSnapshot.put("later", manifest("later")));
        assertThrows(UnsupportedOperationException.class, () -> catalog.names().add("later"));

        loaderService.nextLoad(manifest("beta"));
        Map<String, SkillManifest> secondSnapshot = catalog.loadAll();

        assertTrue(firstSnapshot.containsKey("alpha"));
        assertFalse(secondSnapshot.containsKey("alpha"));
        assertFalse(catalog.contains("alpha"));
        assertTrue(catalog.contains("beta"));
        assertEquals(List.of("beta"), catalog.names().stream().toList());
    }

    @Test
    void reportsAddedUpdatedAndRemovedManifestDiffs() throws Exception {
        FakeSkillsLoaderService loaderService = new FakeSkillsLoaderService(skillsDir);
        SkillManifestCatalog catalog = new SkillManifestCatalog(skillsDir, loaderService);

        loaderService.nextLoad(manifest("alpha", "1.0.0", "first"), manifest("beta", "1.0.0", "second"));
        SkillManifestCatalogChange firstChange = catalog.reload();

        assertTrue(firstChange.hasChanges());
        assertEquals(List.of("alpha", "beta"), firstChange.added());
        assertTrue(firstChange.updated().isEmpty());
        assertTrue(firstChange.removed().isEmpty());
        assertEquals(List.of("alpha", "beta"), firstChange.changedNames());
        assertThrows(UnsupportedOperationException.class, () -> firstChange.added().add("later"));
        assertThrows(UnsupportedOperationException.class, () -> firstChange.after().manifests().put("later", manifest("later")));

        SkillManifestSnapshot firstSnapshot = catalog.snapshot();
        loaderService.nextLoad(manifest("alpha", "2.0.0", "changed"), manifest("gamma", "1.0.0", "third"));
        SkillManifestCatalogChange secondChange = catalog.reload();

        assertEquals(firstSnapshot, secondChange.before());
        assertEquals(List.of("gamma"), secondChange.added());
        assertEquals(List.of("beta"), secondChange.removed());
        assertEquals(List.of("alpha"), secondChange.updated());
        assertEquals(List.of("gamma", "alpha", "beta"), secondChange.changedNames());

        loaderService.nextLoad(manifest("alpha", "2.0.0", "changed"), manifest("gamma", "1.0.0", "third"));
        SkillManifestCatalogChange unchanged = catalog.reload();

        assertFalse(unchanged.hasChanges());
        assertTrue(unchanged.changedNames().isEmpty());
    }

    @Test
    void ignoresManifestMetadataMapOrderWhenDetectingUpdates() {
        Map<String, Object> firstMetadata = new LinkedHashMap<>();
        firstMetadata.put("region", "id");
        firstMetadata.put("tier", "core");

        Map<String, Object> secondMetadata = new LinkedHashMap<>();
        secondMetadata.put("tier", "core");
        secondMetadata.put("region", "id");

        SkillManifestSnapshot first = SkillManifestSnapshot.from(Map.of("alpha", manifest("alpha", firstMetadata)));
        SkillManifestSnapshot second = SkillManifestSnapshot.from(Map.of("alpha", manifest("alpha", secondMetadata)));

        SkillManifestCatalogChange change = SkillManifestCatalogChange.between(first, second);

        assertFalse(change.hasChanges());
        assertEquals(first.fingerprint("alpha"), second.fingerprint("alpha"));
    }

    private static SkillManifest manifest(String name) {
        return manifest(name, "1.0.0", "Catalog contract fixture");
    }

    private static SkillManifest manifest(String name, String version, String body) {
        return SkillManifest.builder()
                .name(name)
                .description("Catalog contract fixture")
                .version(version)
                .bodyContent(body)
                .build();
    }

    private static SkillManifest manifest(String name, Map<String, Object> metadata) {
        return SkillManifest.builder()
                .name(name)
                .description("Catalog contract fixture")
                .version("1.0.0")
                .metadata(metadata)
                .bodyContent("Catalog contract fixture")
                .build();
    }

    private static final class FakeSkillsLoaderService implements SkillsLoaderService {
        private final Path skillsBaseDir;
        private List<SkillManifest> manifests = List.of();

        private FakeSkillsLoaderService(Path skillsBaseDir) {
            this.skillsBaseDir = skillsBaseDir;
        }

        private void nextLoad(SkillManifest... manifests) {
            this.manifests = List.of(manifests);
        }

        @Override
        public SkillLoaderResult installLocal(String localPath, String skillFilter) {
            throw new UnsupportedOperationException("not needed for catalog contract");
        }

        @Override
        public SkillLoaderResult installGit(String repoUrl, String skillFilter) {
            throw new UnsupportedOperationException("not needed for catalog contract");
        }

        @Override
        public SkillLoaderResult update(String repoName) {
            throw new UnsupportedOperationException("not needed for catalog contract");
        }

        @Override
        public boolean remove(String repoName) {
            throw new UnsupportedOperationException("not needed for catalog contract");
        }

        @Override
        public List<String> listInstalledRepos() {
            return List.of();
        }

        @Override
        public List<SkillManifest> loadSkillsFromDirectory(Path directory) {
            return new ArrayList<>(manifests);
        }

        @Override
        public Path getSkillsBaseDir() {
            return skillsBaseDir;
        }
    }
}
