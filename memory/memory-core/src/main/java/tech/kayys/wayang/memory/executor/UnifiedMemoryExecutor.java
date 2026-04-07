package tech.kayys.wayang.memory.executor;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gamelan.engine.node.NodeExecutionResult;
import tech.kayys.gamelan.engine.node.NodeExecutionTask;
import tech.kayys.gamelan.engine.protocol.CommunicationType;
import tech.kayys.gamelan.sdk.executor.core.Executor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Unified memory executor handling operations for all enabled memory types.
 * Replaces individual executors (SemanticMemoryExecutor, EpisodicMemoryExecutor, etc.).
 */
@ApplicationScoped
@Executor(
        executorType = "memory-executor",
        communicationType = CommunicationType.GRPC,
        maxConcurrentTasks = 30,
        supportedNodeTypes = {"memory", "memory-task", "memory-node", "memory-operation", "semantic-memory", "episodic-memory", "working-memory", "short-memory", "longterm-memory"},
        version = "1.0.0"
)
public class UnifiedMemoryExecutor extends AbstractMemoryExecutor {

    private static final Logger LOG = LoggerFactory.getLogger(UnifiedMemoryExecutor.class);

    // Configuration flags for each memory type
    @ConfigProperty(name = "wayang.memory.semantic.enabled", defaultValue = "true")
    boolean semanticEnabled;

    @ConfigProperty(name = "wayang.memory.episodic.enabled", defaultValue = "true")
    boolean episodicEnabled;

    @ConfigProperty(name = "wayang.memory.working.enabled", defaultValue = "true")
    boolean workingEnabled;

    @ConfigProperty(name = "wayang.memory.short.enabled", defaultValue = "true")
    boolean shortTermEnabled;

    @ConfigProperty(name = "wayang.memory.longterm.enabled", defaultValue = "true")
    boolean longTermEnabled;

    @Override
    protected String getMemoryType() {
        return "unified"; 
    }

    @Override
    public Uni<NodeExecutionResult> execute(NodeExecutionTask task) {
        Instant startedAt = Instant.now();
        Map<String, Object> context = task.context() == null ? Map.of() : task.context();
        String agentId = resolveAgentId(context);
        MemoryOperationType operation = resolveOperation(context);
        
        // Use "semantic" as default if none specified, but we ideally want multiple enabled
        String memoryTypeRequested = (String) context.getOrDefault("memoryType", "semantic");

        if (!isMemoryTypeEnabled(memoryTypeRequested)) {
            LOG.warn("Requested memory type '{}' is disabled by configuration.", memoryTypeRequested);
            return Uni.createFrom().item(createFailureResult(task, "Memory type '" + memoryTypeRequested + "' is disabled.", startedAt));
        }

        LOG.info("Executing unified memory task: runId={}, nodeId={}, agentId={}, memoryType={}, operation={}",
                task.runId(), task.nodeId(), agentId, memoryTypeRequested, operation);

        // We override beforeExecute to inject specific behavior based on type if needed, 
        // but for now, rely on AbstractMemoryExecutor logic + proper namespace overriding.
        // The AbstractMemoryExecutor methods will call store/retrieve on the AgentMemory SPI.
        // We override the base methods to ensure the requested memory type is attached to metadata.

        return super.execute(task);
    }
    
    @Override
    protected tech.kayys.wayang.memory.spi.MemoryEntry createMemoryEntry(String content, Map<String, Object> context) {
        tech.kayys.wayang.memory.spi.MemoryEntry entry = super.createMemoryEntry(content, context);
        String memoryType = (String) context.getOrDefault("memoryType", "semantic");
        
        // Override the default "unified" type with the actually requested subclass of memory
        entry.metadata().put("memoryType", memoryType);
        
        // Add specific categories if needed based on type
        if ("semantic".equals(memoryType)) {
            entry.metadata().putIfAbsent("category", context.getOrDefault("category", "general"));
        } else if ("episodic".equals(memoryType)) {
            entry.metadata().putIfAbsent("event", context.getOrDefault("event", "interaction"));
        }
        
        return entry;
    }

    private boolean isMemoryTypeEnabled(String type) {
        if (type == null) return false;
        return switch (type.toLowerCase()) {
            case "semantic" -> semanticEnabled;
            case "episodic" -> episodicEnabled;
            case "working" -> workingEnabled;
            case "short", "shortterm" -> shortTermEnabled;
            case "longterm" -> longTermEnabled;
            default -> false; // If unknown type, reject
        };
    }
}
