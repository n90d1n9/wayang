package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Approval store that checks a primary store and keeps a fallback store usable.
 */
public final class HybridHermesSkillLineageRepairApprovalStore
        implements HermesSkillLineageRepairApprovalStore {

    private final HermesSkillLineageRepairApprovalStore primaryStore;
    private final HermesSkillLineageRepairApprovalStore fallbackStore;

    public HybridHermesSkillLineageRepairApprovalStore(
            HermesSkillLineageRepairApprovalStore primaryStore,
            HermesSkillLineageRepairApprovalStore fallbackStore) {
        this.primaryStore = primaryStore == null ? HermesSkillLineageRepairApprovalStore.noop() : primaryStore;
        this.fallbackStore = fallbackStore == null ? HermesSkillLineageRepairApprovalStore.noop() : fallbackStore;
    }

    @Override
    public Optional<HermesSkillLineageRepairApproval> find(String approvalId) {
        return primaryStore.find(approvalId).or(() -> fallbackStore.find(approvalId));
    }

    @Override
    public HermesSkillLineageRepairApprovalDecision authorize(
            HermesSkillLineageRepairAdapterDispatchRequest request) {
        if (request == null || !request.mutationRequested()) {
            return HermesSkillLineageRepairApprovalDecision.notRequired(request);
        }
        if (!request.approvalSatisfied()) {
            return HermesSkillLineageRepairApprovalDecision.rejected(
                    "missing-request-approval",
                    "repair mutation requires an approved approval id",
                    request);
        }

        boolean approvalFound = false;
        for (Optional<HermesSkillLineageRepairApproval> candidate : java.util.List.of(
                primaryStore.find(request.approvalId()),
                fallbackStore.find(request.approvalId()))) {
            if (candidate.isEmpty()) {
                continue;
            }
            HermesSkillLineageRepairApproval approval = candidate.orElseThrow();
            approvalFound = true;
            if (approval.matches(request)) {
                return HermesSkillLineageRepairApprovalDecision.approved(approval, request);
            }
        }
        if (approvalFound) {
            return HermesSkillLineageRepairApprovalDecision.rejected(
                    "scope-mismatch",
                    "repair approval does not match dispatch request",
                    request);
        }
        return HermesSkillLineageRepairApprovalDecision.rejected(
                "missing",
                "repair approval not found",
                request);
    }

    @Override
    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("storeType", "hybrid");
        metadata.put("configured", true);
        metadata.put("primaryStore", primaryStore.toMetadata());
        metadata.put("fallbackStore", fallbackStore.toMetadata());
        return Map.copyOf(metadata);
    }
}
