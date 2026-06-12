package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.storage.spi.ObjectStorageService;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Routes adapter-ready repair batches to concrete repair adapters.
 */
public final class HermesSkillLineageRepairAdapterRegistry {

    private final Map<String, HermesSkillLineageRepairAdapter> adapters;
    private final HermesSkillLineageRepairAdapterDispatchLedger dispatchLedger;

    public HermesSkillLineageRepairAdapterRegistry(List<HermesSkillLineageRepairAdapter> adapters) {
        this(adapters, HermesSkillLineageRepairAdapterDispatchLedger.noop());
    }

    public HermesSkillLineageRepairAdapterRegistry(
            List<HermesSkillLineageRepairAdapter> adapters,
            HermesSkillLineageRepairAdapterDispatchLedger dispatchLedger) {
        Map<String, HermesSkillLineageRepairAdapter> values = new LinkedHashMap<>();
        if (adapters != null) {
            adapters.stream()
                    .filter(adapter -> adapter != null && adapter.capabilities() != null)
                    .forEach(adapter -> values.put(adapter.adapterId(), adapter));
        }
        this.adapters = Collections.unmodifiableMap(values);
        this.dispatchLedger = dispatchLedger == null
                ? HermesSkillLineageRepairAdapterDispatchLedger.noop()
                : dispatchLedger;
    }

    public static HermesSkillLineageRepairAdapterRegistry empty() {
        return new HermesSkillLineageRepairAdapterRegistry(List.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public int adapterCount() {
        return adapters.size();
    }

    public boolean isEmpty() {
        return adapters.isEmpty();
    }

    public List<String> adapterIds() {
        return List.copyOf(adapters.keySet());
    }

    public List<HermesSkillLineageRepairAdapterCapabilities> capabilities() {
        return adapters.values().stream()
                .map(HermesSkillLineageRepairAdapter::capabilities)
                .toList();
    }

    public Optional<HermesSkillLineageRepairAdapter> resolve(
            String action,
            HermesSkillLineageRepairOperationBatch batch) {
        String resolvedAction = HermesSkillLineageRepairAdapterCapabilities.normalize(
                action,
                HermesSkillLineageRepairAdapter.PREVIEW);
        if (batch == null || !batch.adapterReady()) {
            return Optional.empty();
        }
        return adapters.values().stream()
                .filter(adapter -> adapter.supports(resolvedAction, batch))
                .max(Comparator.comparingInt(adapter -> score(adapter.capabilities(), batch)));
    }

    public HermesSkillLineageRepairAdapterResult dispatch(
            String action,
            HermesSkillLineageRepairOperationBatch batch) {
        return dispatch(batchRequest(action), batch);
    }

    private HermesSkillLineageRepairAdapterResult dispatch(
            HermesSkillLineageRepairAdapterDispatchRequest request,
            HermesSkillLineageRepairOperationBatch batch) {
        HermesSkillLineageRepairAdapterDispatchRequest resolvedRequest = request == null
                ? batchRequest(HermesSkillLineageRepairAdapter.PREVIEW)
                : request;
        String resolvedAction = resolvedRequest.action();
        if (batch == null) {
            return HermesSkillLineageRepairAdapterResult.unavailable(
                    "repair-adapter-registry",
                    resolvedAction,
                    null,
                    "no repair operation batch supplied",
                    Map.of("registry", toMetadata(), "request", resolvedRequest.toResultMetadata()));
        }
        if (!batch.adapterReady()) {
            return HermesSkillLineageRepairAdapterResult.unavailable(
                    "repair-adapter-registry",
                    resolvedAction,
                    batch,
                    "repair operation batch is not adapter-ready",
                    dispatchMetadata(resolvedRequest, batch));
        }
        if (batch.approvalRequired()
                && resolvedRequest.mutationRequested()
                && !resolvedRequest.approvalSatisfied()) {
            return HermesSkillLineageRepairAdapterResult.unavailable(
                    "repair-adapter-registry",
                    resolvedAction,
                    batch,
                    "repair operation batch requires approval before mutation",
                    dispatchMetadata(resolvedRequest, batch));
        }
        Optional<HermesSkillLineageRepairAdapter> adapter = resolve(resolvedAction, batch);
        if (adapter.isEmpty()) {
            return HermesSkillLineageRepairAdapterResult.unavailable(
                    "repair-adapter-registry",
                    resolvedAction,
                    batch,
                    "no repair adapter registered for batch backend, storage family, and action",
                    dispatchMetadata(resolvedRequest, batch));
        }
        HermesSkillLineageRepairAdapterResult result = switch (resolvedAction) {
            case HermesSkillLineageRepairAdapter.APPLY -> adapter.get().apply(batch);
            case HermesSkillLineageRepairAdapter.ROLLBACK -> adapter.get().rollback(batch);
            default -> adapter.get().preview(batch);
        };
        if (result == null) {
            return HermesSkillLineageRepairAdapterResult.unavailable(
                    adapter.get().adapterId(),
                    resolvedAction,
                    batch,
                    "repair adapter returned no dispatch result",
                    dispatchMetadata(resolvedRequest, batch));
        }
        return withDispatchMetadata(result, resolvedRequest);
    }

    public HermesSkillLineageRepairAdapterDispatch preview(
            HermesSkillLineageRepairOperationBatchSelection selection) {
        return dispatch(HermesSkillLineageRepairAdapterDispatchRequest.preview(selection));
    }

    public HermesSkillLineageRepairAdapterDispatch apply(
            HermesSkillLineageRepairOperationBatchSelection selection) {
        return dispatch(HermesSkillLineageRepairAdapterDispatchRequest.apply(selection));
    }

    public HermesSkillLineageRepairAdapterDispatch apply(
            HermesSkillLineageRepairOperationBatchSelection selection,
            String approvalId) {
        return dispatch(HermesSkillLineageRepairAdapterDispatchRequest.approvedApply(selection, approvalId));
    }

    public HermesSkillLineageRepairAdapterDispatch rollback(
            HermesSkillLineageRepairOperationBatchSelection selection) {
        return dispatch(HermesSkillLineageRepairAdapterDispatchRequest.rollback(selection));
    }

    public HermesSkillLineageRepairAdapterDispatch rollback(
            HermesSkillLineageRepairOperationBatchSelection selection,
            String approvalId) {
        return dispatch(HermesSkillLineageRepairAdapterDispatchRequest.approvedRollback(selection, approvalId));
    }

    public HermesSkillLineageRepairAdapterDispatch dispatch(
            String action,
            HermesSkillLineageRepairOperationBatchSelection selection) {
        return dispatch(new HermesSkillLineageRepairAdapterDispatchRequest(
                action,
                selection,
                false,
                "",
                "",
                Map.of()));
    }

    public HermesSkillLineageRepairAdapterDispatch dispatch(
            HermesSkillLineageRepairAdapterDispatchRequest request) {
        HermesSkillLineageRepairAdapterDispatchRequest resolved = request == null
                ? HermesSkillLineageRepairAdapterDispatchRequest.preview(null)
                : request;
        if (ledgerEligible(resolved)) {
            Optional<HermesSkillLineageRepairAdapterDispatch> recorded =
                    dispatchLedger.find(resolved.idempotencyKey());
            if (recorded.isPresent()) {
                return withLedgerMetadata(recorded.get(), resolved, "replayed");
            }
        }
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("registry", toMetadata());
        metadata.put("request", resolved.toMetadata());
        HermesSkillLineageRepairAdapterDispatch dispatch = HermesSkillLineageRepairAdapterDispatch.from(
                resolved.action(),
                resolved.selection().batches().stream()
                        .map(batch -> dispatch(resolved, batch))
                        .toList(),
                metadata);
        if (ledgerEligible(resolved) && dispatch.dispatchedBatchCount() > 0) {
            HermesSkillLineageRepairAdapterDispatch recorded =
                    dispatchLedger.record(resolved, withLedgerMetadata(dispatch, resolved, "recorded"));
            return withLedgerMetadata(recorded, resolved, "recorded");
        }
        return dispatch;
    }

    public Map<String, Object> toMetadata() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("adapterCount", adapterCount());
        values.put("adapterIds", adapterIds());
        values.put("capabilities", capabilities().stream()
                .map(HermesSkillLineageRepairAdapterCapabilities::toMetadata)
                .toList());
        values.put("dispatchLedger", dispatchLedger.toMetadata());
        return Map.copyOf(values);
    }

    private static int score(
            HermesSkillLineageRepairAdapterCapabilities capabilities,
            HermesSkillLineageRepairOperationBatch batch) {
        int score = 0;
        if (capabilities.matchesStorageFamily(batch.storageFamily())) {
            score += 1;
        }
        if (capabilities.matchesBackend(batch.backendId())) {
            score += 2;
        }
        return score;
    }

    private static HermesSkillLineageRepairAdapterDispatchRequest batchRequest(String action) {
        return new HermesSkillLineageRepairAdapterDispatchRequest(
                action,
                null,
                false,
                "",
                "",
                Map.of());
    }

    private Map<String, Object> dispatchMetadata(
            HermesSkillLineageRepairAdapterDispatchRequest request,
            HermesSkillLineageRepairOperationBatch batch) {
        return Map.of(
                "registry", toMetadata(),
                "request", request.toResultMetadata(),
                "batch", batch.toMetadata());
    }

    private static HermesSkillLineageRepairAdapterResult withDispatchMetadata(
            HermesSkillLineageRepairAdapterResult result,
            HermesSkillLineageRepairAdapterDispatchRequest request) {
        Map<String, Object> metadata = new LinkedHashMap<>(result.metadata());
        metadata.put("dispatchRequest", request.toResultMetadata());
        metadata.put("idempotencyKey", request.idempotencyKey());
        metadata.put("approvalId", request.approvalId());
        return new HermesSkillLineageRepairAdapterResult(
                result.adapterId(),
                result.action(),
                result.batchId(),
                result.backendId(),
                result.storageFamily(),
                result.operationCount(),
                result.active(),
                result.dispatched(),
                result.successful(),
                result.mutationAttempted(),
                result.status(),
                result.reason(),
                metadata);
    }

    private static boolean ledgerEligible(HermesSkillLineageRepairAdapterDispatchRequest request) {
        return request.mutationRequested() && !request.idempotencyKey().isBlank();
    }

    private HermesSkillLineageRepairAdapterDispatch withLedgerMetadata(
            HermesSkillLineageRepairAdapterDispatch dispatch,
            HermesSkillLineageRepairAdapterDispatchRequest request,
            String ledgerStatus) {
        Map<String, Object> metadata = new LinkedHashMap<>(dispatch.metadata());
        metadata.put("ledgerStatus", HermesText.oneLine(ledgerStatus));
        metadata.put("ledgerReplay", "replayed".equals(ledgerStatus));
        metadata.put("idempotencyKey", request.idempotencyKey());
        metadata.put("dispatchLedger", dispatchLedger.toMetadata());
        return new HermesSkillLineageRepairAdapterDispatch(
                dispatch.action(),
                dispatch.dispatchStatus(),
                dispatch.batchCount(),
                dispatch.dispatchedBatchCount(),
                dispatch.successfulBatchCount(),
                dispatch.failedBatchCount(),
                dispatch.unsupportedBatchCount(),
                dispatch.results(),
                metadata);
    }

    public static final class Builder {
        private final java.util.ArrayList<HermesSkillLineageRepairAdapter> adapters = new java.util.ArrayList<>();
        private HermesSkillLineageRepairAdapterDispatchLedger dispatchLedger =
                HermesSkillLineageRepairAdapterDispatchLedger.noop();

        public Builder register(HermesSkillLineageRepairAdapter adapter) {
            if (adapter != null) {
                adapters.add(adapter);
            }
            return this;
        }

        public Builder dispatchLedger(HermesSkillLineageRepairAdapterDispatchLedger value) {
            this.dispatchLedger = value == null
                    ? HermesSkillLineageRepairAdapterDispatchLedger.noop()
                    : value;
            return this;
        }

        public Builder dispatchLedger(HermesAgentModeConfig config) {
            return dispatchLedger(HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(config));
        }

        public Builder dispatchLedger(
                HermesAgentModeConfig config,
                Optional<ObjectStorageService> objectStorageService) {
            return dispatchLedger(config, objectStorageService, Optional.empty());
        }

        public Builder dispatchLedger(
                HermesAgentModeConfig config,
                Optional<ObjectStorageService> objectStorageService,
                Optional<DataSource> dataSource) {
            return dispatchLedger(HermesSkillLineageRepairAdapterDispatchLedgerResolver.resolve(
                    config,
                    objectStorageService,
                    dataSource));
        }

        public HermesSkillLineageRepairAdapterRegistry build() {
            return new HermesSkillLineageRepairAdapterRegistry(adapters, dispatchLedger);
        }
    }
}
