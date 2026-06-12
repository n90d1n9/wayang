package tech.kayys.wayang.gollek.sdk;

import tech.kayys.wayang.agent.spi.AgentRequest;

public final class AgentRunPlanner {

    private final WayangGollekSdkConfig config;
    private final WayangAgentRequestMapper requestMapper;
    private final LocalWorkspaceInspector workspaceInspector;
    private final LocalHarnessPlanner harnessPlanner;
    private final SkillRegistry skillRegistry;

    public AgentRunPlanner() {
        this(WayangGollekSdkConfig.local());
    }

    public AgentRunPlanner(WayangGollekSdkConfig config) {
        this(
                config,
                new WayangAgentRequestMapper(),
                new LocalWorkspaceInspector(),
                null,
                WayangSkillCatalog.defaultRegistry());
    }

    AgentRunPlanner(
            WayangGollekSdkConfig config,
            WayangAgentRequestMapper requestMapper,
            LocalWorkspaceInspector workspaceInspector,
            LocalHarnessPlanner harnessPlanner,
            SkillRegistry skillRegistry) {
        this.config = config == null ? WayangGollekSdkConfig.local() : config;
        this.requestMapper = requestMapper == null ? new WayangAgentRequestMapper() : requestMapper;
        this.workspaceInspector = workspaceInspector == null ? new LocalWorkspaceInspector() : workspaceInspector;
        this.harnessPlanner = harnessPlanner == null ? new LocalHarnessPlanner(this.workspaceInspector) : harnessPlanner;
        this.skillRegistry = skillRegistry == null ? WayangSkillCatalog.defaultRegistry() : skillRegistry;
    }

    public AgentRunPreparation prepare(AgentRunRequest request) {
        AgentRunRequest configured = applyDefaults(request);
        WorkspaceSnapshot workspace = workspaceSnapshot(configured);
        HarnessPlan harness = harnessPlan(configured);
        AgentRequest coreRequest = requestMapper.toAgentRequest(configured, workspace, harness);
        AgentRunReadiness readiness = assessReadiness(configured);
        return new AgentRunPreparation(configured, workspace, harness, coreRequest, readiness);
    }

    public AgentRunPreview preview(AgentRunRequest request) {
        return prepare(request).preview();
    }

    public AgentRunReadiness assessReadiness(AgentRunRequest request) {
        return AgentRunReadiness.assess(skillRegistry, applyDefaults(request));
    }

    public AgentRequest toCoreAgentRequest(AgentRunRequest request) {
        return prepare(request).coreRequest();
    }

    public AgentRunRequest applyDefaults(AgentRunRequest request) {
        AgentRunRequest normalized = request == null
                ? AgentRunRequest.builder()
                        .tenantId(config.defaultTenantId())
                        .modelId(config.defaultModelId())
                        .build()
                : AgentRunRequest.builder(request).build();
        String tenantId = "default".equals(normalized.tenantId()) && !"default".equals(config.defaultTenantId())
                ? config.defaultTenantId()
                : normalized.tenantId();
        String modelId = normalized.modelId().isBlank() ? config.defaultModelId() : normalized.modelId();
        String surfaceId = WayangProductCatalog.requireKnownSurfaceId(normalized.surfaceId());
        return AgentRunRequest.builder(normalized)
                .tenantId(tenantId)
                .modelId(modelId)
                .surfaceId(surfaceId)
                .build();
    }

    private WorkspaceSnapshot workspaceSnapshot(AgentRunRequest request) {
        if (!request.workspaceEnabled()) {
            return null;
        }
        return workspaceInspector.inspect(new WorkspaceInspectionRequest(
                request.workspacePath(),
                request.workspaceMaxEntries(),
                false));
    }

    private HarnessPlan harnessPlan(AgentRunRequest request) {
        if (!request.harnessEnabled()) {
            return null;
        }
        return harnessPlanner.plan(new HarnessPlanRequest(
                request.workspacePath(),
                request.harnessMaxChecks(),
                request.harnessIncludeOptional()));
    }
}
