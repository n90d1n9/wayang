package tech.kayys.wayang.agent.hermes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Process-local approval store for tests and embedded Hermes deployments.
 */
public final class InMemoryHermesSkillLineageRepairApprovalStore
        implements HermesSkillLineageRepairApprovalStore {

    private final Map<String, HermesSkillLineageRepairApproval> approvals;

    public InMemoryHermesSkillLineageRepairApprovalStore(
            List<HermesSkillLineageRepairApproval> approvals) {
        Map<String, HermesSkillLineageRepairApproval> values = new LinkedHashMap<>();
        if (approvals != null) {
            approvals.stream()
                    .filter(approval -> approval != null && !approval.approvalId().isBlank())
                    .forEach(approval -> values.put(approval.approvalId(), approval));
        }
        this.approvals = Map.copyOf(values);
    }

    @Override
    public Optional<HermesSkillLineageRepairApproval> find(String approvalId) {
        return Optional.ofNullable(approvals.get(approvalId == null ? "" : approvalId));
    }

    public int approvalCount() {
        return approvals.size();
    }

    @Override
    public Map<String, Object> toMetadata() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("storeType", "in-memory");
        metadata.put("configured", true);
        metadata.put("approvalCount", approvalCount());
        metadata.put("approvalIds", List.copyOf(approvals.keySet()));
        return Map.copyOf(metadata);
    }
}
