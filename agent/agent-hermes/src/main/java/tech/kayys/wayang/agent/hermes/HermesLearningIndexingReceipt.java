package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operator-facing receipt for optional learned-skill indexing.
 */
public record HermesLearningIndexingReceipt(
        String status,
        String outcome,
        String skillId,
        boolean indexed,
        String reason,
        String adapterId,
        Map<String, Object> metadata) {

    public static final String STATUS_INDEXED = "indexed";
    public static final String STATUS_SKIPPED = "skipped";
    public static final String STATUS_FAILED = "failed";

    public static final String OUTCOME_INDEXED = "indexed";
    public static final String OUTCOME_SKIPPED = "skipped";
    public static final String OUTCOME_FAILED = "failed";

    public HermesLearningIndexingReceipt {
        status = HermesText.oneLineOr(status, STATUS_SKIPPED);
        outcome = HermesText.oneLineOr(outcome, OUTCOME_SKIPPED);
        skillId = HermesText.trimOr(skillId, "");
        reason = HermesText.oneLineOr(reason, "");
        adapterId = HermesText.trimOr(adapterId, "");
        metadata = HermesMetadata.copy(metadata);
    }

    public static HermesLearningIndexingReceipt indexed(
            HermesLearningIndexingRequest request,
            String adapterId,
            Map<String, Object> metadata) {
        HermesLearningIndexingRequest resolved = request == null
                ? new HermesLearningIndexingRequest(null, null, null)
                : request;
        return new HermesLearningIndexingReceipt(
                STATUS_INDEXED,
                OUTCOME_INDEXED,
                resolved.skillId(),
                true,
                "learned skill indexed",
                adapterId,
                metadata);
    }

    public static HermesLearningIndexingReceipt skipped(
            HermesLearningIndexingRequest request,
            String reason,
            String adapterId) {
        HermesLearningIndexingRequest resolved = request == null
                ? new HermesLearningIndexingRequest(null, null, null)
                : request;
        return new HermesLearningIndexingReceipt(
                STATUS_SKIPPED,
                OUTCOME_SKIPPED,
                resolved.skillId(),
                false,
                reason,
                adapterId,
                Map.of());
    }

    public static HermesLearningIndexingReceipt failed(
            HermesLearningIndexingRequest request,
            Throwable error,
            String adapterId) {
        HermesLearningIndexingRequest resolved = request == null
                ? new HermesLearningIndexingRequest(null, null, null)
                : request;
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("errorType", error == null ? "" : error.getClass().getName());
        metadata.put("error", error == null ? "" : HermesText.oneLineOr(error.getMessage(), ""));
        return new HermesLearningIndexingReceipt(
                STATUS_FAILED,
                OUTCOME_FAILED,
                resolved.skillId(),
                false,
                error == null ? "learned-skill indexing failed" : HermesText.oneLineOr(error.getMessage(), ""),
                adapterId,
                metadata);
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("status", status);
        values.put("outcome", outcome);
        values.put("skillId", skillId);
        values.put("indexed", indexed);
        values.put("reason", reason);
        values.put("adapterId", adapterId);
        values.put("metadata", metadata);
        return Map.copyOf(values);
    }
}
