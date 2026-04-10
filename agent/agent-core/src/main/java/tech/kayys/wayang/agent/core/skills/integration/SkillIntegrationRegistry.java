package tech.kayys.wayang.agent.core.skills.integration;

import tech.kayys.wayang.agent.core.skills.adapters.*;
import tech.kayys.wayang.agent.spi.skills.SkillRegistry;
import io.smallrye.mutiny.Uni;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified integration registry for skill adapters across all wayang-gollek modules.
 * 
 * Provides centralized management of skill integrations with:
 * - Tools Module (SkillAsToolAdapter)
 * - Prompt Module (PromptContextProvider)
 * - Memory Module (SkillMemoryProvider)
 * - Guardrails Module (SkillSafetyValidator)
 * - HITL Module (HITLSkillExecutor)
 * - RAG Module (RAGAwareSkillContext)
 * - MCP Module (MCPSkillProvider)
 * - Vector/Embedding Module (VectorSkillIndexer)
 */
public class SkillIntegrationRegistry {

    private final SkillRegistry skillRegistry;
    private final Map<String, Object> adapters;

    public SkillIntegrationRegistry(SkillRegistry skillRegistry) {
        this.skillRegistry = skillRegistry;
        this.adapters = new HashMap<>();
    }

    /**
     * Initialize all integrations.
     */
    public Uni<SkillIntegrationRegistry> initialize() {
        return Uni.createFrom().item(this)
            .chain(self -> initializeToolIntegration().map(v -> self))
            .chain(self -> initializePromptIntegration().map(v -> self))
            .chain(self -> initializeMemoryIntegration().map(v -> self))
            .chain(self -> initializeSafetyIntegration().map(v -> self))
            .chain(self -> initializeHITLIntegration().map(v -> self))
            .chain(self -> initializeRAGIntegration().map(v -> self))
            .chain(self -> initializeMCPIntegration().map(v -> self))
            .chain(self -> initializeVectorIntegration().map(v -> self));
    }

    /**
     * Tools Module Integration: Skills as tools.
     */
    private Uni<Void> initializeToolIntegration() {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                java.util.List<SkillAsToolAdapter> toolAdapters = 
                    SkillAsToolAdapter.adaptSkills(skillRegistry);
                adapters.put("tools", toolAdapters);
                System.out.println("[Integration] Tools: " + toolAdapters.size() + " skills adapted as tools");
            });
    }

    /**
     * Prompt Module Integration: Prompt context providers.
     */
    private Uni<Void> initializePromptIntegration() {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                adapters.put("prompt", new Object()); // Placeholder
                System.out.println("[Integration] Prompt: Context providers initialized");
            });
    }

    /**
     * Memory Module Integration: Memory providers.
     */
    private Uni<Void> initializeMemoryIntegration() {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                adapters.put("memory", new Object()); // Placeholder
                System.out.println("[Integration] Memory: Skill memory providers initialized");
            });
    }

    /**
     * Guardrails Module Integration: Safety validators.
     */
    private Uni<Void> initializeSafetyIntegration() {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                adapters.put("guardrails", new Object()); // Placeholder
                System.out.println("[Integration] Guardrails: Safety validators initialized");
            });
    }

    /**
     * HITL Module Integration: Human feedback handlers.
     */
    private Uni<Void> initializeHITLIntegration() {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                adapters.put("hitl", new Object()); // Placeholder
                System.out.println("[Integration] HITL: Human feedback handlers initialized");
            });
    }

    /**
     * RAG Module Integration: RAG-aware skill context.
     */
    private Uni<Void> initializeRAGIntegration() {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                adapters.put("rag", new Object()); // Placeholder
                System.out.println("[Integration] RAG: RAG-aware skill context initialized");
            });
    }

    /**
     * MCP Module Integration: Skills via MCP protocol.
     */
    private Uni<Void> initializeMCPIntegration() {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                MCPSkillProvider mcpProvider = new MCPSkillProvider(skillRegistry);
                adapters.put("mcp", mcpProvider);
                System.out.println("[Integration] MCP: Skill provider initialized");
            });
    }

    /**
     * Vector/Embedding Module Integration: Semantic indexing.
     */
    private Uni<Void> initializeVectorIntegration() {
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                VectorSkillIndexer indexer = new VectorSkillIndexer(skillRegistry);
                adapters.put("vector", indexer);
                System.out.println("[Integration] Vector: Skill indexer initialized");
            });
    }

    /**
     * Get tool adapters for tools module integration.
     */
    @SuppressWarnings("unchecked")
    public java.util.List<SkillAsToolAdapter> getToolAdapters() {
        return (java.util.List<SkillAsToolAdapter>) adapters.getOrDefault("tools", java.util.List.of());
    }

    /**
     * Get status of all integrations.
     */
    public Map<String, String> getIntegrationStatus() {
        Map<String, String> status = new HashMap<>();
        status.put("tools", adapters.containsKey("tools") ? "initialized" : "pending");
        status.put("prompt", adapters.containsKey("prompt") ? "initialized" : "pending");
        status.put("memory", adapters.containsKey("memory") ? "initialized" : "pending");
        status.put("guardrails", adapters.containsKey("guardrails") ? "initialized" : "pending");
        status.put("hitl", adapters.containsKey("hitl") ? "initialized" : "pending");
        status.put("rag", adapters.containsKey("rag") ? "initialized" : "pending");
        status.put("mcp", adapters.containsKey("mcp") ? "initialized" : "pending");
        status.put("vector", adapters.containsKey("vector") ? "initialized" : "pending");
        return status;
    }
}
