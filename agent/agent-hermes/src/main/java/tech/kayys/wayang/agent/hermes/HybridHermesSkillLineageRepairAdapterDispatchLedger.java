package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Primary/fallback idempotency ledger for cloud-first repair dispatch replay.
 */
public final class HybridHermesSkillLineageRepairAdapterDispatchLedger
        implements HermesSkillLineageRepairAdapterDispatchLedger {

    private final HermesSkillLineageRepairAdapterDispatchLedger primary;
    private final HermesSkillLineageRepairAdapterDispatchLedger fallback;

    public HybridHermesSkillLineageRepairAdapterDispatchLedger(
            HermesSkillLineageRepairAdapterDispatchLedger primary,
            HermesSkillLineageRepairAdapterDispatchLedger fallback) {
        this.primary = primary == null ? HermesSkillLineageRepairAdapterDispatchLedger.noop() : primary;
        this.fallback = fallback == null ? HermesSkillLineageRepairAdapterDispatchLedger.noop() : fallback;
    }

    @Override
    public Optional<HermesSkillLineageRepairAdapterDispatch> find(String idempotencyKey) {
        Optional<HermesSkillLineageRepairAdapterDispatch> primaryDispatch = findSafely(primary, idempotencyKey);
        return primaryDispatch.isPresent() ? primaryDispatch : findSafely(fallback, idempotencyKey);
    }

    @Override
    public HermesSkillLineageRepairAdapterDispatch record(
            HermesSkillLineageRepairAdapterDispatchRequest request,
            HermesSkillLineageRepairAdapterDispatch dispatch) {
        if (request == null || dispatch == null || request.idempotencyKey().isBlank()) {
            return dispatch;
        }
        Optional<HermesSkillLineageRepairAdapterDispatch> existing = find(request.idempotencyKey());
        if (existing.isPresent()) {
            return existing.get();
        }

        HermesSkillLineageRepairAdapterDispatch recorded = null;
        RuntimeException primaryError = null;
        try {
            recorded = primary.record(request, dispatch);
        } catch (RuntimeException error) {
            primaryError = error;
        }

        try {
            HermesSkillLineageRepairAdapterDispatch fallbackDispatch =
                    fallback.record(request, recorded == null ? dispatch : recorded);
            return recorded == null ? fallbackDispatch : recorded;
        } catch (RuntimeException fallbackError) {
            if (recorded != null) {
                return recorded;
            }
            if (primaryError != null) {
                primaryError.addSuppressed(fallbackError);
                throw primaryError;
            }
            throw fallbackError;
        }
    }

    @Override
    public int recordCount() {
        return Math.max(recordCount(primary), recordCount(fallback));
    }

    @Override
    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("ledgerType", "hybrid");
        values.put("recordCount", recordCount());
        values.put("replaySupported", true);
        values.put("primaryLedger", primary.toMetadata());
        values.put("fallbackLedger", fallback.toMetadata());
        return Map.copyOf(values);
    }

    private static Optional<HermesSkillLineageRepairAdapterDispatch> findSafely(
            HermesSkillLineageRepairAdapterDispatchLedger ledger,
            String idempotencyKey) {
        try {
            return ledger.find(idempotencyKey);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private static int recordCount(HermesSkillLineageRepairAdapterDispatchLedger ledger) {
        try {
            return ledger.recordCount();
        } catch (RuntimeException ignored) {
            return 0;
        }
    }
}
