package tech.kayys.wayang.gollek.sdk;

import java.util.Properties;

/**
 * Non-mutating read result for one run-store snapshot properties file.
 */
record AgentRunStoreSnapshotFileInspection(
        boolean exists,
        boolean readable,
        Properties properties,
        String errorMessage) {

    AgentRunStoreSnapshotFileInspection {
        Properties copy = new Properties();
        if (properties != null) {
            copy.putAll(properties);
        }
        properties = copy;
        errorMessage = SdkText.trimToEmpty(errorMessage);
    }

    static AgentRunStoreSnapshotFileInspection missing() {
        return new AgentRunStoreSnapshotFileInspection(false, true, new Properties(), "");
    }

    static AgentRunStoreSnapshotFileInspection readable(Properties properties) {
        return new AgentRunStoreSnapshotFileInspection(true, true, properties, "");
    }

    static AgentRunStoreSnapshotFileInspection unreadable(Exception cause) {
        String message = cause == null ? "Snapshot could not be read." : cause.getMessage();
        return new AgentRunStoreSnapshotFileInspection(true, false, new Properties(), message);
    }
}
