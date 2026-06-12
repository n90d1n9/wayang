package tech.kayys.wayang.agent.adapter;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.agent.spi.skills.analytics.UsageAnalyticsService;
import tech.kayys.wayang.agent.spi.AgentMemoryManager;

import java.util.Map;

/**
 * Adapter that wraps gollek memory services for backward compatibility.
 * 
 * @deprecated Use gollek memory services directly
 */
@ApplicationScoped
@Deprecated
public class AgentMemoryManagerAdapter implements AgentMemoryManager {

    private static final Logger log = LoggerFactory.getLogger(AgentMemoryManagerAdapter.class);

    @Inject
    UsageAnalyticsService analyticsService;

    // Note: In production, inject actual gollek memory service
    // @Inject
    // GollekMemoryService gollekMemory;

    @Override
    public Uni<String> storeMemory(String agentId, String content, Map<String, Object> metadata) {
        log.debug("Storing memory for agent: {}", agentId);
        
        // In production, delegate to gollek memory service
        // return gollekMemory.store(agentId, content, metadata);
        
        // Placeholder - return dummy ID
        return Uni.createFrom().item("memory-" + System.currentTimeMillis());
    }

    @Override
    public Uni<String> retrieveContext(String agentId, String query, int limit) {
        log.debug("Retrieving context for agent: {}, query: {}", agentId, query);
        
        // In production, delegate to gollek memory retrieval
        // return gollekMemory.retrieve(agentId, query, limit);
        
        // Placeholder - return empty context
        return Uni.createFrom().item("");
    }

    @Override
    public Uni<String> storeObservation(String agentId, String toolName, String observation) {
        log.debug("Storing observation for agent: {}, tool: {}", agentId, toolName);
        
        // Record analytics
        analyticsService.recordExecution(toolName, agentId, 0, true, 0);
        
        // Store as memory
        return storeMemory(agentId, toolName + ": " + observation, Map.of("type", "observation"));
    }
}
