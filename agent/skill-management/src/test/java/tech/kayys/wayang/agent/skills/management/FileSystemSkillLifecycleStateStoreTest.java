package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemSkillLifecycleStateStoreTest {

    @Test
    void persistsLifecycleStateAcrossInstances(@TempDir Path tempDir) {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-02T00:00:00Z");
        SkillLifecycleState state = new SkillLifecycleState(
                "planner",
                SkillLifecycleStatus.DISABLED,
                createdAt,
                updatedAt,
                7);

        new FileSystemSkillLifecycleStateStore(tempDir).save(state);

        FileSystemSkillLifecycleStateStore reloaded = new FileSystemSkillLifecycleStateStore(tempDir);
        assertThat(reloaded.get("planner")).contains(state);
        assertThat(reloaded.snapshot()).containsEntry("planner", state);
    }

    @Test
    void removesPersistedLifecycleState(@TempDir Path tempDir) {
        FileSystemSkillLifecycleStateStore store = new FileSystemSkillLifecycleStateStore(tempDir);
        store.save(SkillLifecycleState.created("planner"));

        assertThat(store.remove("planner")).isTrue();

        assertThat(new FileSystemSkillLifecycleStateStore(tempDir).get("planner")).isEmpty();
    }
}
