package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared pass/fail status for adapter diagnostics surfaces.
 */
public record WayangDiagnosticsStatus(
        boolean passed,
        int exitCode) {

    public WayangDiagnosticsStatus {
        exitCode = Math.max(0, exitCode);
    }

    public static WayangDiagnosticsStatus from(boolean passed, int exitCode) {
        return new WayangDiagnosticsStatus(passed, exitCode);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", passed);
        values.put("exitCode", exitCode);
        return SdkMaps.copy(values);
    }
}
