package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FileSystemSkillManagementEventStoreTest {

    @Test
    void persistsAndQueriesEventsAcrossStoreInstances(@TempDir Path tempDir) {
        FileSystemSkillManagementEventStore writer = new FileSystemSkillManagementEventStore(tempDir);
        writer.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        writer.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        FileSystemSkillManagementEventStore reader = new FileSystemSkillManagementEventStore(tempDir);
        SkillManagementEventPage failures = reader.query(SkillManagementEventQuery.failures(10));

        assertThat(reader.events()).hasSize(2);
        assertThat(failures.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.DELETE_SKILL);
        assertThat(failures.events().get(0).attributes()).containsEntry("status", "DELETE_SKILL");
        assertThat(failures.summary().failedEvents()).isEqualTo(1);
    }

    @Test
    void prunesOldestFilesWhenRetentionIsExceeded(@TempDir Path tempDir) throws Exception {
        FileSystemSkillManagementEventStore store = new FileSystemSkillManagementEventStore(tempDir, 2);

        store.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.UPDATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:02Z", SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        assertThat(store.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(
                        SkillManagementEventOperation.UPDATE_SKILL,
                        SkillManagementEventOperation.DELETE_SKILL);
        try (var files = Files.list(tempDir)) {
            assertThat(files.filter(Files::isRegularFile)).hasSize(2);
        }
    }

    @Test
    void prunesOldestFilesOnDemandWithDryRun(@TempDir Path tempDir) {
        FileSystemSkillManagementEventStore store = new FileSystemSkillManagementEventStore(tempDir, 10);
        store.record(event("2026-01-01T00:00:00Z", SkillManagementEventOperation.CREATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:01Z", SkillManagementEventOperation.UPDATE_SKILL, "planner", true));
        store.record(event("2026-01-01T00:00:02Z", SkillManagementEventOperation.DELETE_SKILL, "planner", false));

        SkillManagementEventPruneResult preview =
                store.pruneEvents(SkillManagementEventPruneOptions.dryRun(1));

        assertThat(preview.prunedEvents()).isEqualTo(2);
        assertThat(store.events()).hasSize(3);

        SkillManagementEventPruneResult result =
                store.pruneEvents(SkillManagementEventPruneOptions.keepLatest(1));

        assertThat(result.changed()).isTrue();
        assertThat(result.prunedEventReferences()).hasSize(2);
        assertThat(store.events()).extracting(SkillManagementEvent::operation)
                .containsExactly(SkillManagementEventOperation.DELETE_SKILL);
    }

    @Test
    void ignoresNullEvents(@TempDir Path tempDir) {
        FileSystemSkillManagementEventStore store = new FileSystemSkillManagementEventStore(tempDir);

        store.record(null);

        assertThat(store.events()).isEmpty();
        assertThat(store.latest().events()).isEmpty();
    }

    private SkillManagementEvent event(
            String occurredAt,
            SkillManagementEventOperation operation,
            String skillId,
            boolean success) {
        return new SkillManagementEvent(
                Instant.parse(occurredAt),
                operation,
                skillId,
                success,
                Map.of("status", operation.name()));
    }
}
