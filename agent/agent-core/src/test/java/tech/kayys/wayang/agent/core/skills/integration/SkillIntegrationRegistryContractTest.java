package tech.kayys.wayang.agent.core.skills.integration;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.agent.core.skills.adapters.HITLSkillExecutor;
import tech.kayys.wayang.agent.core.skills.adapters.MCPSkillProvider;
import tech.kayys.wayang.agent.core.skills.adapters.PromptContextProvider;
import tech.kayys.wayang.agent.core.skills.adapters.RAGAwareSkillContext;
import tech.kayys.wayang.agent.core.skills.adapters.SkillAsToolAdapter;
import tech.kayys.wayang.agent.core.skills.adapters.SkillMemoryProvider;
import tech.kayys.wayang.agent.core.skills.adapters.SkillSafetyValidator;
import tech.kayys.wayang.agent.core.skills.adapters.VectorSkillIndexer;
import tech.kayys.wayang.agent.core.skills.loader.SkillManifestCatalogChange;
import tech.kayys.wayang.agent.core.skills.loader.SkillManifestSnapshot;
import tech.kayys.wayang.agent.core.skills.manifest.SkillManifest;
import tech.kayys.wayang.agent.core.skills.support.TestSkillContexts;
import tech.kayys.wayang.agent.core.skills.support.TestSkillRegistry;
import tech.kayys.wayang.agent.spi.skills.SkillContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillIntegrationRegistryContractTest {

    @Test
    void reportsPendingStatusBeforeInitialization() {
        SkillIntegrationRegistry registry = registry();

        Map<String, String> status = registry.getIntegrationStatus();

        assertEquals(List.of("tools", "prompt", "memory", "guardrails", "hitl", "rag", "mcp", "vector"),
                List.copyOf(status.keySet()));
        assertTrue(status.values().stream().allMatch("pending"::equals));
        assertThrows(UnsupportedOperationException.class, () -> status.put("new", "initialized"));
        assertTrue(registry.getToolAdapters().isEmpty());
        assertTrue(registry.getMcpProvider().isEmpty());
        assertTrue(registry.getVectorIndexer().isEmpty());
    }

    @Test
    void initializesRegistryScopedAdaptersAndStatus() {
        SkillIntegrationRegistry registry = registry();

        SkillIntegrationRegistry initialized = registry.initialize().await().indefinitely();

        assertSame(registry, initialized);
        assertTrue(registry.getIntegrationStatus().values().stream().allMatch("initialized"::equals));

        List<SkillAsToolAdapter> tools = registry.getToolAdapters();
        assertEquals(2, tools.size());
        assertEquals("rag-search", tools.get(0).id());
        assertThrows(UnsupportedOperationException.class, () -> tools.add(tools.get(0)));

        MCPSkillProvider mcpProvider = registry.getMcpProvider().orElseThrow();
        assertEquals(2, mcpProvider.listSkillsAsResources().await().indefinitely().size());

        VectorSkillIndexer indexer = registry.getVectorIndexer().orElseThrow();
        assertFalse(indexer.searchByEmbedding("search documents", 1).await().indefinitely().isEmpty());
    }

    @Test
    void createsContextScopedAdaptersThroughTypedFactories() {
        SkillIntegrationRegistry registry = registry().initialize().await().indefinitely();
        SkillContext context = TestSkillContexts.context("rag-search", null);

        assertInstanceOf(PromptContextProvider.class, registry.createPromptContextProvider(context));
        assertInstanceOf(SkillMemoryProvider.class, registry.createMemoryProvider(context));
        assertInstanceOf(SkillSafetyValidator.class, registry.createSafetyValidator(context));
        assertInstanceOf(HITLSkillExecutor.class, registry.createHITLExecutor(context));
        assertInstanceOf(RAGAwareSkillContext.class, registry.createRAGAwareSkillContext(context));
    }

    @Test
    void contextScopedFactoriesRequireInitializationAndContext() {
        SkillIntegrationRegistry registry = registry();
        SkillContext context = TestSkillContexts.context("rag-search", null);

        assertThrows(IllegalStateException.class, () -> registry.createPromptContextProvider(context));
        registry.initialize().await().indefinitely();
        assertThrows(NullPointerException.class, () -> registry.createPromptContextProvider(null));
    }

    @Test
    void initializeCanRunMoreThanOnceWithoutDuplicatingTools() {
        SkillIntegrationRegistry registry = registry();

        registry.initialize().await().indefinitely();
        registry.initialize().await().indefinitely();

        assertEquals(2, registry.getToolAdapters().size());
        assertTrue(registry.getIntegrationStatus().values().stream().allMatch("initialized"::equals));
    }

    @Test
    void refreshesCacheBackedIntegrationsFromManifestDiffs() {
        TestSkillRegistry skills = TestSkillRegistry.of(
                TestSkillRegistry.skill("rag-search", "RAG Search", "Search documents with retrieval"),
                TestSkillRegistry.skill("email-draft", "Email Draft", "Draft concise messages"));
        SkillIntegrationRegistry registry = new SkillIntegrationRegistry(skills).initialize().await().indefinitely();

        skills.unregisterSkill("rag-search");
        skills.registerSkill(TestSkillRegistry.skill("email-draft", "Email Draft", "Draft polished messages"));
        skills.registerSkill(TestSkillRegistry.skill("summarize", "Summarize", "Summarize documents"));

        SkillManifestCatalogChange change = SkillManifestCatalogChange.between(
                manifestSnapshot(
                        manifest("rag-search", "old"),
                        manifest("email-draft", "old")),
                manifestSnapshot(
                        manifest("email-draft", "changed"),
                        manifest("summarize", "new")));

        SkillIntegrationRefreshResult result = registry.refreshSkillIntegrations(change).await().indefinitely();

        assertTrue(result.hasRequestedChanges());
        assertTrue(result.hasIntegrationWork());
        assertEquals(List.of("tools", "vector"), result.refreshedIntegrationKeys());

        SkillIntegrationRefreshImpact toolImpact = result.impact("tools").orElseThrow();
        assertEquals(List.of("summarize", "email-draft"), toolImpact.refreshed());
        assertEquals(List.of("rag-search"), toolImpact.removed());
        assertTrue(toolImpact.skipped().isEmpty());
        assertEquals(List.of("email-draft", "summarize"),
                registry.getToolAdapters().stream().map(SkillAsToolAdapter::id).toList());

        SkillIntegrationRefreshImpact vectorImpact = result.impact("vector").orElseThrow();
        assertEquals(List.of("summarize", "email-draft"), vectorImpact.refreshed());
        assertEquals(List.of("rag-search"), vectorImpact.removed());
        assertTrue(vectorImpact.skipped().isEmpty());

        VectorSkillIndexer indexer = registry.getVectorIndexer().orElseThrow();
        assertEquals(0, indexer.getSkillVector("rag-search").vector().length);
        assertTrue(indexer.getSkillVector("summarize").vector().length > 0);
    }

    @Test
    void requiresSkillRegistry() {
        assertThrows(NullPointerException.class, () -> new SkillIntegrationRegistry(null));
    }

    private static SkillIntegrationRegistry registry() {
        return new SkillIntegrationRegistry(TestSkillRegistry.of(
                TestSkillRegistry.skill("rag-search", "RAG Search", "Search documents with retrieval"),
                TestSkillRegistry.skill("email-draft", "Email Draft", "Draft concise messages")));
    }

    private static SkillManifestSnapshot manifestSnapshot(SkillManifest... manifests) {
        Map<String, SkillManifest> byName = new LinkedHashMap<>();
        for (SkillManifest manifest : manifests) {
            byName.put(manifest.getName(), manifest);
        }
        return SkillManifestSnapshot.from(byName);
    }

    private static SkillManifest manifest(String name, String body) {
        return SkillManifest.builder()
                .name(name)
                .description("Integration refresh fixture")
                .version("1.0.0")
                .bodyContent(body)
                .build();
    }
}
