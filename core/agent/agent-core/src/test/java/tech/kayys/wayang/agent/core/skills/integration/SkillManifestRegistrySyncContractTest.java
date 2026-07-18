package tech.kayys.wayang.agent.core.skills.integration;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.skills.loader.SkillManifestCatalogChange;
import tech.kayys.wayang.agent.core.skills.loader.SkillManifestSnapshot;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.core.skills.support.TestSkillRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillManifestRegistrySyncContractTest {

    @Test
    void synchronizesAddedUpdatedAndRemovedManifestSkills() {
        TestSkillRegistry registry = TestSkillRegistry.of(
                TestSkillRegistry.skill("old-skill", "Old Skill", "Retired"),
                TestSkillRegistry.skill("draft", "Draft", "Old description"));
        SkillManifestRegistrySync sync = new SkillManifestRegistrySync(registry);
        SkillManifestCatalogChange change = SkillManifestCatalogChange.between(
                snapshot(manifest("old-skill", "1.0.0", "old"), manifest("draft", "1.0.0", "old")),
                snapshot(manifest("draft", "2.0.0", "changed"), manifest("summarize", "1.0.0", "new")));

        SkillManifestRegistrySyncResult result = sync.synchronize(change);

        assertEquals(List.of("summarize", "draft"), result.registered());
        assertEquals(List.of("old-skill"), result.removed());
        assertTrue(result.skipped().isEmpty());
        assertTrue(result.hasWork());

        assertFalse(registry.getSkill("old-skill").isPresent());
        assertEquals("changed", registry.getSkill("draft").orElseThrow().systemPrompt());
        assertEquals("new", registry.getSkill("summarize").orElseThrow().systemPrompt());
    }

    @Test
    void requiresRegistry() {
        assertThrows(NullPointerException.class, () -> new SkillManifestRegistrySync(null));
    }

    private static SkillManifestSnapshot snapshot(SkillManifest... manifests) {
        Map<String, SkillManifest> byName = new LinkedHashMap<>();
        for (SkillManifest manifest : manifests) {
            byName.put(manifest.getName(), manifest);
        }
        return SkillManifestSnapshot.from(byName);
    }

    private static SkillManifest manifest(String name, String version, String body) {
        return SkillManifest.builder()
                .name(name)
                .description("Registry sync fixture")
                .version(version)
                .bodyContent(body)
                .build();
    }
}
