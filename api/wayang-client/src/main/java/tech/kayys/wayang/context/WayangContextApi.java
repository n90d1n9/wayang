package tech.kayys.wayang.context;

import java.util.Map;

import tech.kayys.wayang.client.Wayang;
import tech.kayys.wayang.client.WayangGollekSdk;
import tech.kayys.wayang.client.WayangWireApi;
import tech.kayys.wayang.client.WorkspaceInspectionRequest;
import tech.kayys.wayang.client.WorkspaceSnapshot;
import tech.kayys.wayang.harness.HarnessPlan;
import tech.kayys.wayang.harness.HarnessPlanRequest;

/**
 * Context engineering API for workspace inspection and harness planning surfaces.
 *
 * <p>This facade keeps workspace snapshots, verification check planning, and
 * JSON envelope rendering in the SDK so agent products can attach consistent
 * context to coding-agent and automation runs.</p>
 */
public final class WayangContextApi {

    private final WayangGollekSdk sdk;
    private final WayangWireApi wire;

    public WayangContextApi(WayangGollekSdk sdk, WayangWireApi wire) {
        this.sdk = sdk == null ? Wayang.local() : sdk;
        this.wire = wire == null ? new WayangWireApi() : wire;
    }

    public WorkspaceSnapshot workspace(WorkspaceInspectionRequest request) {
        return sdk.inspectWorkspace(request);
    }

    public WorkspaceSnapshot workspace(String rootPath, int maxEntries, boolean includeHidden) {
        return workspace(new WorkspaceInspectionRequest(rootPath, maxEntries, includeHidden));
    }

    public HarnessPlan harness(HarnessPlanRequest request) {
        return sdk.planHarness(request);
    }

    public HarnessPlan harness(String rootPath, int maxChecks, boolean includeOptional) {
        return harness(new HarnessPlanRequest(rootPath, maxChecks, includeOptional));
    }

    public Map<String, Object> workspaceEnvelope(WorkspaceSnapshot snapshot) {
        return WayangContextEnvelopes.workspace(snapshot);
    }

    public Map<String, Object> harnessEnvelope(HarnessPlan plan) {
        return WayangContextEnvelopes.harness(plan);
    }

    public String workspaceJson(WorkspaceSnapshot snapshot) {
        return wire.object(workspaceEnvelope(snapshot));
    }

    public String harnessJson(HarnessPlan plan) {
        return wire.object(harnessEnvelope(plan));
    }
}
