package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Backend-neutral catalog summary for the whole learned-skill library.
 */
public record HermesSkillLineageCatalog(
        int learnedSkillCount,
        int rootCount,
        long refinedRootCount,
        long refinedEntryCount,
        long orphanedRootCount,
        List<String> sourceRequestIds,
        Map<String, Long> mergeStrategyCounts,
        List<HermesSkillLineageRoot> roots) {

    public HermesSkillLineageCatalog {
        learnedSkillCount = Math.max(learnedSkillCount, 0);
        rootCount = Math.max(rootCount, 0);
        refinedRootCount = Math.max(refinedRootCount, 0);
        refinedEntryCount = Math.max(refinedEntryCount, 0);
        orphanedRootCount = Math.max(orphanedRootCount, 0);
        sourceRequestIds = sourceRequestIds == null ? List.of() : List.copyOf(sourceRequestIds.stream()
                .map(HermesSkillLineageCatalog::clean)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
        mergeStrategyCounts = mergeStrategyCounts == null ? Map.of() : Map.copyOf(mergeStrategyCounts);
        roots = HermesCollections.copyNonNull(roots);
    }

    public static HermesSkillLineageCatalog empty() {
        return from(List.of());
    }

    public static HermesSkillLineageCatalog from(List<SkillDefinition> learnedSkills) {
        List<HermesSkillLineageEntry> entries = HermesCollections.copyNonNull(learnedSkills).stream()
                .map(HermesSkillLineageEntry::from)
                .toList();
        Map<String, List<HermesSkillLineageEntry>> byRoot = new LinkedHashMap<>();
        for (HermesSkillLineageEntry entry : entries) {
            String rootSkillId = entry.lineageRootSkillId().isBlank()
                    ? entry.skillId()
                    : entry.lineageRootSkillId();
            byRoot.computeIfAbsent(rootSkillId, ignored -> new java.util.ArrayList<>()).add(entry);
        }
        List<HermesSkillLineageRoot> roots = byRoot.entrySet().stream()
                .map(entry -> HermesSkillLineageRoot.from(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(HermesSkillLineageRoot::rootSkillId))
                .toList();
        return new HermesSkillLineageCatalog(
                entries.size(),
                roots.size(),
                roots.stream().filter(HermesSkillLineageRoot::hasRefinements).count(),
                entries.stream().filter(HermesSkillLineageEntry::refined).count(),
                roots.stream().filter(root -> !root.rootPresent()).count(),
                sourceRequestIds(roots),
                mergeStrategyCounts(entries),
                roots);
    }

    public boolean emptyCatalog() {
        return learnedSkillCount == 0;
    }

    public HermesSkillLineageHealth health() {
        return HermesSkillLineageHealth.from(this);
    }

    public HermesSkillStoreConsistencyReport consistencyReport() {
        return HermesSkillStoreConsistencyReport.from(this);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("learnedSkillCount", learnedSkillCount);
        metadata.put("rootCount", rootCount);
        metadata.put("refinedRootCount", refinedRootCount);
        metadata.put("refinedEntryCount", refinedEntryCount);
        metadata.put("orphanedRootCount", orphanedRootCount);
        metadata.put("health", health().toMetadata());
        metadata.put("consistencyReport", consistencyReport().toMetadata());
        metadata.put("sourceRequestIds", sourceRequestIds);
        metadata.put("mergeStrategyCounts", mergeStrategyCounts);
        metadata.put("roots", roots.stream()
                .map(HermesSkillLineageRoot::toMetadata)
                .toList());
        return Map.copyOf(metadata);
    }

    private static List<String> sourceRequestIds(List<HermesSkillLineageRoot> roots) {
        Set<String> values = new LinkedHashSet<>();
        for (HermesSkillLineageRoot root : roots) {
            values.addAll(root.sourceRequestIds());
        }
        return List.copyOf(values);
    }

    private static Map<String, Long> mergeStrategyCounts(List<HermesSkillLineageEntry> entries) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (HermesSkillLineageEntry entry : entries) {
            String mergeStrategy = entry.mergeStrategy().isBlank() ? "unknown" : entry.mergeStrategy();
            counts.merge(mergeStrategy, 1L, Long::sum);
        }
        return counts;
    }

    private static String clean(String value) {
        return HermesText.oneLine(value);
    }
}
