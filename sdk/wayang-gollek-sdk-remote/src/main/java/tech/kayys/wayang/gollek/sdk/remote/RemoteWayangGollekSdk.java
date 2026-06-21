package tech.kayys.wayang.gollek.sdk.remote;

import tech.kayys.wayang.gollek.sdk.AgentRunRequest;
import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunForgetResult;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunPreview;
import tech.kayys.wayang.gollek.sdk.AgentRunReadiness;
import tech.kayys.wayang.gollek.sdk.AgentRunResult;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;
import tech.kayys.wayang.gollek.sdk.AgentSkillDiscovery;
import tech.kayys.wayang.gollek.sdk.AgentSkillQuery;
import tech.kayys.wayang.gollek.sdk.HarnessPlan;
import tech.kayys.wayang.gollek.sdk.HarnessPlanRequest;
import tech.kayys.wayang.gollek.sdk.ProductSurface;
import tech.kayys.wayang.gollek.sdk.RegisteredSkill;
import tech.kayys.wayang.gollek.sdk.SkillRegistry;
import tech.kayys.wayang.gollek.sdk.WayangCommandDiscoveryService;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformStatus;
import tech.kayys.wayang.gollek.sdk.WayangSkillCatalog;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandDiscovery;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommandQuery;
import tech.kayys.wayang.gollek.sdk.WorkspaceInspectionRequest;
import tech.kayys.wayang.gollek.sdk.WorkspaceSnapshot;

import java.util.List;

public final class RemoteWayangGollekSdk implements WayangGollekSdk {

    private static final WayangCommandDiscoveryService COMMAND_DISCOVERY = WayangCommandDiscoveryService.create();

    private final WayangRemoteTransport transport;

    public RemoteWayangGollekSdk(WayangGollekSdkConfig config) {
        this(new HttpWayangRemoteTransport(config));
    }

    public RemoteWayangGollekSdk(WayangRemoteTransport transport) {
        this.transport = transport;
    }

    @Override
    public WayangPlatformStatus status() {
        return transport.status();
    }

    @Override
    public List<ProductSurface> productSurfaces() {
        return transport.productSurfaces();
    }

    @Override
    public WayangWorkbenchModel workbench() {
        return transport.workbench();
    }

    @Override
    public WayangWorkbenchModel workbench(WorkbenchCommandQuery query) {
        return COMMAND_DISCOVERY.filterWorkbench(transport.workbench(), query);
    }

    @Override
    public WorkbenchCommandDiscovery commandDiscovery(WorkbenchCommandQuery query) {
        return COMMAND_DISCOVERY.commandDiscovery(transport.workbench(), query);
    }

    @Override
    public SkillRegistry skillRegistry() {
        return transport.skillRegistry();
    }

    @Override
    public AgentRunReadiness assessRunReadiness(AgentRunRequest request) {
        SkillRegistry registry;
        try {
            registry = transport.skillRegistry();
        } catch (RemoteWayangGollekException e) {
            registry = WayangSkillCatalog.defaultRegistry();
        }
        return AgentRunReadiness.assess(registry, request);
    }

    @Override
    public RegisteredSkill skill(String skillId) {
        return transport.skill(skillId);
    }

    @Override
    public AgentSkillDiscovery skillDiscovery(AgentSkillQuery query, String search) {
        return transport.skillDiscovery(query, search);
    }

    @Override
    public WorkspaceSnapshot inspectWorkspace(WorkspaceInspectionRequest request) {
        return transport.inspectWorkspace(request);
    }

    @Override
    public HarnessPlan planHarness(HarnessPlanRequest request) {
        return transport.planHarness(request);
    }

    @Override
    public AgentRunPreview previewRun(AgentRunRequest request) {
        return transport.previewRun(request);
    }

    @Override
    public AgentRunResult run(AgentRunRequest request) {
        return transport.run(request);
    }

    @Override
    public AgentRunStatus runStatus(String runId) {
        return transport.runStatus(runId);
    }

    @Override
    public AgentRunHistory runHistory(AgentRunHistoryQuery query) {
        return transport.runHistory(query);
    }

    @Override
    public AgentRunEvents runEvents(String runId) {
        return runEvents(runId, AgentRunEventsQuery.all());
    }

    @Override
    public AgentRunEvents runEvents(String runId, AgentRunEventsQuery query) {
        return transport.runEvents(runId, query);
    }

    @Override
    public AgentRunCancelResult cancelRun(String runId, String reason) {
        return transport.cancelRun(runId, reason);
    }

    @Override
    public AgentRunForgetResult forgetRun(String runId) {
        return transport.forgetRun(runId);
    }

    @Override
    public void setPreferredProvider(String providerId) {
        throw new UnsupportedOperationException("Remote SDK does not support setting preferred provider yet.");
    }

    @Override
    public java.util.Optional<String> getPreferredProvider() {
        return java.util.Optional.empty();
    }

    @Override
    public List<String> listAvailableProviders() {
        return List.of();
    }
}
