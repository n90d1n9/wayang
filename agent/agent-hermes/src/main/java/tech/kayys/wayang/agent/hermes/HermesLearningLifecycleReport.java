package tech.kayys.wayang.agent.hermes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Compact report describing the stages observed during one Hermes learning pass.
 */
public record HermesLearningLifecycleReport(
        List<HermesLearningStageReport> stages,
        String terminalStage) {

    public HermesLearningLifecycleReport {
        stages = HermesCollections.copyNonNull(stages);
        terminalStage = HermesDirectiveSupport.clean(terminalStage, lastStage(stages));
    }

    public static HermesLearningLifecycleReport empty() {
        return new HermesLearningLifecycleReport(List.of(), "");
    }

    public static HermesLearningLifecycleReport fromStages(HermesLearningStageReport... stages) {
        return new HermesLearningLifecycleReport(
                stages == null ? List.of() : Arrays.asList(stages),
                "");
    }

    public static HermesLearningLifecycleReport fromMetadata(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return empty();
        }
        Object stagesValue = map.get(HermesLearningMetadataKeys.STAGES);
        if (!(stagesValue instanceof List<?> stageValues)) {
            return empty();
        }
        List<HermesLearningStageReport> reports = stageValues.stream()
                .map(HermesLearningStageReport::fromMetadata)
                .flatMap(java.util.Optional::stream)
                .toList();
        return new HermesLearningLifecycleReport(
                reports,
                text(map.get(HermesLearningMetadataKeys.TERMINAL_STAGE)));
    }

    public boolean emptyReport() {
        return stages.isEmpty();
    }

    public HermesLearningLifecycleReport withStage(HermesLearningStageReport stage) {
        if (stage == null) {
            return this;
        }
        List<HermesLearningStageReport> merged = new ArrayList<>();
        boolean replaced = false;
        for (HermesLearningStageReport existing : stages) {
            if (existing.stage().equals(stage.stage())) {
                merged.add(stage);
                replaced = true;
            } else {
                merged.add(existing);
            }
        }
        if (!replaced) {
            merged.add(stage);
        }
        return new HermesLearningLifecycleReport(merged, stage.stage());
    }

    public List<String> completedStages() {
        return stagesWithStatus(HermesLearningStageReport.STATUS_COMPLETED);
    }

    public List<String> skippedStages() {
        return stagesWithStatus(HermesLearningStageReport.STATUS_SKIPPED);
    }

    public List<String> failedStages() {
        return stagesWithStatus(HermesLearningStageReport.STATUS_FAILED);
    }

    public List<String> pendingStages() {
        return stagesWithStatus(HermesLearningStageReport.STATUS_PENDING);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put(HermesLearningMetadataKeys.TERMINAL_STAGE, terminalStage);
        values.put(HermesLearningMetadataKeys.COMPLETED_STAGES, completedStages());
        values.put(HermesLearningMetadataKeys.SKIPPED_STAGES, skippedStages());
        values.put(HermesLearningMetadataKeys.FAILED_STAGES, failedStages());
        values.put(HermesLearningMetadataKeys.PENDING_STAGES, pendingStages());
        values.put(HermesLearningMetadataKeys.STAGES, stages.stream()
                .map(HermesLearningStageReport::toMetadata)
                .toList());
        return Map.copyOf(values);
    }

    private List<String> stagesWithStatus(String status) {
        return stages.stream()
                .filter(stage -> stage.status().equals(status))
                .map(HermesLearningStageReport::stage)
                .toList();
    }

    private static String lastStage(List<HermesLearningStageReport> stages) {
        if (stages == null || stages.isEmpty()) {
            return "";
        }
        return stages.getLast().stage();
    }

    private static String text(Object value) {
        return value == null ? "" : HermesText.oneLineOr(String.valueOf(value), "");
    }
}
