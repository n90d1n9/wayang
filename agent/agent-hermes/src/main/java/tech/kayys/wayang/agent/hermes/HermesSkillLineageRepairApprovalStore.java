package tech.kayys.wayang.agent.hermes;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Approval lookup boundary for lineage repair mutations.
 */
public interface HermesSkillLineageRepairApprovalStore {

    Optional<HermesSkillLineageRepairApproval> find(String approvalId);

    default HermesSkillLineageRepairApprovalDecision authorize(
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
        Optional<HermesSkillLineageRepairApproval> approval = find(request.approvalId());
        if (approval.isEmpty()) {
            return HermesSkillLineageRepairApprovalDecision.rejected(
                    "missing",
                    "repair approval not found",
                    request);
        }
        if (!approval.orElseThrow().matches(request)) {
            return HermesSkillLineageRepairApprovalDecision.rejected(
                    "scope-mismatch",
                    "repair approval does not match dispatch request",
                    request);
        }
        return HermesSkillLineageRepairApprovalDecision.approved(approval.orElseThrow(), request);
    }

    Map<String, Object> toMetadata();

    static HermesSkillLineageRepairApprovalStore noop() {
        return new HermesSkillLineageRepairApprovalStore() {
            @Override
            public HermesSkillLineageRepairApprovalDecision authorize(
                    HermesSkillLineageRepairAdapterDispatchRequest request) {
                if (request == null || !request.mutationRequested()) {
                    return HermesSkillLineageRepairApprovalDecision.notRequired(request);
                }
                return HermesSkillLineageRepairApprovalDecision.rejected(
                        "not-configured",
                        "repair approval store not configured",
                        request);
            }

            @Override
            public Optional<HermesSkillLineageRepairApproval> find(String approvalId) {
                return Optional.empty();
            }

            @Override
            public Map<String, Object> toMetadata() {
                return Map.of(
                        "storeType", "noop",
                        "configured", false,
                        "approvalCount", 0);
            }
        };
    }

    static HermesSkillLineageRepairApprovalStore inMemory(
            List<HermesSkillLineageRepairApproval> approvals) {
        return new InMemoryHermesSkillLineageRepairApprovalStore(approvals);
    }
}
