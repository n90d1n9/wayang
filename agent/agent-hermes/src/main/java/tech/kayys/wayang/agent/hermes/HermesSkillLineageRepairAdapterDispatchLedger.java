package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Idempotency ledger for mutation-oriented repair adapter dispatches.
 */
public interface HermesSkillLineageRepairAdapterDispatchLedger {

    Optional<HermesSkillLineageRepairAdapterDispatch> find(String idempotencyKey);

    HermesSkillLineageRepairAdapterDispatch record(
            HermesSkillLineageRepairAdapterDispatchRequest request,
            HermesSkillLineageRepairAdapterDispatch dispatch);

    int recordCount();

    Map<String, Object> toMetadata();

    static HermesSkillLineageRepairAdapterDispatchLedger noop() {
        return NoopLedger.INSTANCE;
    }

    static HermesSkillLineageRepairAdapterDispatchLedger inMemory() {
        return new InMemoryLedger();
    }

    private static String key(String idempotencyKey) {
        return idempotencyKey == null || idempotencyKey.isBlank()
                ? ""
                : HermesText.oneLine(idempotencyKey);
    }

    final class NoopLedger implements HermesSkillLineageRepairAdapterDispatchLedger {

        private static final NoopLedger INSTANCE = new NoopLedger();

        private NoopLedger() {
        }

        @Override
        public Optional<HermesSkillLineageRepairAdapterDispatch> find(String idempotencyKey) {
            return Optional.empty();
        }

        @Override
        public HermesSkillLineageRepairAdapterDispatch record(
                HermesSkillLineageRepairAdapterDispatchRequest request,
                HermesSkillLineageRepairAdapterDispatch dispatch) {
            return dispatch;
        }

        @Override
        public int recordCount() {
            return 0;
        }

        @Override
        public Map<String, Object> toMetadata() {
            return Map.of(
                    "ledgerType", "noop",
                    "recordCount", 0,
                    "replaySupported", false);
        }
    }

    final class InMemoryLedger implements HermesSkillLineageRepairAdapterDispatchLedger {

        private final Map<String, HermesSkillLineageRepairAdapterDispatch> records =
                new ConcurrentHashMap<>();

        @Override
        public Optional<HermesSkillLineageRepairAdapterDispatch> find(String idempotencyKey) {
            String key = key(idempotencyKey);
            return key.isBlank() ? Optional.empty() : Optional.ofNullable(records.get(key));
        }

        @Override
        public HermesSkillLineageRepairAdapterDispatch record(
                HermesSkillLineageRepairAdapterDispatchRequest request,
                HermesSkillLineageRepairAdapterDispatch dispatch) {
            if (request == null || dispatch == null || request.idempotencyKey().isBlank()) {
                return dispatch;
            }
            String key = key(request.idempotencyKey());
            records.putIfAbsent(key, dispatch);
            return records.get(key);
        }

        @Override
        public int recordCount() {
            return records.size();
        }

        @Override
        public Map<String, Object> toMetadata() {
            Map<String, Object> values = new LinkedHashMap<>();
            values.put("ledgerType", "in-memory");
            values.put("recordCount", recordCount());
            values.put("replaySupported", true);
            return Map.copyOf(values);
        }
    }
}
