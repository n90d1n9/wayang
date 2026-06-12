package tech.kayys.wayang.agent.hermes;

import java.util.List;
import java.util.Map;

/**
 * Concrete execution boundary for one or more lineage repair backends.
 */
public interface HermesSkillLineageRepairAdapter {

    String PREVIEW = "preview";
    String APPLY = "apply";
    String ROLLBACK = "rollback";

    HermesSkillLineageRepairAdapterCapabilities capabilities();

    default String adapterId() {
        return capabilities().adapterId();
    }

    default boolean supports(String action, HermesSkillLineageRepairOperationBatch batch) {
        HermesSkillLineageRepairAdapterCapabilities capabilities = capabilities();
        return capabilities.supportsAction(action) && capabilities.supportsBatch(batch);
    }

    default HermesSkillLineageRepairAdapterResult preview(
            HermesSkillLineageRepairOperationBatch batch) {
        return HermesSkillLineageRepairAdapterResult.dispatched(
                adapterId(),
                PREVIEW,
                batch,
                "previewed",
                "repair batch preview accepted by adapter",
                batchMetadata(batch));
    }

    default HermesSkillLineageRepairAdapterResult apply(
            HermesSkillLineageRepairOperationBatch batch) {
        return HermesSkillLineageRepairAdapterResult.unavailable(
                adapterId(),
                APPLY,
                batch,
                "repair adapter does not implement apply",
                batchMetadata(batch));
    }

    default HermesSkillLineageRepairAdapterResult rollback(
            HermesSkillLineageRepairOperationBatch batch) {
        return HermesSkillLineageRepairAdapterResult.unavailable(
                adapterId(),
                ROLLBACK,
                batch,
                "repair adapter does not implement rollback",
                batchMetadata(batch));
    }

    static HermesSkillLineageRepairAdapter previewOnly(
            String adapterId,
            String backendId,
            String storageFamily) {
        return () -> HermesSkillLineageRepairAdapterCapabilities.previewOnly(
                adapterId,
                oneOrEmpty(backendId),
                oneOrEmpty(storageFamily));
    }

    private static Map<String, Object> batchMetadata(HermesSkillLineageRepairOperationBatch batch) {
        return batch == null ? Map.of() : Map.of("batch", batch.toMetadata());
    }

    private static List<String> oneOrEmpty(String value) {
        return value == null || value.isBlank() ? List.of() : List.of(value);
    }
}
