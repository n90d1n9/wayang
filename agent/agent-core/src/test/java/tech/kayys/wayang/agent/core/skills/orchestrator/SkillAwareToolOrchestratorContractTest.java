package tech.kayys.wayang.agent.core.skills.orchestrator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.core.skills.integration.SkillIntegrationRefreshImpact;
import tech.kayys.wayang.agent.core.skills.integration.SkillIntegrationRegistry;
import tech.kayys.wayang.agent.core.skills.integration.SkillLifecycleRefreshResult;
import tech.kayys.wayang.agent.core.skills.loader.SkillExecutionOutcome;
import tech.kayys.wayang.agent.core.skills.loader.SkillExecutionOutcomes;
import tech.kayys.wayang.agent.core.skills.loader.SkillFailureType;
import tech.kayys.wayang.agent.core.skills.support.TestSkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillMetadataKeys;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillAwareToolOrchestratorContractTest {

    @TempDir
    Path skillsDir;

    @Test
    void reportsUnloadedSkillThroughStableOutcomeContract() {
        SkillAwareToolOrchestrator orchestrator = new SkillAwareToolOrchestrator(skillsDir);

        SkillExecutionOutcome result = orchestrator.executeSkill("missing", Map.of());

        assertFalse(result.success());
        assertEquals("missing", result.skillName());
        assertTrue(result.error().contains("not loaded"));
        assertEquals(SkillFailureType.SKILL_NOT_LOADED, SkillExecutionOutcomes.failureType(result).orElseThrow());
        assertThrows(UnsupportedOperationException.class, () -> result.metadata().put("later", true));
    }

    @Test
    void filtersLoadedSkillsThroughSharedDomainMetadata() throws Exception {
        createSkillManifest("rag-skill", """
                metadata:
                  domains:
                    - rag
                    - search
                  output-format: json
                """);
        createSkillManifest("general-skill", "");
        createSkillManifest("code-skill", """
                metadata:
                  domains: code, review
                """);

        SkillAwareToolOrchestrator orchestrator = new SkillAwareToolOrchestrator(skillsDir);

        List<String> loaded = orchestrator.initializeForDomain("rag");

        assertTrue(loaded.contains("rag-skill"));
        assertTrue(loaded.contains("general-skill"));
        assertFalse(loaded.contains("code-skill"));
        assertTrue(orchestrator.validateSkillOutput("rag-skill", "{}"));

        SkillLifecycleRefreshResult refresh = orchestrator.getLastRefreshResult();
        assertTrue(refresh.hasCatalogChanges());
        assertFalse(refresh.hasIntegrationWork());
        assertTrue(refresh.integrationRefresh().hasRequestedChanges());
    }

    @Test
    void refreshesIntegrationsWhenInitializingDomain() throws Exception {
        createSkillManifest("rag-skill", """
                metadata:
                  domains:
                    - rag
                """);
        createSkillManifest("general-skill", "");
        TestSkillRegistry skills = TestSkillRegistry.of();
        SkillIntegrationRegistry integrations = new SkillIntegrationRegistry(skills)
                .initialize()
                .await()
                .indefinitely();
        SkillAwareToolOrchestrator orchestrator = new SkillAwareToolOrchestrator(skillsDir, integrations);

        List<String> loaded = orchestrator.initializeForDomain("rag");

        assertTrue(loaded.contains("rag-skill"));
        assertTrue(loaded.contains("general-skill"));

        SkillLifecycleRefreshResult refresh = orchestrator.getLastRefreshResult();
        assertTrue(refresh.hasCatalogChanges());
        assertTrue(refresh.hasRegistryWork());
        assertTrue(refresh.hasIntegrationWork());
        assertEquals(List.of("tools", "vector"), refresh.integrationRefresh().refreshedIntegrationKeys());
        assertTrue(refresh.catalogChange().added().containsAll(List.of("rag-skill", "general-skill")));
        assertTrue(refresh.registrySync().registered().containsAll(List.of("rag-skill", "general-skill")));
        assertTrue(skills.getSkill("rag-skill").isPresent());
        assertTrue(skills.getSkill("general-skill").isPresent());

        SkillIntegrationRefreshImpact toolImpact = refresh.integrationRefresh().impact("tools").orElseThrow();
        assertTrue(toolImpact.refreshed().containsAll(List.of("rag-skill", "general-skill")));
        assertTrue(toolImpact.removed().isEmpty());
        assertTrue(toolImpact.skipped().isEmpty());
    }

    private Path createSkillManifest(String name, String extraFrontmatter) throws Exception {
        Path skillPath = skillsDir.resolve(name);
        Files.createDirectories(skillPath);
        Files.writeString(skillPath.resolve("SKILL.md"), """
                ---
                name: %s
                description: Orchestrator metadata contract fixture
                %s
                ---

                ## Usage
                Domain metadata should use %s for wildcard domains.
                """.formatted(name, extraFrontmatter, SkillMetadataKeys.WILDCARD_DOMAIN));
        return skillPath;
    }
}
