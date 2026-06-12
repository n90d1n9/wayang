package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bodyMap;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.bool;
import static tech.kayys.wayang.a2a.wayang.WayangA2aJsonReportMaps.map;

/**
 * Composite operational readiness result for the A2A JSON-RPC HTTP surface.
 */
public record WayangA2aJsonRpcReadinessProbeResult(
        WayangA2aJsonRpcBindingReportProbeResult bindingReportProbe,
        WayangA2aJsonRpcRouteCatalogProbeResult routeCatalogProbe,
        boolean routeCatalogRequired,
        WayangA2aJsonRpcSmokeProbeResult smokeProbe,
        boolean smokeRequired) {

    public static final String READINESS_ID = "a2a.jsonrpc.readiness";
    public static final String OPERATION_JSON_RPC_READINESS = "JsonRpcReadiness";

    public WayangA2aJsonRpcReadinessProbeResult {
        bindingReportProbe = Objects.requireNonNull(bindingReportProbe, "bindingReportProbe");
        routeCatalogProbe = routeCatalogProbe == null
                ? WayangA2aJsonRpcReadinessProbePlaceholders.disabledRouteCatalogProbe()
                : routeCatalogProbe;
        smokeProbe = smokeProbe == null
                ? WayangA2aJsonRpcReadinessProbePlaceholders.disabledSmokeProbe()
                : smokeProbe;
    }

    public static WayangA2aJsonRpcReadinessProbeResult run(WayangA2aJsonRpcHttpAdapter adapter) {
        WayangA2aJsonRpcHttpAdapter resolved = Objects.requireNonNull(adapter, "adapter");
        WayangA2aJsonRpcBindingReportProbeResult bindingReportProbe = resolved.bindingReportProbe();
        boolean routeCatalogRequired = resolved.config().routeCatalogEnabled();
        WayangA2aJsonRpcRouteCatalogProbeResult routeCatalogProbe = routeCatalogRequired
                ? resolved.routeCatalogProbe()
                : WayangA2aJsonRpcReadinessProbePlaceholders.disabledRouteCatalogProbe();
        boolean smokeRequired = bindingReportProbe.smokeEnabled();
        WayangA2aJsonRpcSmokeProbeResult smokeProbe = smokeRequired
                ? resolved.smokeProbe()
                : WayangA2aJsonRpcReadinessProbePlaceholders.disabledSmokeProbe();
        return new WayangA2aJsonRpcReadinessProbeResult(
                bindingReportProbe,
                routeCatalogProbe,
                routeCatalogRequired,
                smokeProbe,
                smokeRequired);
    }

    public static WayangA2aJsonRpcReadinessProbeResult from(WayangA2aHttpResponse response) {
        return fromJson(Objects.requireNonNull(response, "response").body());
    }

    public static WayangA2aJsonRpcReadinessProbeResult fromJson(String json) {
        return fromMap(bodyMap(json));
    }

    public static WayangA2aJsonRpcReadinessProbeResult fromMap(Map<?, ?> values) {
        Map<String, Object> copy = WayangA2aMaps.copyMap(values);
        return new WayangA2aJsonRpcReadinessProbeResult(
                WayangA2aJsonRpcBindingReportProbeResult.fromMap(
                        map(copy.get("bindingReportProbe"))),
                WayangA2aJsonRpcRouteCatalogProbeResult.fromMap(map(copy.get("routeCatalogProbe"))),
                bool(copy.get("routeCatalogRequired"), false),
                WayangA2aJsonRpcSmokeProbeResult.fromMap(map(copy.get("smokeProbe"))),
                bool(copy.get("smokeRequired"), false));
    }

    public boolean bindingReportPassed() {
        return bindingReportProbe.passed();
    }

    public boolean routeCatalogPassed() {
        return !routeCatalogRequired || routeCatalogProbe.passed();
    }

    public boolean smokePassed() {
        return !smokeRequired || smokeProbe.passed();
    }

    public boolean methodDispatchReported() {
        return methodDispatchSnapshot().reported();
    }

    public boolean methodDispatchPassed() {
        return methodDispatchSnapshot().passed();
    }

    public boolean methodRegistryReported() {
        return methodRegistrySnapshot().reported();
    }

    public boolean methodRegistryPassed() {
        return !methodRegistryReported() || methodRegistrySnapshot().groupCount() > 0;
    }

    public WayangA2aJsonRpcMethodHandlerRegistrySnapshot methodRegistrySnapshot() {
        return bindingReportProbe.methodRegistrySnapshot();
    }

    public boolean passed() {
        return bindingReportPassed() && routeCatalogPassed() && smokePassed();
    }

    public int exitCode() {
        return passed()
                ? WayangA2aJsonRpcSmokeResult.EXIT_SUCCESS
                : WayangA2aJsonRpcSmokeResult.EXIT_FAILURE;
    }

    public List<Map<String, Object>> issues() {
        return WayangA2aJsonRpcReadinessProbeCheck.issues(this);
    }

    public int issueCount() {
        return issues().size();
    }

    public Map<String, Object> toMap() {
        return WayangA2aJsonRpcReadinessProbeProjection.probe(this);
    }

    public WayangReadinessReport standardReadiness() {
        return WayangA2aJsonRpcReadinessProbeProjection.standardReadiness(this);
    }

    public String toJson() {
        return WayangA2aHttpJson.write(toMap());
    }

    public WayangA2aHttpResponse response() {
        return WayangA2aJsonRpcReadinessProbeProjection.response(this);
    }

    private WayangA2aJsonRpcReadinessMethodDispatchSnapshot methodDispatchSnapshot() {
        return WayangA2aJsonRpcReadinessMethodDispatchSnapshot.from(bindingReportProbe);
    }

}
