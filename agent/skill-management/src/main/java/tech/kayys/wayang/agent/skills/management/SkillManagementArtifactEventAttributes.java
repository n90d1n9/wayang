package tech.kayys.wayang.agent.skills.management;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.agent.skills.management.SkillManagementEventAttributeKeys.*;

/**
 * Event-attribute projection for artifact synchronization and mutations.
 */
final class SkillManagementArtifactEventAttributes {

    private SkillManagementArtifactEventAttributes() {
    }

    static Map<String, String> sync(SkillArtifactStoreSyncResult result) {
        Objects.requireNonNull(result, "result");
        return Map.of(
                DRY_RUN, String.valueOf(result.dryRun()),
                CHANGED, String.valueOf(result.changed()),
                CONSISTENT, String.valueOf(result.conflicts() == 0),
                COPIED, String.valueOf(result.copied()),
                UPDATED, String.valueOf(result.updated()),
                UNCHANGED, String.valueOf(result.unchanged()),
                CONFLICTS, String.valueOf(result.conflicts()),
                DELETED, String.valueOf(result.deleted()));
    }

    static Map<String, String> artifact(SkillArtifact artifact) {
        Objects.requireNonNull(artifact, "artifact");
        return reference(artifact.reference(), Map.of(
                CONTENT_TYPE, artifact.contentType(),
                SIZE_BYTES, String.valueOf(artifact.sizeBytes())));
    }

    static Map<String, String> reference(SkillArtifactReference reference) {
        return reference(reference, Map.of());
    }

    static Map<String, String> deleted(
            SkillArtifactReference reference,
            boolean deleted) {
        return reference(reference, Map.of(DELETED, String.valueOf(deleted)));
    }

    static Map<String, String> reference(
            SkillArtifactReference reference,
            Map<String, String> extra) {
        Objects.requireNonNull(reference, "reference");
        LinkedHashMap<String, String> attributes = new LinkedHashMap<>();
        attributes.put(KIND, reference.kind().label());
        attributes.put(NAME, reference.name());
        attributes.put(VERSION, reference.version());
        attributes.put(QUALIFIED_NAME, reference.qualifiedName());
        if (extra != null) {
            attributes.putAll(extra);
        }
        return Map.copyOf(attributes);
    }
}
