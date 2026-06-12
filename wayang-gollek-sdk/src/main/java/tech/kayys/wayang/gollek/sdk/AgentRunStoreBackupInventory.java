package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Operator inventory of file-backed run-store compaction backups.
 *
 * <p>Backup paths are ordered newest first so dashboards, CLIs, and recovery
 * workflows can prefer the newest rollback snapshot without repeating
 * filesystem sorting rules.</p>
 */
public record AgentRunStoreBackupInventory(List<String> backupPaths) {

    public AgentRunStoreBackupInventory {
        backupPaths = backupPaths == null
                ? List.of()
                : backupPaths.stream()
                        .map(SdkText::trimToEmpty)
                        .filter(path -> !path.isBlank())
                        .toList();
    }

    public static AgentRunStoreBackupInventory empty() {
        return new AgentRunStoreBackupInventory(List.of());
    }

    public int backupCount() {
        return backupPaths.size();
    }

    public String latestBackupPath() {
        return backupPaths.isEmpty() ? "" : backupPaths.get(0);
    }

    public String oldestBackupPath() {
        return backupPaths.isEmpty() ? "" : backupPaths.get(backupPaths.size() - 1);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("backupCount", backupCount());
        values.put("latestBackupPath", latestBackupPath());
        values.put("oldestBackupPath", oldestBackupPath());
        values.put("backupPaths", backupPaths);
        return SdkMaps.copy(values);
    }
}
