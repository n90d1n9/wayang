package tech.kayys.wayang.agent.store;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;

/**
 * Result of applying backup retention to file-backed run-store compaction
 * backups.
 */
public record AgentRunStoreBackupRetentionResult(
        AgentRunStoreBackupRetentionPolicy policy,
        int retainedBackupCount,
        int prunedBackupCount,
        int failedBackupPruneCount,
        List<String> retainedBackupPaths,
        List<String> prunedBackupPaths,
        List<String> failedBackupPrunePaths) {

    public AgentRunStoreBackupRetentionResult(
            AgentRunStoreBackupRetentionPolicy policy,
            int retainedBackupCount,
            int prunedBackupCount,
            List<String> retainedBackupPaths,
            List<String> prunedBackupPaths) {
        this(policy, retainedBackupCount, prunedBackupCount, 0, retainedBackupPaths, prunedBackupPaths, List.of());
    }

    public AgentRunStoreBackupRetentionResult {
        policy = policy == null ? AgentRunStoreBackupRetentionPolicy.defaults() : policy;
        retainedBackupCount = Math.max(0, retainedBackupCount);
        prunedBackupCount = Math.max(0, prunedBackupCount);
        retainedBackupPaths = retainedBackupPaths == null ? List.of() : List.copyOf(retainedBackupPaths);
        prunedBackupPaths = prunedBackupPaths == null ? List.of() : List.copyOf(prunedBackupPaths);
        failedBackupPrunePaths = failedBackupPrunePaths == null ? List.of() : List.copyOf(failedBackupPrunePaths);
        failedBackupPruneCount = Math.max(0, Math.max(failedBackupPruneCount, failedBackupPrunePaths.size()));
    }

    public static AgentRunStoreBackupRetentionResult empty(AgentRunStoreBackupRetentionPolicy policy) {
        return new AgentRunStoreBackupRetentionResult(policy, 0, 0, 0, List.of(), List.of(), List.of());
    }

    public static AgentRunStoreBackupRetentionResult preview(
            AgentRunStoreBackupRetentionPolicy policy,
            AgentRunStoreBackupInventory inventory) {
        AgentRunStoreBackupRetentionPolicy resolvedPolicy = policy == null
                ? AgentRunStoreBackupRetentionPolicy.defaults()
                : policy;
        AgentRunStoreBackupInventory resolvedInventory = inventory == null
                ? AgentRunStoreBackupInventory.empty()
                : inventory;
        List<String> backups = resolvedInventory.backupPaths();
        if (backups.isEmpty()) {
            return empty(resolvedPolicy);
        }
        if (resolvedPolicy.isUnlimited()) {
            return new AgentRunStoreBackupRetentionResult(
                    resolvedPolicy,
                    backups.size(),
                    0,
                    0,
                    backups,
                    List.of(),
                    List.of());
        }
        int keep = Math.min(resolvedPolicy.maxBackups(), backups.size());
        List<String> retained = backups.subList(0, keep);
        List<String> pruned = backups.subList(keep, backups.size());
        return new AgentRunStoreBackupRetentionResult(
                resolvedPolicy,
                retained.size(),
                pruned.size(),
                0,
                retained,
                pruned,
                List.of());
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("policy", policy.toMap());
        values.put("retainedBackupCount", retainedBackupCount);
        values.put("prunedBackupCount", prunedBackupCount);
        values.put("failedBackupPruneCount", failedBackupPruneCount);
        values.put("retainedBackupPaths", retainedBackupPaths);
        values.put("prunedBackupPaths", prunedBackupPaths);
        values.put("failedBackupPrunePaths", failedBackupPrunePaths);
        return SdkMaps.copy(values);
    }
}
