package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keeps Hermes learned-skill lineage metadata scalar and portable across file,
 * database, and object-storage persistence backends.
 */
final class HermesSkillRevisionMetadata {

    static final String INITIAL_STRATEGY = "initial-distillation";
    static final String REFINEMENT_STRATEGY = "metadata-overlay-tool-union-prompt-append";

    private HermesSkillRevisionMetadata() {
    }

    static Map<String, Object> initial(
            String skillId,
            HermesLearningSignal signal,
            Map<String, Object> baseMetadata) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (baseMetadata != null) {
            metadata.putAll(baseMetadata);
        }
        String requestId = clean(signal.requestId());
        metadata.put("hermes.revision", "1");
        metadata.put("hermes.revisionStatus", "initial");
        metadata.put("hermes.lineageRootSkillId", clean(skillId));
        metadata.put("hermes.lineageDepth", "1");
        metadata.put("hermes.createdRequestId", requestId);
        metadata.put("hermes.latestRequestId", requestId);
        metadata.put("hermes.sourceRequestIds", requestId);
        metadata.put("hermes.mergeStrategy", INITIAL_STRATEGY);
        metadata.put("hermes.mergeReason", "new learned skill");
        metadata.put("hermes.previousRevision", "");
        metadata.put("hermes.supersedesRevision", "");
        metadata.put("hermes.supersedesSkillId", "");
        metadata.put("hermes.derivedFromSkillId", "");
        metadata.put("hermes.lastCandidateSkillId", clean(skillId));
        return metadata;
    }

    static Map<String, Object> refined(
            SkillDefinition existing,
            SkillDefinition candidate,
            HermesLearningSignal signal,
            String mergeReason) {
        Map<String, Object> metadata = new LinkedHashMap<>(existing.metadata());
        metadata.putAll(candidate.metadata());

        String previousRevision = revision(existing.metadata().get("hermes.revision"));
        String requestId = clean(signal.requestId());
        metadata.put("hermes.refinedAt", DateTimeFormatter.ISO_INSTANT.format(signal.observedAt()));
        metadata.put("hermes.refinementRequestId", requestId);
        metadata.put("hermes.revision", nextRevision(previousRevision));
        metadata.put("hermes.previousRevision", previousRevision);
        metadata.put("hermes.supersedesRevision", previousRevision);
        metadata.put("hermes.revisionStatus", "refined");
        metadata.put("hermes.lineageRootSkillId", lineageRootSkillId(existing));
        metadata.put("hermes.lineageDepth", nextLineageDepth(existing));
        metadata.put("hermes.createdRequestId", createdRequestId(existing, candidate, requestId));
        metadata.put("hermes.latestRequestId", requestId);
        metadata.put("hermes.sourceRequestIds", sourceRequestIds(existing, candidate, requestId));
        metadata.put("hermes.mergeStrategy", REFINEMENT_STRATEGY);
        metadata.put("hermes.mergeReason", HermesText.oneLine(mergeReason));
        metadata.put("hermes.supersedesSkillId", existing.id());
        metadata.put("hermes.derivedFromSkillId", candidate.id());
        metadata.put("hermes.lastCandidateSkillId", candidate.id());
        return metadata;
    }

    private static String lineageRootSkillId(SkillDefinition existing) {
        String rootSkillId = metadataText(existing.metadata(), "hermes.lineageRootSkillId");
        return rootSkillId.isBlank() ? existing.id() : rootSkillId;
    }

    private static String createdRequestId(
            SkillDefinition existing,
            SkillDefinition candidate,
            String requestId) {
        String createdRequestId = metadataText(existing.metadata(), "hermes.createdRequestId");
        if (createdRequestId.isBlank()) {
            createdRequestId = metadataText(existing.metadata(), "hermes.requestId");
        }
        if (createdRequestId.isBlank()) {
            createdRequestId = metadataText(candidate.metadata(), "hermes.createdRequestId");
        }
        return createdRequestId.isBlank() ? requestId : createdRequestId;
    }

    private static String sourceRequestIds(
            SkillDefinition existing,
            SkillDefinition candidate,
            String requestId) {
        Set<String> requestIds = new LinkedHashSet<>();
        addCsv(requestIds, metadataText(existing.metadata(), "hermes.sourceRequestIds"));
        addValue(requestIds, metadataText(existing.metadata(), "hermes.createdRequestId"));
        addValue(requestIds, metadataText(existing.metadata(), "hermes.requestId"));
        addCsv(requestIds, metadataText(candidate.metadata(), "hermes.sourceRequestIds"));
        addValue(requestIds, metadataText(candidate.metadata(), "hermes.requestId"));
        addValue(requestIds, requestId);
        return String.join(",", requestIds);
    }

    private static String nextLineageDepth(SkillDefinition existing) {
        int depth = intValue(existing.metadata().get("hermes.lineageDepth"), 1);
        return String.valueOf(depth + 1);
    }

    private static String nextRevision(String revision) {
        return String.valueOf(intValue(revision, 1) + 1);
    }

    private static String revision(Object revision) {
        int value = intValue(revision, 1);
        return String.valueOf(value);
    }

    private static int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static void addCsv(Set<String> values, String csv) {
        if (csv == null || csv.isBlank()) {
            return;
        }
        for (String value : csv.split(",")) {
            addValue(values, value);
        }
    }

    private static void addValue(Set<String> values, String value) {
        String clean = clean(value);
        if (!clean.isBlank()) {
            values.add(clean);
        }
    }

    private static String metadataText(Map<String, Object> metadata, String key) {
        Object value = metadata == null ? null : metadata.get(key);
        return value == null ? "" : HermesText.oneLine(value.toString());
    }

    private static String clean(String value) {
        return HermesText.oneLine(value);
    }
}
