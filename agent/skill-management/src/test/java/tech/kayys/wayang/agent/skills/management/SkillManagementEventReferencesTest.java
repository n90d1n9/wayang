package tech.kayys.wayang.agent.skills.management;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillManagementEventReferencesTest {

    @Test
    void createsSortableStorageReferences() {
        Instant occurredAt = Instant.parse("2026-01-01T00:00:00Z");
        SkillManagementEvent event = new SkillManagementEvent(
                occurredAt,
                SkillManagementEventOperation.CREATE_SKILL,
                "planner",
                true,
                Map.of());

        String reference = SkillManagementEventReferences.storageReference(event);

        assertThat(reference)
                .startsWith(String.format(Locale.ROOT, "%020d-", occurredAt.toEpochMilli()))
                .endsWith(SkillManagementEventReferences.STORAGE_EXTENSION)
                .matches("\\d{20}-[0-9a-f\\-]{36}\\.event\\.properties");
    }

    @Test
    void createsSortableEventReferencesWithoutStorageExtension() {
        Instant occurredAt = Instant.parse("2026-01-01T00:00:00Z");
        SkillManagementEvent event = new SkillManagementEvent(
                occurredAt,
                SkillManagementEventOperation.CREATE_SKILL,
                "planner",
                true,
                Map.of());

        String reference = SkillManagementEventReferences.sortableReference(event);

        assertThat(reference)
                .startsWith(String.format(Locale.ROOT, "%020d-", occurredAt.toEpochMilli()))
                .matches("\\d{20}-[0-9a-f\\-]{36}");
        assertThat(SkillManagementEventReferences.hasStorageExtension(reference)).isFalse();
    }

    @Test
    void detectsStorageReferences() {
        assertThat(SkillManagementEventReferences.hasStorageExtension("a.event.properties")).isTrue();
        assertThat(SkillManagementEventReferences.hasStorageExtension("a.properties")).isFalse();
        assertThat(SkillManagementEventReferences.hasStorageExtension(null)).isFalse();
    }
}
