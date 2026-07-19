package tech.kayys.gollek.agent.skills.repo.spi;

import io.smallrye.mutiny.Uni;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Skill Repository SPI - provides storage and retrieval of skills.
 *
 * <p>Implementations can use different storage backends:
 * <ul>
 *   <li>File-based storage (~/.gollek/skills/)</li>
 *   <li>Database storage (PostgreSQL, etc.)</li>
 *   <li>Hybrid approaches</li>
 * </ul>
 *
 * <p>All implementations should provide caching for better performance.
 */
public interface SkillRepository {

    /**
     * Get repository name/identifier.
     * @return repository name
     */
    String name();

    /**
     * Save a skill to the repository.
     *
     * @param content skill content to save
     * @return Uni with saved skill metadata
     */
    Uni<SkillMetadata> save(SkillContent content);

    /**
     * Get a skill by ID.
     *
     * @param skillId skill identifier
     * @return Uni with optional skill content
     */
    Uni<Optional<SkillContent>> get(String skillId);

    /**
     * Get skill metadata by ID (without content).
     *
     * @param skillId skill identifier
     * @return Uni with optional skill metadata
     */
    Uni<Optional<SkillMetadata>> getMetadata(String skillId);

    /**
     * Delete a skill from the repository.
     *
     * @param skillId skill identifier
     * @return Uni with success status
     */
    Uni<Boolean> delete(String skillId);

    /**
     * List all skills with pagination.
     *
     * @param offset number of skills to skip
     * @param limit maximum number of skills to return
     * @return Uni with list of skill metadata
     */
    Uni<List<SkillMetadata>> list(int offset, int limit);

    /**
     * List all skills (no pagination).
     *
     * @return Uni with list of skill metadata
     */
    default Uni<List<SkillMetadata>> list() {
        return list(0, 1000);
    }

    /**
     * Search skills by query.
     *
     * @param query search query (matches name, description, tags)
     * @param offset number of results to skip
     * @param limit maximum number of results to return
     * @return Uni with list of matching skill metadata
     */
    Uni<List<SkillMetadata>> search(String query, int offset, int limit);

    /**
     * Search skills by category.
     *
     * @param category category to filter by
     * @return Uni with list of matching skill metadata
     */
    Uni<List<SkillMetadata>> getByCategory(String category);

    /**
     * Search skills by tags.
     *
     * @param tags tags to filter by
     * @param matchAll if true, skill must have all tags; if false, any tag matches
     * @return Uni with list of matching skill metadata
     */
    Uni<List<SkillMetadata>> getByTags(List<String> tags, boolean matchAll);

    /**
     * Check if a skill exists.
     *
     * @param skillId skill identifier
     * @return Uni with existence status
     */
    Uni<Boolean> exists(String skillId);

    /**
     * Enable or disable a skill.
     *
     * @param skillId skill identifier
     * @param enabled enable status
     * @return Uni with success status
     */
    Uni<Boolean> setEnabled(String skillId, boolean enabled);

    /**
     * Get repository statistics.
     *
     * @return Uni with repository stats
     */
    Uni<RepositoryStats> getStats();

    /**
     * Clear repository cache.
     *
     * @return Uni with success status
     */
    Uni<Void> clearCache();

    /**
     * Initialize repository (create directories, tables, etc.).
     *
     * @return Uni with success status
     */
    Uni<Void> initialize();

    /**
     * Repository statistics.
     */
    record RepositoryStats(
        int totalSkills,
        int enabledSkills,
        int disabledSkills,
        long totalSizeBytes,
        int cacheHits,
        int cacheMisses,
        double cacheHitRatio
    ) {}
}
