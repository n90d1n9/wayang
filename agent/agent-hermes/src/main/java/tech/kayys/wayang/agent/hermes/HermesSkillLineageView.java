package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Backend-neutral read model for learned-skill revision and merge lineage.
 */
public record HermesSkillLineageView(
        String requestedSkillId,
        boolean found,
        String rootSkillId,
        String currentSkillId,
        String currentRevision,
        int entryCount,
        boolean hasRefinements,
        List<String> sourceRequestIds,
        List<String> mergeStrategies,
        List<HermesSkillLineageEntry> entries) {

    public HermesSkillLineageView {
        requestedSkillId = clean(requestedSkillId);
        rootSkillId = clean(rootSkillId);
        currentSkillId = clean(currentSkillId);
        currentRevision = clean(currentRevision);
        entries = HermesCollections.copyNonNull(entries);
        entryCount = Math.max(entryCount, entries.size());
        sourceRequestIds = sourceRequestIds == null ? List.of() : List.copyOf(sourceRequestIds.stream()
                .map(HermesSkillLineageView::clean)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
        mergeStrategies = mergeStrategies == null ? List.of() : List.copyOf(mergeStrategies.stream()
                .map(HermesSkillLineageView::clean)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList());
    }

    public static HermesSkillLineageView missing(String requestedSkillId) {
        return new HermesSkillLineageView(
                requestedSkillId,
                false,
                "",
                "",
                "",
                0,
                false,
                List.of(),
                List.of(),
                List.of());
    }

    public static HermesSkillLineageView from(
            String requestedSkillId,
            Optional<SkillDefinition> requestedSkill,
            List<SkillDefinition> learnedSkills) {
        Optional<SkillDefinition> requested = requestedSkill == null ? Optional.empty() : requestedSkill;
        if (requested.isEmpty()) {
            return missing(requestedSkillId);
        }

        HermesSkillLineageEntry requestedEntry = HermesSkillLineageEntry.from(requested.orElseThrow());
        String rootSkillId = requestedEntry.lineageRootSkillId().isBlank()
                ? requestedEntry.skillId()
                : requestedEntry.lineageRootSkillId();
        List<HermesSkillLineageEntry> entries = entriesForRoot(rootSkillId, requestedEntry, learnedSkills);
        HermesSkillLineageEntry current = entries.stream()
                .max(Comparator
                        .comparingInt(HermesSkillLineageEntry::lineageDepth)
                        .thenComparingInt(HermesSkillLineageEntry::revisionNumber)
                        .thenComparing(HermesSkillLineageEntry::skillId))
                .orElse(requestedEntry);
        return new HermesSkillLineageView(
                requestedSkillId,
                true,
                rootSkillId,
                current.skillId(),
                current.revision(),
                entries.size(),
                entries.stream().anyMatch(HermesSkillLineageEntry::refined),
                sourceRequestIds(entries),
                mergeStrategies(entries),
                entries);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestedSkillId", requestedSkillId);
        metadata.put("found", found);
        metadata.put("rootSkillId", rootSkillId);
        metadata.put("currentSkillId", currentSkillId);
        metadata.put("currentRevision", currentRevision);
        metadata.put("entryCount", entryCount);
        metadata.put("hasRefinements", hasRefinements);
        metadata.put("sourceRequestIds", sourceRequestIds);
        metadata.put("mergeStrategies", mergeStrategies);
        metadata.put("entries", entries.stream()
                .map(HermesSkillLineageEntry::toMetadata)
                .toList());
        return Map.copyOf(metadata);
    }

    private static List<HermesSkillLineageEntry> entriesForRoot(
            String rootSkillId,
            HermesSkillLineageEntry requestedEntry,
            List<SkillDefinition> learnedSkills) {
        Map<String, HermesSkillLineageEntry> entries = new LinkedHashMap<>();
        entries.put(requestedEntry.skillId(), requestedEntry);
        if (learnedSkills != null) {
            for (SkillDefinition skill : learnedSkills) {
                HermesSkillLineageEntry entry = HermesSkillLineageEntry.from(skill);
                if (belongsToRoot(rootSkillId, entry)) {
                    entries.put(entry.skillId(), entry);
                }
            }
        }
        return entries.values().stream()
                .sorted(Comparator
                        .comparingInt(HermesSkillLineageEntry::lineageDepth)
                        .thenComparingInt(HermesSkillLineageEntry::revisionNumber)
                        .thenComparing(HermesSkillLineageEntry::skillId))
                .toList();
    }

    private static boolean belongsToRoot(String rootSkillId, HermesSkillLineageEntry entry) {
        return clean(rootSkillId).equals(entry.lineageRootSkillId())
                || clean(rootSkillId).equals(entry.skillId())
                || clean(rootSkillId).equals(entry.supersedesSkillId())
                || clean(rootSkillId).equals(entry.derivedFromSkillId());
    }

    private static List<String> sourceRequestIds(List<HermesSkillLineageEntry> entries) {
        Set<String> requestIds = new LinkedHashSet<>();
        for (HermesSkillLineageEntry entry : entries) {
            requestIds.addAll(entry.sourceRequestIds());
        }
        return List.copyOf(requestIds);
    }

    private static List<String> mergeStrategies(List<HermesSkillLineageEntry> entries) {
        Set<String> strategies = new LinkedHashSet<>();
        for (HermesSkillLineageEntry entry : entries) {
            if (!entry.mergeStrategy().isBlank()) {
                strategies.add(entry.mergeStrategy());
            }
        }
        return List.copyOf(strategies);
    }

    private static String clean(String value) {
        return HermesText.oneLine(value);
    }
}
