package tech.kayys.wayang.agent.core.checkpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.AgentState;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * State persistence and recovery manager for agent checkpoint/resume functionality.
 *
 * <p>This manager enables long-running agents to:
 * <ul>
 *   <li>Save execution state at any point</li>
 *   <li>Resume from checkpoints after failures</li>
 *   <li>Maintain state across application restarts</li>
 *   <li>Support multiple checkpoint versions</li>
 * </ul>
 *
 * <h2>Checkpoint Storage:</h2>
 * <ul>
 *   <li><b>In-Memory:</b> Fast, volatile storage for recent checkpoints</li>
 *   <li><b>File System:</b> Persistent storage for long-term retention</li>
 *   <li><b>Database:</b> Optional PostgreSQL storage for production</li>
 * </ul>
 *
 * <h2>Use Cases:</h2>
 * <ul>
 *   <li>Long-running workflows that may exceed timeout limits</li>
 *   <li>Human-in-the-loop scenarios requiring pauses</li>
 *   <li>Failure recovery and retry from last known good state</li>
 *   <li>Debugging and auditing agent execution</li>
 * </ul>
 *
 * @author Wayang AI Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class AgentCheckpointManager {

    private static final Logger LOG = Logger.getLogger(AgentCheckpointManager.class);

    @Inject
    ObjectMapper objectMapper;

    // In-memory checkpoint cache
    private final Map<String, CheckpointData> memoryCache = new ConcurrentHashMap<>();
    
    // File system storage path
    private final Path checkpointDir;
    
    // Checkpoint retention policy
    private static final int MAX_CHECKPOINTS_PER_RUN = 10;
    private static final long CHECKPOINT_TTL_HOURS = 24;

    public AgentCheckpointManager() {
        // Initialize checkpoint directory
        String baseDir = System.getProperty("java.io.tmpdir", "/tmp");
        this.checkpointDir = Paths.get(baseDir, "wayang", "agent-checkpoints");
        
        try {
            Files.createDirectories(checkpointDir);
            LOG.infof("Checkpoint directory initialized: %s", checkpointDir);
        } catch (IOException e) {
            LOG.warnf("Failed to create checkpoint directory: %s", e.getMessage());
            throw new RuntimeException("Failed to initialize checkpoint manager", e);
        }
    }

    /**
     * Save a checkpoint for later recovery.
     *
     * @param runId unique run identifier
     * @param checkpointId unique checkpoint identifier
     * @param state the agent state to save
     * @return Uni that completes when checkpoint is saved
     */
    public Uni<Void> saveCheckpoint(String runId, String checkpointId, AgentState state) {
        LOG.infof("Saving checkpoint: runId=%s, checkpointId=%s", runId, checkpointId);

        CheckpointData checkpoint = new CheckpointData(
            runId,
            checkpointId,
            state,
            Instant.now(),
            "active"
        );

        // Save to memory cache
        memoryCache.put(checkpointId, checkpoint);

        // Save to file system
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                try {
                    saveToFile(checkpoint);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .invoke(() -> cleanupOldCheckpoints(runId))
            .onFailure().recoverWithItem(err -> {
                LOG.errorf(err, "Failed to save checkpoint to file system");
                // Memory cache still has the checkpoint
                return null;
            });
    }

    /**
     * Restore agent state from a checkpoint.
     *
     * @param checkpointId the checkpoint identifier
     * @return Uni containing the restored state
     */
    public Uni<AgentState> restoreCheckpoint(String checkpointId) {
        LOG.infof("Restoring checkpoint: checkpointId=%s", checkpointId);

        // Try memory cache first
        CheckpointData cached = memoryCache.get(checkpointId);
        if (cached != null) {
            LOG.debug("Checkpoint found in memory cache");
            return Uni.createFrom().item(cached.state());
        }

        // Try file system
        return Uni.createFrom().item(() -> {
                try {
                    return loadFromFile(checkpointId);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .onItem().transformToUni(checkpoint -> {
                if (checkpoint != null) {
                    // Cache for future access
                    memoryCache.put(checkpointId, checkpoint);
                    return Uni.createFrom().item(checkpoint.state());
                }
                return Uni.createFrom().failure(
                    new CheckpointNotFoundException("Checkpoint not found: " + checkpointId));
            });
    }

    /**
     * List all checkpoints for a run.
     *
     * @param runId the run identifier
     * @return list of checkpoint IDs
     */
    public List<String> listCheckpoints(String runId) {
        List<String> checkpoints = new ArrayList<>();
        
        // Check memory cache
        memoryCache.entrySet().stream()
            .filter(e -> e.getValue().runId().equals(runId))
            .map(Map.Entry::getKey)
            .forEach(checkpoints::add);

        // Check file system
        try (var stream = Files.list(checkpointDir)) {
            stream.filter(path -> path.getFileName().toString().startsWith(runId + "-"))
                .forEach(path -> {
                    String checkpointId = path.getFileName().toString()
                        .replaceFirst(runId + "-", "")
                        .replace(".json", "");
                    if (!checkpoints.contains(checkpointId)) {
                        checkpoints.add(checkpointId);
                    }
                });
        } catch (IOException e) {
            LOG.warnf("Failed to list checkpoints: %s", e.getMessage());
        }

        return checkpoints;
    }

    /**
     * Delete a checkpoint.
     *
     * @param checkpointId the checkpoint identifier
     * @return Uni that completes when deleted
     */
    public Uni<Void> deleteCheckpoint(String checkpointId) {
        LOG.infof("Deleting checkpoint: checkpointId=%s", checkpointId);

        // Remove from memory
        memoryCache.remove(checkpointId);

        // Delete from file system
        return Uni.createFrom().voidItem()
            .invoke(() -> {
                Path file = checkpointDir.resolve(checkpointId + ".json");
                try {
                    Files.deleteIfExists(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            })
            .onFailure().recoverWithItem(err -> {
                LOG.warnf("Failed to delete checkpoint file: %s", err.getMessage());
                return null;
            });
    }

    /**
     * Delete all checkpoints for a run.
     *
     * @param runId the run identifier
     * @return Uni that completes when all checkpoints are deleted
     */
    public Uni<Void> deleteRunCheckpoints(String runId) {
        LOG.infof("Deleting all checkpoints for run: %s", runId);

        List<String> checkpoints = listCheckpoints(runId);
        
        return Uni.combine().all().unis(
                checkpoints.stream()
                    .map(this::deleteCheckpoint)
                    .toList()
            )
            .discardItems();
    }

    /**
     * Get checkpoint metadata.
     *
     * @param checkpointId the checkpoint identifier
     * @return checkpoint metadata or null if not found
     */
    public CheckpointMetadata getCheckpointMetadata(String checkpointId) {
        CheckpointData checkpoint = memoryCache.get(checkpointId);
        
        if (checkpoint == null) {
            try {
                checkpoint = loadFromFile(checkpointId);
            } catch (Exception e) {
                return null;
            }
        }

        if (checkpoint == null) {
            return null;
        }

        return new CheckpointMetadata(
            checkpoint.checkpointId(),
            checkpoint.runId(),
            checkpoint.createdAt(),
            checkpoint.status(),
            estimateStateSize(checkpoint.state())
        );
    }

    /**
     * Cleanup expired checkpoints.
     */
    public void cleanupExpiredCheckpoints() {
        LOG.info("Cleaning up expired checkpoints");
        
        Instant cutoff = Instant.now().minusSeconds(CHECKPOINT_TTL_HOURS * 3600);
        AtomicInteger deleted = new AtomicInteger();

        for (Map.Entry<String, CheckpointData> entry : memoryCache.entrySet()) {
            if (entry.getValue().createdAt().isBefore(cutoff)) {
                memoryCache.remove(entry.getKey());
                deleted.incrementAndGet();
            }
        }

        // Cleanup file system
        try (var stream = Files.list(checkpointDir)) {
            stream.filter(path -> {
                try {
                    return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                } catch (IOException e) {
                    return false;
                }
            }).forEach(path -> {
                try {
                    Files.delete(path);
                    deleted.incrementAndGet();
                } catch (IOException e) {
                    LOG.warnf("Failed to delete expired checkpoint: %s", path);
                }
            });
        } catch (IOException e) {
            LOG.warnf("Failed to cleanup expired checkpoints: %s", e.getMessage());
        }

        LOG.infof("Cleaned up %d expired checkpoints", deleted.get());
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Internal Methods
    // ═══════════════════════════════════════════════════════════════════════

    private void saveToFile(CheckpointData checkpoint) throws IOException {
        String filename = checkpoint.runId() + "-" + checkpoint.checkpointId() + ".json";
        Path file = checkpointDir.resolve(filename);
        
        String json = objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(checkpoint);
        
        Files.writeString(file, json);
        LOG.debugf("Checkpoint saved to file: %s", file);
    }

    private CheckpointData loadFromFile(String checkpointId) throws IOException {
        // Try to find the file
        try (var stream = Files.list(checkpointDir)) {
            Optional<Path> fileOpt = stream
                .filter(path -> path.getFileName().toString().endsWith("-" + checkpointId + ".json"))
                .findFirst();
            
            if (fileOpt.isPresent()) {
                String json = Files.readString(fileOpt.get());
                return objectMapper.readValue(json, CheckpointData.class);
            }
        }
        return null;
    }

    private void cleanupOldCheckpoints(String runId) {
        List<String> checkpoints = listCheckpoints(runId);
        
        if (checkpoints.size() > MAX_CHECKPOINTS_PER_RUN) {
            // Keep only the most recent checkpoints
            checkpoints.stream()
                .limit(checkpoints.size() - MAX_CHECKPOINTS_PER_RUN)
                .forEach(this::deleteCheckpoint);
            
            LOG.debugf("Cleaned up old checkpoints for run %s, kept %d most recent",
                runId, MAX_CHECKPOINTS_PER_RUN);
        }
    }

    private int estimateStateSize(AgentState state) {
        // Rough estimate based on history size
        return state.getHistory().size() * 1000; // ~1KB per step
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Records
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Checkpoint data structure for serialization.
     */
    public record CheckpointData(
        String runId,
        String checkpointId,
        AgentState state,
        Instant createdAt,
        String status
    ) {}

    /**
     * Checkpoint metadata for listing and management.
     */
    public record CheckpointMetadata(
        String checkpointId,
        String runId,
        Instant createdAt,
        String status,
        int estimatedStateSize
    ) {}

    /**
     * Exception thrown when a checkpoint is not found.
     */
    public static class CheckpointNotFoundException extends RuntimeException {
        public CheckpointNotFoundException(String message) {
            super(message);
        }
    }
}
