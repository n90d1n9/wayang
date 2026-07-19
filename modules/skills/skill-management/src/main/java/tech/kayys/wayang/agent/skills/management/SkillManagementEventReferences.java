package tech.kayys.wayang.agent.skills.management;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Shared event-history reference generation for persistent event stores.
 */
final class SkillManagementEventReferences {

    static final String STORAGE_EXTENSION = ".event.properties";

    private SkillManagementEventReferences() {
    }

    static String storageReference(SkillManagementEvent event) {
        return sortableReference(event) + STORAGE_EXTENSION;
    }

    static boolean hasStorageExtension(String reference) {
        return reference != null && reference.endsWith(STORAGE_EXTENSION);
    }

    static String sortableReference(SkillManagementEvent event) {
        Objects.requireNonNull(event, "event");
        return sortableReference(event.occurredAt());
    }

    private static String sortableReference(Instant occurredAt) {
        return String.format(
                Locale.ROOT,
                "%020d-%s",
                Objects.requireNonNull(occurredAt, "occurredAt").toEpochMilli(),
                UUID.randomUUID());
    }
}
