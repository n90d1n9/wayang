package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Scalar learned-skill revision view suitable for definition stores, files, and
 * object-storage metadata.
 */
public record HermesSkillLineageEntry(
        String skillId,
        String name,
        String task,
        String revision,
        String previousRevision,
        String supersedesRevision,
        String revisionStatus,
        String lineageRootSkillId,
        int lineageDepth,
        String createdRequestId,
        String latestRequestId,
        List<String> sourceRequestIds,
        String mergeStrategy,
        String mergeReason,
        String supersedesSkillId,
        String derivedFromSkillId,
        String lastCandidateSkillId,
        String learnedAt,
        String refinedAt,
        String learningQualityScore,
        String learningQualityThreshold,
        List<String> tools) {

    public HermesSkillLineageEntry {
        skillId = clean(skillId);
        name = clean(name);
        task = clean(task);
        revision = clean(revision).isBlank() ? "1" : clean(revision);
        previousRevision = clean(previousRevision);
        supersedesRevision = clean(supersedesRevision);
        revisionStatus = clean(revisionStatus).isBlank() ? "unknown" : clean(revisionStatus);
        lineageRootSkillId = clean(lineageRootSkillId).isBlank() ? skillId : clean(lineageRootSkillId);
        lineageDepth = Math.max(lineageDepth, 1);
        createdRequestId = clean(createdRequestId);
        latestRequestId = clean(latestRequestId);
        sourceRequestIds = sourceRequestIds == null ? List.of() : List.copyOf(sourceRequestIds.stream()
                .map(HermesSkillLineageEntry::clean)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
        mergeStrategy = clean(mergeStrategy);
        mergeReason = clean(mergeReason);
        supersedesSkillId = clean(supersedesSkillId);
        derivedFromSkillId = clean(derivedFromSkillId);
        lastCandidateSkillId = clean(lastCandidateSkillId);
        learnedAt = clean(learnedAt);
        refinedAt = clean(refinedAt);
        learningQualityScore = clean(learningQualityScore);
        learningQualityThreshold = clean(learningQualityThreshold);
        tools = tools == null ? List.of() : List.copyOf(tools.stream()
                .map(HermesSkillLineageEntry::clean)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
    }

    public static HermesSkillLineageEntry from(SkillDefinition skill) {
        Map<String, Object> metadata = skill == null ? Map.of() : skill.metadata();
        String skillId = skill == null ? "" : skill.id();
        return new HermesSkillLineageEntry(
                skillId,
                skill == null ? "" : skill.name(),
                metadataText(metadata, "hermes.task"),
                metadataText(metadata, "hermes.revision", "1"),
                metadataText(metadata, "hermes.previousRevision"),
                metadataText(metadata, "hermes.supersedesRevision"),
                metadataText(metadata, "hermes.revisionStatus"),
                metadataText(metadata, "hermes.lineageRootSkillId", skillId),
                intValue(metadata.get("hermes.lineageDepth"), 1),
                metadataText(metadata, "hermes.createdRequestId", metadataText(metadata, "hermes.requestId")),
                metadataText(metadata, "hermes.latestRequestId", metadataText(metadata, "hermes.requestId")),
                sourceRequestIds(metadata),
                metadataText(metadata, "hermes.mergeStrategy"),
                metadataText(metadata, "hermes.mergeReason"),
                metadataText(metadata, "hermes.supersedesSkillId"),
                metadataText(metadata, "hermes.derivedFromSkillId"),
                metadataText(metadata, "hermes.lastCandidateSkillId", skillId),
                metadataText(metadata, "hermes.learnedAt"),
                metadataText(metadata, "hermes.refinedAt"),
                metadataText(metadata, "hermes.learningQualityScore"),
                metadataText(metadata, "hermes.learningQualityThreshold"),
                skill == null ? List.of() : skill.tools());
    }

    public int revisionNumber() {
        return intValue(revision, 1);
    }

    public boolean refined() {
        return revisionNumber() > 1 || "refined".equalsIgnoreCase(revisionStatus);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("skillId", skillId);
        metadata.put("name", name);
        metadata.put("task", task);
        metadata.put("revision", revision);
        metadata.put("previousRevision", previousRevision);
        metadata.put("supersedesRevision", supersedesRevision);
        metadata.put("revisionStatus", revisionStatus);
        metadata.put("lineageRootSkillId", lineageRootSkillId);
        metadata.put("lineageDepth", lineageDepth);
        metadata.put("createdRequestId", createdRequestId);
        metadata.put("latestRequestId", latestRequestId);
        metadata.put("sourceRequestIds", sourceRequestIds);
        metadata.put("mergeStrategy", mergeStrategy);
        metadata.put("mergeReason", mergeReason);
        metadata.put("supersedesSkillId", supersedesSkillId);
        metadata.put("derivedFromSkillId", derivedFromSkillId);
        metadata.put("lastCandidateSkillId", lastCandidateSkillId);
        metadata.put("learnedAt", learnedAt);
        metadata.put("refinedAt", refinedAt);
        metadata.put("learningQualityScore", learningQualityScore);
        metadata.put("learningQualityThreshold", learningQualityThreshold);
        metadata.put("tools", tools);
        return Map.copyOf(metadata);
    }

    private static List<String> sourceRequestIds(Map<String, Object> metadata) {
        String sourceRequestIds = metadataText(metadata, "hermes.sourceRequestIds");
        if (!sourceRequestIds.isBlank()) {
            return Arrays.stream(sourceRequestIds.split(","))
                    .map(HermesSkillLineageEntry::clean)
                    .filter(value -> !value.isBlank())
                    .distinct()
                    .toList();
        }
        String requestId = metadataText(metadata, "hermes.requestId");
        return requestId.isBlank() ? List.of() : List.of(requestId);
    }

    private static String metadataText(Map<String, Object> metadata, String key) {
        return metadataText(metadata, key, "");
    }

    private static String metadataText(Map<String, Object> metadata, String key, String fallback) {
        Object value = metadata == null ? null : metadata.get(key);
        String text = value == null ? "" : clean(value.toString());
        return text.isBlank() ? clean(fallback) : text;
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

    private static String clean(String value) {
        return HermesText.oneLine(value);
    }
}
