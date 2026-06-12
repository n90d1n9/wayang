package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Operator diagnostics for the active agent run-store backend and snapshot.
 */
public record AgentRunStoreDiagnostics(
        String backend,
        boolean persistent,
        String path,
        String lockPath,
        boolean snapshotPresent,
        boolean lockPresent,
        String snapshotVersion,
        boolean unsupportedSnapshotVersion,
        int runCount,
        int statusCount,
        int eventCount,
        AgentRunStoreRetentionPolicy retentionPolicy,
        AgentRunStoreRetentionAssessment retentionAssessment,
        AgentRunStoreBackupRetentionPolicy backupRetentionPolicy,
        AgentRunStoreBackupInventory backupInventory) {

    public AgentRunStoreDiagnostics {
        backend = SdkText.trimToDefault(backend, "memory");
        path = SdkText.trimToEmpty(path);
        lockPath = SdkText.trimToEmpty(lockPath);
        snapshotVersion = SdkText.trimToEmpty(snapshotVersion);
        runCount = Math.max(0, runCount);
        statusCount = Math.max(0, statusCount);
        eventCount = Math.max(0, eventCount);
        retentionPolicy = retentionPolicy == null ? AgentRunStoreRetentionPolicy.defaults() : retentionPolicy;
        retentionAssessment = retentionAssessment == null
                ? AgentRunStoreRetention.assess(AgentRunStoreSnapshot.empty(), retentionPolicy)
                : retentionAssessment;
        backupRetentionPolicy = backupRetentionPolicy == null
                ? AgentRunStoreBackupRetentionPolicy.defaults()
                : backupRetentionPolicy;
        backupInventory = backupInventory == null ? AgentRunStoreBackupInventory.empty() : backupInventory;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("backend", backend);
        values.put("persistent", persistent);
        values.put("path", path);
        values.put("lockPath", lockPath);
        values.put("snapshotPresent", snapshotPresent);
        values.put("lockPresent", lockPresent);
        values.put("snapshotVersion", snapshotVersion);
        values.put("unsupportedSnapshotVersion", unsupportedSnapshotVersion);
        values.put("runCount", runCount);
        values.put("statusCount", statusCount);
        values.put("eventCount", eventCount);
        values.put("retentionPolicy", retentionPolicy.toMap());
        values.put("retentionAssessment", retentionAssessment.toMap());
        values.put("backupRetentionPolicy", backupRetentionPolicy.toMap());
        values.put("backupInventory", backupInventory.toMap());
        return SdkMaps.copy(values);
    }
}
