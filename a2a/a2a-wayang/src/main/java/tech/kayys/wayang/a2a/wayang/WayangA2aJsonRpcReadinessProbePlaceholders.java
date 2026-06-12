package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

final class WayangA2aJsonRpcReadinessProbePlaceholders {

    private WayangA2aJsonRpcReadinessProbePlaceholders() {
    }

    static WayangA2aJsonRpcSmokeProbeResult disabledSmokeProbe() {
        return new WayangA2aJsonRpcSmokeProbeResult(
                0,
                false,
                "",
                "",
                "",
                WayangA2aJsonRpcSmokeSummary.fromMap(Map.of()),
                Map.of());
    }

    static WayangA2aJsonRpcRouteCatalogProbeResult disabledRouteCatalogProbe() {
        return new WayangA2aJsonRpcRouteCatalogProbeResult(
                0,
                false,
                "",
                "",
                "",
                "",
                0,
                0,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                false,
                0,
                List.of(),
                Map.of(),
                Map.of());
    }
}
