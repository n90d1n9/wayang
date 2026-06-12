package tech.kayys.gollek.agent.skills.management;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.gollek.agent.skills.repo.spi.*;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Comprehensive skill management service with CRUD operations and seamless repository switching.
 *
 * <p>Features:
 * <ul>
 *   <li>CRUD operations (Create, Read, Update, Delete)</li>
 *   <li>Seamless repository switching (file ↔ database)</li>
 *   <li>Skill validation and versioning</li>
 *   <li>Batch operations</li>
 *   <li>Skill lifecycle management</li>
 *   <li>Import/Export capabilities</li>
 *   <li>Search and filtering</li>
 * </ul>
 */
@ApplicationScoped
public class SkillManagementService {

    private static final Logger log = LoggerFactory.getLogger(SkillManagementService.class);

    @Inject
    SkillRepository fileRepository;

    @Inject
    SkillRepository databaseRepository;

    private volatile SkillRepository activeRepository;
    private final Map<String, SkillLock> skillLocks;

    /**
     * Default constructor.
     */
    public SkillManagementService() {
        this.skillLocks = new ConcurrentHashMap<>();
        this.activeRepository = null; // Will be set by initialize()
    }

    /**
     * Initialize the service with default repository.
     *
     * @param useDatabase if true, use database; otherwise use file repository
     * @return Uni with success status
     */
    public Uni<Void> initialize(boolean useDatabase) {
        return Uni.createFrom().voidItem().invoke(() -> {
            activeRepository = useDatabase ? databaseRepository : fileRepository;
            log.info("SkillManagementService initialized with {} repository",
                    useDatabase ? "database" : "file");
        });
    }

    /**
     * Get the active repository name.
     *
     * @return repository name
     */
    public String getActiveRepositoryName() {
        return activeRepository != null ? activeRepository.name() : "none";
    }

    // ==================== Repository Switching ====================

    /**
     * Switch to a different repository.
     *
     * @param repositoryName "file" or "database"
     * @return Uni with success status
     */
    public Uni<Void> switchRepository(String repositoryName) {
        return Uni.createFrom().voidItem().invoke(() -> {
            SkillRepository newRepo = switch (repositoryName.toLowerCase()) {
                case "file" -> fileRepository;
                case "database" -> databaseRepository;
                default -> throw new IllegalArgumentException("Unknown repository: " + repositoryName);
            };

            if (newRepo != activeRepository) {
                log.info("Switching repository from {} to {}", 
                        activeRepository != null ? activeRepository.name() : "none",
                        repositoryName);
                activeRepository = newRepo;
            }
        });
    }

    /**
     * Migrate all skills from one repository to another.
     *
     * @param fromRepo source repository name
     * @param toRepo target repository name
     * @return Uni with migration stats
     */
    public Uni<MigrationStats> migrateRepository(String fromRepo, String toRepo) {
        return Uni.createFrom().item(() -> {
            SkillRepository source = getRepository(fromRepo);
            SkillRepository target = getRepository(toRepo);

            int migrated = 0;
            int failed = 0;
            int skipped = 0;

            // Get all skills from source
            List<SkillMetadata> skills = source.list().await().indefinitely();

            for (SkillMetadata metadata : skills) {
                try {
                    // Check if already exists in target
                    if (target.exists(metadata.id()).await().indefinitely()) {
                        log.debug("Skill {} already exists in target, skipping", metadata.id());
                        skipped++;
                        continue;
                    }

                    // Get full content
                    Optional<SkillContent> content = source.get(metadata.id())
                            .await().indefinitely();

                    if (content.isPresent()) {
                        // Save to target
                        target.save(content.get()).await().indefinitely();
                        migrated++;
                        log.debug("Migrated skill: {}", metadata.id());
                    } else {
                        failed++;
                        log.warn("Failed to get content for skill: {}", metadata.id());
                    }

                } catch (Exception e) {
                    failed++;
                    log.error("Failed to migrate skill: {}", metadata.id(), e);
                }
            }

            return new MigrationStats(migrated, failed, skipped, skills.size());
        });
    }

    /**
     * Sync skills between repositories.
     *
     * @param repo1 first repository name
     * @param repo2 second repository name
     * @return Uni with sync stats
     */
    public Uni<SyncStats> syncRepositories(String repo1, String repo2) {
        return Uni.createFrom().item(() -> {
            SkillRepository source = getRepository(repo1);
            SkillRepository target = getRepository(repo2);

            int added = 0;
            int updated = 0;
            int deleted = 0;

            // Get all from both
            List<SkillMetadata> sourceSkills = source.list().await().indefinitely();
            List<SkillMetadata> targetSkills = target.list().await().indefinitely();

            Set<String> sourceIds = sourceSkills.stream()
                    .map(SkillMetadata::id)
                    .collect(Collectors.toSet());
            Set<String> targetIds = targetSkills.stream()
                    .map(SkillMetadata::id)
                    .collect(Collectors.toSet());

            // Add missing to target
            for (String id : sourceIds) {
                if (!targetIds.contains(id)) {
                    source.get(id).await().indefinitely()
                            .ifPresent(content -> {
                                target.save(content).await().indefinitely();
                                added++;
                            });
                }
            }

            // Update modified (by checksum comparison)
            for (String id : sourceIds) {
                if (targetIds.contains(id)) {
                    Optional<SkillMetadata> sourceMeta = source.getMetadata(id).await().indefinitely();
                    Optional<SkillMetadata> targetMeta = target.getMetadata(id).await().indefinitely();

                    if (sourceMeta.isPresent() && targetMeta.isPresent()) {
                        if (!Objects.equals(sourceMeta.get().checksum(), targetMeta.get().checksum())) {
                            source.get(id).await().indefinitely()
                                    .ifPresent(content -> {
                                        target.save(content).await().indefinitely();
                                        updated++;
                                    });
                        }
                    }
                }
            }

            return new SyncStats(added, updated, deleted, sourceSkills.size(), targetSkills.size());
        });
    }

    // ==================== CRUD Operations ====================

    /**
     * Create a new skill.
     *
     * @param content skill content
     * @return Uni with created skill metadata
     */
    public Uni<SkillMetadata> createSkill(SkillContent content) {
        return Uni.createFrom().item(() -> {
            // Validate
            validateSkillContent(content);

            // Check if exists
            if (activeRepository.exists(content.metadata().id()).await().indefinitely()) {
                throw new SkillAlreadyExistsException(content.metadata().id());
            }

            // Acquire lock
            acquireLock(content.metadata().id());
            try {
                return activeRepository.save(content).await().indefinitely();
            } finally {
                releaseLock(content.metadata().id());
            }
        });
    }

    /**
     * Get a skill by ID.
     *
     * @param skillId skill identifier
     * @return Uni with optional skill content
     */
    public Uni<Optional<SkillContent>> getSkill(String skillId) {
        return activeRepository.get(skillId);
    }

    /**
     * Update an existing skill.
     *
     * @param skillId skill identifier
     * @param content new skill content
     * @return Uni with updated skill metadata
     */
    public Uni<SkillMetadata> updateSkill(String skillId, SkillContent content) {
        return Uni.createFrom().item(() -> {
            // Validate
            validateSkillContent(content);

            // Check if exists
            if (!activeRepository.exists(skillId).await().indefinitely()) {
                throw new SkillNotFoundException(skillId);
            }

            // Ensure ID matches
            if (!skillId.equals(content.metadata().id())) {
                throw new IllegalArgumentException("Skill ID mismatch");
            }

            // Acquire lock
            acquireLock(skillId);
            try {
                // Update timestamp
                SkillMetadata updatedMetadata = new SkillMetadata.Builder()
                        .from(content.metadata())
                        .updatedAt(Instant.now())
                        .build();

                SkillContent updatedContent = new SkillContent.Builder()
                        .from(content)
                        .metadata(updatedMetadata)
                        .build();

                return activeRepository.save(updatedContent).await().indefinitely();
            } finally {
                releaseLock(skillId);
            }
        });
    }

    /**
     * Delete a skill.
     *
     * @param skillId skill identifier
     * @return Uni with success status
     */
    public Uni<Boolean> deleteSkill(String skillId) {
        return activeRepository.delete(skillId);
    }

    /**
     * Check if a skill exists.
     *
     * @param skillId skill identifier
     * @return Uni with existence status
     */
    public Uni<Boolean> skillExists(String skillId) {
        return activeRepository.exists(skillId);
    }

    // ==================== Batch Operations ====================

    /**
     * Create multiple skills.
     *
     * @param contents list of skill contents
     * @return Uni with batch result
     */
    public Uni<BatchResult> createSkills(List<SkillContent> contents) {
        return Uni.createFrom().item(() -> {
            int created = 0;
            int failed = 0;
            List<String> errors = new ArrayList<>();

            for (SkillContent content : contents) {
                try {
                    createSkill(content).await().indefinitely();
                    created++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Failed to create " + content.metadata().id() + ": " + e.getMessage());
                }
            }

            return new BatchResult(created, failed, errors);
        });
    }

    /**
     * Delete multiple skills.
     *
     * @param skillIds list of skill IDs
     * @return Uni with batch result
     */
    public Uni<BatchResult> deleteSkills(List<String> skillIds) {
        return Uni.createFrom().item(() -> {
            int deleted = 0;
            int failed = 0;
            List<String> errors = new ArrayList<>();

            for (String id : skillIds) {
                try {
                    if (deleteSkill(id).await().indefinitely()) {
                        deleted++;
                    } else {
                        failed++;
                        errors.add("Skill not found: " + id);
                    }
                } catch (Exception e) {
                    failed++;
                    errors.add("Failed to delete " + id + ": " + e.getMessage());
                }
            }

            return new BatchResult(deleted, failed, errors);
        });
    }

    // ==================== Search and Listing ====================

    /**
     * List all skills.
     *
     * @return Uni with list of skill metadata
     */
    public Uni<List<SkillMetadata>> listSkills() {
        return activeRepository.list();
    }

    /**
     * List skills with pagination.
     *
     * @param page page number (0-based)
     * @param size page size
     * @return Uni with paginated result
     */
    public Uni<PaginatedResult<SkillMetadata>> listSkills(int page, int size) {
        return Uni.createFrom().item(() -> {
            List<SkillMetadata> all = activeRepository.list().await().indefinitely();
            int total = all.size();
            int totalPages = (int) Math.ceil((double) total / size);

            int fromIndex = page * size;
            int toIndex = Math.min(fromIndex + size, total);

            List<SkillMetadata> pageContent = all.subList(fromIndex, toIndex);

            return new PaginatedResult<>(pageContent, page, size, total, totalPages);
        });
    }

    /**
     * Search skills.
     *
     * @param query search query
     * @param page page number
     * @param size page size
     * @return Uni with paginated result
     */
    public Uni<PaginatedResult<SkillMetadata>> searchSkills(String query, int page, int size) {
        return Uni.createFrom().item(() -> {
            List<SkillMetadata> results = activeRepository.search(query, page * size, size)
                    .await().indefinitely();

            int total = activeRepository.search(query, 0, 10000).await().indefinitely().size();
            int totalPages = (int) Math.ceil((double) total / size);

            return new PaginatedResult<>(results, page, size, total, totalPages);
        });
    }

    /**
     * Get skills by category.
     *
     * @param category category name
     * @return Uni with list of skill metadata
     */
    public Uni<List<SkillMetadata>> getSkillsByCategory(String category) {
        return activeRepository.getByCategory(category);
    }

    /**
     * Get skills by tags.
     *
     * @param tags tags to filter by
     * @param matchAll if true, must have all tags
     * @return Uni with list of skill metadata
     */
    public Uni<List<SkillMetadata>> getSkillsByTags(List<String> tags, boolean matchAll) {
        return activeRepository.getByTags(tags, matchAll);
    }

    // ==================== Lifecycle Management ====================

    /**
     * Enable a skill.
     *
     * @param skillId skill identifier
     * @return Uni with success status
     */
    public Uni<Boolean> enableSkill(String skillId) {
        return activeRepository.setEnabled(skillId, true);
    }

    /**
     * Disable a skill.
     *
     * @param skillId skill identifier
     * @return Uni with success status
     */
    public Uni<Boolean> disableSkill(String skillId) {
        return activeRepository.setEnabled(skillId, false);
    }

    /**
     * Get skill statistics.
     *
     * @return Uni with repository stats
     */
    public Uni<SkillRepository.RepositoryStats> getStats() {
        return activeRepository.getStats();
    }

    /**
     * Clear repository cache.
     *
     * @return Uni with success status
     */
    public Uni<Void> clearCache() {
        return activeRepository.clearCache();
    }

    // ==================== Import/Export ====================

    /**
     * Export a skill to JSON string.
     *
     * @param skillId skill identifier
     * @return Uni with JSON string
     */
    public Uni<String> exportSkill(String skillId) {
        return getSkill(skillId)
                .onItem().transform(content -> {
                    if (content.isEmpty()) {
                        throw new SkillNotFoundException(skillId);
                    }
                    // Convert to JSON (simplified)
                    return String.format(
                        "{\"id\":\"%s\",\"name\":\"%s\",\"version\":\"%s\",\"content\":\"%s\"}",
                        content.get().metadata().id(),
                        content.get().metadata().name(),
                        content.get().metadata().version(),
                        content.get().content().replace("\"", "\\\"")
                    );
                });
    }

    /**
     * Import a skill from JSON string.
     *
     * @param json JSON string
     * @return Uni with created skill metadata
     */
    public Uni<SkillMetadata> importSkill(String json) {
        return Uni.createFrom().item(() -> {
            // Parse JSON (simplified - in production use proper parser)
            // This is a placeholder for actual JSON parsing logic
            SkillContent content = parseSkillFromJson(json);
            return createSkill(content).await().indefinitely();
        });
    }

    // ==================== Private Helpers ====================

    private SkillRepository getRepository(String name) {
        return switch (name.toLowerCase()) {
            case "file" -> fileRepository;
            case "database" -> databaseRepository;
            default -> throw new IllegalArgumentException("Unknown repository: " + name);
        };
    }

    private void validateSkillContent(SkillContent content) {
        if (content == null) {
            throw new IllegalArgumentException("Skill content cannot be null");
        }
        if (content.metadata() == null) {
            throw new IllegalArgumentException("Skill metadata cannot be null");
        }
        if (content.metadata().id() == null || content.metadata().id().isBlank()) {
            throw new IllegalArgumentException("Skill ID is required");
        }
        if (content.metadata().name() == null || content.metadata().name().isBlank()) {
            throw new IllegalArgumentException("Skill name is required");
        }
    }

    private void acquireLock(String skillId) {
        skillLocks.putIfAbsent(skillId, new SkillLock());
        skillLocks.get(skillId).acquire();
    }

    private void releaseLock(String skillId) {
        SkillLock lock = skillLocks.get(skillId);
        if (lock != null) {
            lock.release();
        }
    }

    private SkillContent parseSkillFromJson(String json) {
        // Placeholder - in production use Jackson to parse JSON
        throw new UnsupportedOperationException("JSON parsing not implemented in this example");
    }

    // ==================== Record Classes ====================

    /**
     * Migration statistics.
     */
    public record MigrationStats(
        int migrated,
        int failed,
        int skipped,
        int total
    ) {}

    /**
     * Sync statistics.
     */
    public record SyncStats(
        int added,
        int updated,
        int deleted,
        int sourceCount,
        int targetCount
    ) {}

    /**
     * Batch operation result.
     */
    public record BatchResult(
        int success,
        int failed,
        List<String> errors
    ) {}

    /**
     * Paginated result.
     */
    public record PaginatedResult<T>(
        List<T> content,
        int page,
        int size,
        int total,
        int totalPages
    ) {}

    /**
     * Simple lock implementation.
     */
    private static class SkillLock {
        private volatile boolean locked = false;

        public synchronized void acquire() {
            while (locked) {
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            locked = true;
        }

        public synchronized void release() {
            locked = false;
            notifyAll();
        }
    }

    // ==================== Exception Classes ====================

    /**
     * Skill already exists exception.
     */
    public static class SkillAlreadyExistsException extends RuntimeException {
        public SkillAlreadyExistsException(String skillId) {
            super("Skill already exists: " + skillId);
        }
    }

    /**
     * Skill not found exception.
     */
    public static class SkillNotFoundException extends RuntimeException {
        public SkillNotFoundException(String skillId) {
            super("Skill not found: " + skillId);
        }
    }
}
