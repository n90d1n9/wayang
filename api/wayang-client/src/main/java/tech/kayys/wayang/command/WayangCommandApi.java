package tech.kayys.wayang.command;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.Wayang;
import tech.kayys.wayang.client.WayangGollekSdk;
import tech.kayys.wayang.client.WayangWireApi;
import tech.kayys.wayang.client.WayangWorkbenchModel;
import tech.kayys.wayang.workbench.WorkbenchCommand;
import tech.kayys.wayang.workbench.WorkbenchCommandDiscovery;
import tech.kayys.wayang.workbench.WorkbenchCommandEnvelopes;
import tech.kayys.wayang.workbench.WorkbenchCommandQuery;

/**
 * Command discovery API for workbench, command palette, and wrapper integrations.
 *
 * <p>The API delegates discovery to the configured SDK instance and owns the
 * command JSON envelope boundary through {@link WorkbenchCommandEnvelopes}.</p>
 */
public final class WayangCommandApi {

    private final WayangGollekSdk sdk;
    private final WayangWireApi wire;

    public WayangCommandApi(WayangGollekSdk sdk, WayangWireApi wire) {
        this.sdk = sdk == null ? Wayang.local() : sdk;
        this.wire = wire == null ? new WayangWireApi() : wire;
    }

    public WayangWorkbenchModel workbench(WorkbenchCommandQuery query) {
        return sdk.workbench(query);
    }

    public WorkbenchCommandDiscovery discover(WorkbenchCommandQuery query) {
        return sdk.commandDiscovery(query);
    }

    public WorkbenchCommandDiscovery discover(String surfaceId, String category, String commandId) {
        return discover(WorkbenchCommandQuery.of(surfaceId, category, commandId));
    }

    public List<WorkbenchCommand> list(WorkbenchCommandQuery query) {
        return discover(query).commands();
    }

    public Map<String, Object> discoveryEnvelope(WorkbenchCommandQuery query) {
        return discoveryEnvelope(discover(query));
    }

    public Map<String, Object> discoveryEnvelope(WorkbenchCommandDiscovery discovery) {
        return WorkbenchCommandEnvelopes.discovery(productName(), discovery);
    }

    public Map<String, Object> indexEnvelope(WorkbenchCommandQuery query) {
        return indexEnvelope(discover(query));
    }

    public Map<String, Object> indexEnvelope(WorkbenchCommandDiscovery discovery) {
        return WorkbenchCommandEnvelopes.index(productName(), discovery);
    }

    public Map<String, Object> workbenchEnvelope(WorkbenchCommandQuery query) {
        return workbenchEnvelope(workbench(query), query);
    }

    public Map<String, Object> workbenchEnvelope(WayangWorkbenchModel model, WorkbenchCommandQuery query) {
        return WorkbenchCommandEnvelopes.workbench(model, sdk.productSurfacePolicies(), query);
    }

    public String discoveryJson(WorkbenchCommandQuery query) {
        return discoveryJson(discover(query));
    }

    public String discoveryJson(WorkbenchCommandDiscovery discovery) {
        return wire.object(discoveryEnvelope(discovery));
    }

    public String indexJson(WorkbenchCommandQuery query) {
        return indexJson(discover(query));
    }

    public String indexJson(WorkbenchCommandDiscovery discovery) {
        return wire.object(indexEnvelope(discovery));
    }

    public String workbenchJson(WorkbenchCommandQuery query) {
        return workbenchJson(workbench(query), query);
    }

    public String workbenchJson(WayangWorkbenchModel model, WorkbenchCommandQuery query) {
        return wire.object(workbenchEnvelope(model, query));
    }

    private String productName() {
        return sdk.status().productName();
    }
}
