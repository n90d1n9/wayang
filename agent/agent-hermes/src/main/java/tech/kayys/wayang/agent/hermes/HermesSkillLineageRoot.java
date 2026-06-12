package tech.kayys.wayang.agent.hermes;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Summary for one learned-skill lineage root.
 */
public record HermesSkillLineageRoot(
        String rootSkillId,
        String currentSkillId,
        String currentRevision,
        String currentTask,
        String latestRequestId,
        boolean rootPresent,
        boolean hasRefinements,
        int entryCount,
        List<String> sourceRequestIds,
        List<String> mergeStrategies,
        List<HermesSkillLineageEntry> entries) {

    public HermesSkillLineageRoot {
        rootSkillId = clean(rootSkillId);
        currentSkillId = clean(currentSkillId);
        currentRevision = clean(currentRevision);
        currentTask = clean(currentTask);
        latestRequestId = clean(latestRequestId);
        entryCount = Math.max(entryCount, 0);
        entries = HermesCollections.copyNonNull(entries);
        sourceRequestIds = sourceRequestIds == null ? List.of() : List.copyOf(sourceRequestIds.stream()
                .map(HermesSkillLineageRoot::clean)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
        mergeStrategies = mergeStrategies == null ? List.of() : List.copyOf(mergeStrategies.stream()
                .map(HermesSkillLineageRoot::clean)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
    }

    public static HermesSkillLineageRoot from(String rootSkillId, List<HermesSkillLineageEntry> entries) {
        List<HermesSkillLineageEntry> sorted = HermesCollections.copyNonNull(entries).stream()
                .sorted(Comparator
                        .comparingInt(HermesSkillLineageEntry::lineageDepth)
                        .thenComparingInt(HermesSkillLineageEntry::revisionNumber)
                        .thenComparing(HermesSkillLineageEntry::skillId))
                .toList();
        HermesSkillLineageEntry current = sorted.stream()
                .max(Comparator
                        .comparingInt(HermesSkillLineageEntry::lineageDepth)
                        .thenComparingInt(HermesSkillLineageEntry::revisionNumber)
                        .thenComparing(HermesSkillLineageEntry::skillId))
                .orElse(null);
        String root = clean(rootSkillId);
        boolean rootPresent = sorted.stream().anyMatch(entry -> root.equals(entry.skillId()));
        return new HermesSkillLineageRoot(
                root,
                current == null ? "" : current.skillId(),
                current == null ? "" : current.revision(),
                current == null ? "" : current.task(),
                current == null ? "" : current.latestRequestId(),
                rootPresent,
                sorted.stream().anyMatch(HermesSkillLineageEntry::refined),
                sorted.size(),
                sourceRequestIds(sorted),
                mergeStrategies(sorted),
                sorted);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("rootSkillId", rootSkillId);
        metadata.put("currentSkillId", currentSkillId);
        metadata.put("currentRevision", currentRevision);
        metadata.put("currentTask", currentTask);
        metadata.put("latestRequestId", latestRequestId);
        metadata.put("rootPresent", rootPresent);
        metadata.put("hasRefinements", hasRefinements);
        metadata.put("entryCount", entryCount);
        metadata.put("sourceRequestIds", sourceRequestIds);
        metadata.put("mergeStrategies", mergeStrategies);
        metadata.put("entries", entries.stream()
                .map(HermesSkillLineageEntry::toMetadata)
                .toList());
        return Map.copyOf(metadata);
    }

    private static List<String> sourceRequestIds(List<HermesSkillLineageEntry> entries) {
        Set<String> values = new LinkedHashSet<>();
        for (HermesSkillLineageEntry entry : entries) {
            values.addAll(entry.sourceRequestIds());
        }
        return List.copyOf(values);
    }

    private static List<String> mergeStrategies(List<HermesSkillLineageEntry> entries) {
        Set<String> values = new LinkedHashSet<>();
        for (HermesSkillLineageEntry entry : entries) {
            if (!entry.mergeStrategy().isBlank()) {
                values.add(entry.mergeStrategy());
            }
        }
        return List.copyOf(values);
    }

    private static String clean(String value) {
        return HermesText.oneLine(value);
    }
}
