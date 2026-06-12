package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record WayangA2aJsonRpcReadinessMethodDispatchSnapshot(
        boolean reported,
        boolean complete,
        int registeredMethodCount,
        int dispatchMethodCount,
        List<String> missingDispatchMethods,
        List<String> orphanDispatchMethods,
        List<Map<String, Object>> methodGroups) {

    WayangA2aJsonRpcReadinessMethodDispatchSnapshot {
        registeredMethodCount = Math.max(0, registeredMethodCount);
        dispatchMethodCount = Math.max(0, dispatchMethodCount);
        missingDispatchMethods = missingDispatchMethods == null ? List.of() : List.copyOf(missingDispatchMethods);
        orphanDispatchMethods = orphanDispatchMethods == null ? List.of() : List.copyOf(orphanDispatchMethods);
        methodGroups = WayangA2aJsonReportMaps.copyObjects(methodGroups);
    }

    static WayangA2aJsonRpcReadinessMethodDispatchSnapshot from(
            WayangA2aJsonRpcBindingReportProbeResult bindingReportProbe) {
        if (bindingReportProbe == null || !bindingReportProbe.methodDispatchReported()) {
            return unreported();
        }
        return new WayangA2aJsonRpcReadinessMethodDispatchSnapshot(
                true,
                bindingReportProbe.methodDispatchComplete(),
                bindingReportProbe.registeredMethodCount(),
                bindingReportProbe.dispatchMethodCount(),
                bindingReportProbe.missingDispatchMethods(),
                bindingReportProbe.orphanDispatchMethods(),
                bindingReportProbe.methodDispatchGroups());
    }

    static WayangA2aJsonRpcReadinessMethodDispatchSnapshot unreported() {
        return new WayangA2aJsonRpcReadinessMethodDispatchSnapshot(
                false,
                false,
                0,
                0,
                List.of(),
                List.of(),
                List.of());
    }

    boolean passed() {
        return !reported || complete;
    }

    Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("reported", reported);
        values.put("complete", complete);
        values.put("passed", passed());
        values.put("registeredMethodCount", registeredMethodCount);
        values.put("dispatchMethodCount", dispatchMethodCount);
        values.put("missingDispatchMethods", missingDispatchMethods);
        values.put("orphanDispatchMethods", orphanDispatchMethods);
        values.put("methodGroups", methodGroups);
        return WayangA2aMaps.copyMap(values);
    }
}
