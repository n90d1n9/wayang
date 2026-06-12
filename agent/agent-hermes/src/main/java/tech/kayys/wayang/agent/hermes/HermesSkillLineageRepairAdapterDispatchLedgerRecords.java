package tech.kayys.wayang.agent.hermes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared JSON record shape for repair adapter dispatch idempotency ledgers.
 */
final class HermesSkillLineageRepairAdapterDispatchLedgerRecords {

    static final String RECORD_TYPE = "hermes.skill-lineage.repair.adapter-dispatch";

    private HermesSkillLineageRepairAdapterDispatchLedgerRecords() {
    }

    static String key(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank()
                ? ""
                : HermesText.oneLine(idempotencyKey);
    }

    static Map<String, Object> recordMetadata(
            HermesSkillLineageRepairAdapterDispatchRequest request,
            HermesSkillLineageRepairAdapterDispatch dispatch) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("recordType", RECORD_TYPE);
        values.put("recordedAt", Instant.now().toString());
        values.put("idempotencyKey", request.idempotencyKey());
        values.put("action", request.action());
        values.put("approvalId", request.approvalId());
        values.put("request", request.toMetadata());
        values.put("dispatch", dispatch.toMetadata());
        return Map.copyOf(values);
    }

    static HermesSkillLineageRepairAdapterDispatch dispatch(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return HermesSkillLineageRepairAdapterDispatch.from(
                    HermesSkillLineageRepairAdapter.PREVIEW,
                    List.of(),
                    Map.of("source", "repair-adapter-dispatch-ledger"));
        }
        Map<String, Object> values = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> values.put(String.valueOf(key), mapValue));
        Object results = values.get("results");
        List<HermesSkillLineageRepairAdapterResult> parsedResults = results instanceof List<?> list
                ? list.stream()
                        .filter(item -> item instanceof Map<?, ?>)
                        .map(HermesSkillLineageRepairAdapterDispatchLedgerRecords::result)
                        .toList()
                : List.of();
        return new HermesSkillLineageRepairAdapterDispatch(
                text(values.get("action")),
                text(values.get("dispatchStatus")),
                number(values.get("batchCount")),
                number(values.get("dispatchedBatchCount")),
                number(values.get("successfulBatchCount")),
                number(values.get("failedBatchCount")),
                number(values.get("unsupportedBatchCount")),
                parsedResults,
                objectMap(values.get("metadata")));
    }

    @SuppressWarnings("unchecked")
    private static HermesSkillLineageRepairAdapterResult result(Object value) {
        return result((Map<String, Object>) value);
    }

    private static HermesSkillLineageRepairAdapterResult result(Map<String, Object> values) {
        return new HermesSkillLineageRepairAdapterResult(
                text(values.get("adapterId")),
                text(values.get("action")),
                text(values.get("batchId")),
                text(values.get("backendId")),
                text(values.get("storageFamily")),
                number(values.get("operationCount")),
                bool(values.get("active")),
                bool(values.get("dispatched")),
                bool(values.get("successful")),
                bool(values.get("mutationAttempted")),
                text(values.get("status")),
                text(values.get("reason")),
                objectMap(values.get("metadata")));
    }

    static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    static int number(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    static boolean bool(Object value) {
        return value instanceof Boolean bool && bool;
    }

    static Map<String, Object> objectMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> values = new LinkedHashMap<>();
        map.forEach((key, mapValue) -> values.put(String.valueOf(key), mapValue));
        return Map.copyOf(values);
    }
}
