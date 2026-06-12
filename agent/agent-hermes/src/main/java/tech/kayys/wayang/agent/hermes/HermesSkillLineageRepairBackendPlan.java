package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregate backend readiness plan for policy-permitted repair intents.
 */
public record HermesSkillLineageRepairBackendPlan(
        int backendCount,
        int intentCount,
        int commandSupportedIntentCount,
        int mutationSupportedIntentCount,
        int previewOnlyIntentCount,
        boolean mutationSupported,
        List<HermesSkillLineageRepairBackendAssessment> assessments) {

    public HermesSkillLineageRepairBackendPlan {
        assessments = HermesCollections.copyNonNull(assessments);
        intentCount = assessments.size();
        commandSupportedIntentCount = (int) assessments.stream()
                .filter(HermesSkillLineageRepairBackendAssessment::commandSupported)
                .count();
        mutationSupportedIntentCount = (int) assessments.stream()
                .filter(HermesSkillLineageRepairBackendAssessment::mutationSupported)
                .count();
        previewOnlyIntentCount = commandSupportedIntentCount - mutationSupportedIntentCount;
        mutationSupported = intentCount > 0 && mutationSupportedIntentCount == intentCount;
        backendCount = Math.max(backendCount, 0);
    }

    public static HermesSkillLineageRepairBackendPlan empty(int backendCount) {
        return new HermesSkillLineageRepairBackendPlan(
                backendCount,
                0,
                0,
                0,
                0,
                false,
                List.of());
    }

    public List<HermesSkillLineageRepairOperation> operations() {
        return assessments.stream()
                .map(HermesSkillLineageRepairOperation::from)
                .toList();
    }

    public int operationCount() {
        return operations().size();
    }

    public int mutationReadyOperationCount() {
        return (int) operations().stream()
                .filter(HermesSkillLineageRepairOperation::mutationReady)
                .count();
    }

    public int previewOnlyOperationCount() {
        return (int) operations().stream()
                .filter(operation -> "preview-only".equals(operation.status()))
                .count();
    }

    public int unsupportedOperationCount() {
        return (int) operations().stream()
                .filter(operation -> "unsupported".equals(operation.status()))
                .count();
    }

    public Map<String, Object> toMetadata() {
        List<HermesSkillLineageRepairOperation> operations = operations();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("backendCount", backendCount);
        values.put("intentCount", intentCount);
        values.put("commandSupportedIntentCount", commandSupportedIntentCount);
        values.put("mutationSupportedIntentCount", mutationSupportedIntentCount);
        values.put("previewOnlyIntentCount", previewOnlyIntentCount);
        values.put("mutationSupported", mutationSupported);
        values.put("operationCount", operations.size());
        values.put("mutationReadyOperationCount", mutationReadyOperationCount());
        values.put("previewOnlyOperationCount", previewOnlyOperationCount());
        values.put("unsupportedOperationCount", unsupportedOperationCount());
        values.put("operations", operations.stream()
                .map(HermesSkillLineageRepairOperation::toMetadata)
                .toList());
        values.put("assessments", assessments.stream()
                .map(HermesSkillLineageRepairBackendAssessment::toMetadata)
                .toList());
        return Map.copyOf(values);
    }
}
