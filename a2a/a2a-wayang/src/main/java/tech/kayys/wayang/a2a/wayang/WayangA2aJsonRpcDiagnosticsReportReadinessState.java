package tech.kayys.wayang.a2a.wayang;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

record WayangA2aJsonRpcDiagnosticsReportReadinessState(
        boolean bindingReportPassed,
        boolean routeCatalogRequired,
        boolean routeCatalogPassed,
        boolean smokeRequired,
        boolean smokePassed) {

    static WayangA2aJsonRpcDiagnosticsReportReadinessState from(
            WayangA2aJsonRpcReadinessProbeResult readiness) {
        WayangA2aJsonRpcReadinessProbeResult resolved = Objects.requireNonNull(readiness, "readiness");
        return new WayangA2aJsonRpcDiagnosticsReportReadinessState(
                resolved.bindingReportPassed(),
                resolved.routeCatalogRequired(),
                resolved.routeCatalogPassed(),
                resolved.smokeRequired(),
                resolved.smokePassed());
    }

    Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("bindingReportPassed", bindingReportPassed);
        values.put("routeCatalogRequired", routeCatalogRequired);
        values.put("routeCatalogPassed", routeCatalogPassed);
        values.put("smokeRequired", smokeRequired);
        values.put("smokePassed", smokePassed);
        return WayangA2aMaps.copyMap(values);
    }
}
