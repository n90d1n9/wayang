package tech.kayys.wayang.skills.store;

import tech.kayys.wayang.skill.spi.SkillDefinition;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * SPI for enterprise/DB skill backends.
 *
 * <p>Implementations are discovered via {@link java.util.ServiceLoader} at
 * runtime. The community edition uses only {@link WayangSkillStore} (file-based).
 * The pro/enterprise edition can register a {@code DatabaseSkillStoreBackend}
 * implementation that transparently syncs with PostgreSQL or another DB.
 *
 * <p>When a backend is present, {@link WayangSkillStore} will:
 * <ol>
 *   <li>Call {@link #loadAll()} at startup and merge results into the unified index.</li>
 *   <li>Call {@link #save(SkillDefinition)} on every create/update.</li>
 *   <li>Call {@link #delete(String)} on every delete.</li>
 * </ol>
 *
 * <p>This ensures that both the local SKILL.md files and the DB are always in
 * sync, with the DB taking precedence for the same skill id.
 */
public interface SkillStoreBackend {

    /**
     * A human-readable name for this backend (e.g. "PostgreSQL", "SQLite").
     */
    String name();

    /**
     * Load all skills from the backend.
     *
     * @return future of list of skill entries (never null, may be empty)
     */
    CompletionStage<List<SkillEntry>> loadAll();

    /**
     * Persist a skill to the backend.
     *
     * @param definition the skill to save
     * @return future that completes when saved
     */
    CompletionStage<Void> save(SkillDefinition definition);

    /**
     * Delete a skill from the backend.
     *
     * @param id the skill id
     * @return future that completes when deleted
     */
    CompletionStage<Void> delete(String id);

    /**
     * Check if a skill exists in the backend.
     *
     * @param id the skill id
     * @return future of true if exists
     */
    default CompletionStage<Boolean> exists(String id) {
        return loadAll().thenApply(list -> list.stream().anyMatch(e -> e.id().equals(id)));
    }
}
