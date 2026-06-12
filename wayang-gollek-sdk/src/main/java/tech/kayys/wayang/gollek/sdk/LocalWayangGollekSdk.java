package tech.kayys.wayang.gollek.sdk;

import tech.kayys.wayang.agent.spi.AgentRequest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class LocalWayangGollekSdk implements WayangGollekSdk {

    private final WayangGollekSdkConfig config;
    private final WayangAgentRequestMapper requestMapper;
    private final LocalWorkspaceInspector workspaceInspector;
    private final LocalHarnessPlanner harnessPlanner;
    private final AgentRunLifecycleService runLifecycle;
    private final SkillRegistry skillRegistry;
    private final WayangProviderCapabilityRegistry providerCapabilityRegistry;
    private final AgentRunPlanner runPlanner;
    private final WayangPlatformReadinessProfileRegistry readinessProfileRegistry;

    public LocalWayangGollekSdk() {
        this(WayangGollekSdkConfig.local());
    }

    public LocalWayangGollekSdk(WayangGollekSdkConfig config) {
        this(config, new WayangAgentRequestMapper(), new LocalWorkspaceInspector());
    }

    public LocalWayangGollekSdk(
            WayangGollekSdkConfig config,
            WayangPlatformReadinessProfileObjectReader readinessProfileObjectReader) {
        this(
                config,
                WayangPlatformReadinessProfileExternalReaders.objectStorage(readinessProfileObjectReader));
    }

    public LocalWayangGollekSdk(
            WayangGollekSdkConfig config,
            WayangPlatformReadinessProfileExternalReaders readinessProfileReaders) {
        this(
                config,
                new WayangAgentRequestMapper(),
                new LocalWorkspaceInspector(),
                null,
                AgentRunStore.configured(config),
                readinessProfileReaders);
    }

    public LocalWayangGollekSdk(WayangGollekSdkConfig config, WayangAgentRequestMapper requestMapper) {
        this(config, requestMapper, new LocalWorkspaceInspector());
    }

    public LocalWayangGollekSdk(WayangGollekSdkConfig config, AgentRunStore runStore) {
        this(config, new WayangAgentRequestMapper(), new LocalWorkspaceInspector(), null, runStore);
    }

    LocalWayangGollekSdk(
            WayangGollekSdkConfig config,
            WayangAgentRequestMapper requestMapper,
            LocalWorkspaceInspector workspaceInspector) {
        this(config, requestMapper, workspaceInspector, null);
    }

    LocalWayangGollekSdk(
            WayangGollekSdkConfig config,
            WayangAgentRequestMapper requestMapper,
            LocalWorkspaceInspector workspaceInspector,
            LocalHarnessPlanner harnessPlanner) {
        this(
                config,
                requestMapper,
                workspaceInspector,
                harnessPlanner,
                AgentRunStore.configured(config));
    }

    LocalWayangGollekSdk(
            WayangGollekSdkConfig config,
            WayangAgentRequestMapper requestMapper,
            LocalWorkspaceInspector workspaceInspector,
            LocalHarnessPlanner harnessPlanner,
            AgentRunStore runStore) {
        this(
                config,
                requestMapper,
                workspaceInspector,
                harnessPlanner,
                runStore,
                WayangPlatformReadinessProfileExternalReaders.none());
    }

    LocalWayangGollekSdk(
            WayangGollekSdkConfig config,
            WayangAgentRequestMapper requestMapper,
            LocalWorkspaceInspector workspaceInspector,
            LocalHarnessPlanner harnessPlanner,
            AgentRunStore runStore,
            WayangPlatformReadinessProfileExternalReaders readinessProfileReaders) {
        this.config = config == null ? WayangGollekSdkConfig.local() : config;
        this.requestMapper = requestMapper == null ? new WayangAgentRequestMapper() : requestMapper;
        this.workspaceInspector = workspaceInspector == null ? new LocalWorkspaceInspector() : workspaceInspector;
        this.harnessPlanner = harnessPlanner == null ? new LocalHarnessPlanner(this.workspaceInspector) : harnessPlanner;
        this.runLifecycle = AgentRunLifecycleService.create(runStore);
        this.skillRegistry = WayangSkillCatalog.defaultRegistry();
        this.providerCapabilityRegistry = WayangProviderCapabilityCatalog.defaultRegistry();
        WayangPlatformReadinessProfileExternalReaders discoveredReaders =
                WayangPlatformReadinessProfileExternalReaderProviders.discover(this.config);
        WayangPlatformReadinessProfileExternalReaders resolvedReaders =
                WayangPlatformReadinessProfileExternalReaders.merge(
                        readinessProfileReaders,
                        discoveredReaders);
        this.readinessProfileRegistry =
                this.config.readinessProfileRegistry().registry(resolvedReaders);
        this.runPlanner = new AgentRunPlanner(
                this.config,
                this.requestMapper,
                this.workspaceInspector,
                this.harnessPlanner,
                this.skillRegistry);
    }

    @Override
    public WayangPlatformStatus status() {
        return new WayangPlatformStatus(
                "Wayang",
                "1.0.0-SNAPSHOT",
                new ComponentStatus(
                        "Gollek",
                        "Inference, serving, and training engine",
                        "external",
                        "configured by backend adapter",
                        80),
                new ComponentStatus(
                        "Gamelan",
                        "Workflow execution engine",
                        "external",
                        "configured by workflow backend",
                        80),
                new ComponentStatus(
                        "Agent Core",
                        "Agentic orchestration and skills",
                        "local",
                        "agent-spi + agent-core",
                        90),
                new ComponentStatus(
                        "RAG Runtime",
                        "Retrieval, plugins, and response assembly",
                        "local",
                        "rag-runtime",
                        90),
                new ComponentStatus(
                        "MCP",
                        "Dynamic tool bridge",
                        "adapter-ready",
                        "agent-mcp + wayang-tool-mcp",
                        70),
                availableSkillCount(),
                List.of(
                        "Wayang SDK is the shared source of truth for agent product surfaces.",
                        "Wayang CLI is product-owned and separate from Gollek CLI.",
                        "Tamboui is used as a renderer reference, not the platform contract.",
                        "The CLI target is an agent workbench for coding, assistant, and workflow products.",
                        "Next SDK adapters should wire local runtime, remote API, and packaged agents."));
    }

    @Override
    public List<ProductSurface> productSurfaces() {
        return WayangProductCatalog.defaultSurfaces();
    }

    @Override
    public SkillRegistry skillRegistry() {
        return skillRegistry;
    }

    @Override
    public WayangProviderCapabilityRegistry providerCapabilityRegistry() {
        return providerCapabilityRegistry;
    }

    @Override
    public WayangWorkbenchModel workbench() {
        return new WayangWorkbenchModel(
                status(),
                productSurfaces(),
                WayangWorkbenchCatalog.localCommandPalette(),
                WayangWorkbenchCatalog.localCommands(),
                List.of(
                        "Attach SDK run execution to the local Wayang agent runtime.",
                        "Use workspace context inspection to seed coding-agent products.",
                        "Expose MCP, RAG, memory, and workflow adapters through stable SDK and CLI commands.",
                        "Keep Tamboui as a renderer adapter behind the SDK workbench model."));
    }

    @Override
    public WorkspaceSnapshot inspectWorkspace(WorkspaceInspectionRequest request) {
        return workspaceInspector.inspect(request);
    }

    @Override
    public HarnessPlan planHarness(HarnessPlanRequest request) {
        return harnessPlanner.plan(request);
    }

    @Override
    public AgentRunPreview previewRun(AgentRunRequest request) {
        return runPlanner.preview(request);
    }

    @Override
    public AgentRunReadiness assessRunReadiness(AgentRunRequest request) {
        return runPlanner.assessReadiness(request);
    }

    @Override
    public WayangReadinessReport storageReadiness() {
        return WayangStorageReadiness.assess(config.storage());
    }

    @Override
    public WayangReadinessReport platformReadiness() {
        WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics =
                platformReadinessProfileRegistryConfigDiagnostics();
        if (!diagnostics.valid()) {
            return platformReadinessRegistryConfigBlocked(diagnostics);
        }
        WayangPlatformReadinessProfileRegistryResolution resolution = platformReadinessProfileRegistryResolution();
        if (!resolution.valid() || resolution.profiles().isEmpty()) {
            return platformReadinessRegistryBlocked(resolution);
        }
        return WayangPlatformReadiness.assess(this, defaultReadinessProfile(resolution.profiles()));
    }

    @Override
    public WayangReadinessReport platformReadiness(String profileId) {
        return WayangPlatformReadiness.assess(this, platformReadinessProfile(profileId));
    }

    @Override
    public List<WayangPlatformReadinessProfileDescriptor> platformReadinessProfiles() {
        return platformReadinessProfileRegistryResolution().profiles();
    }

    @Override
    public WayangPlatformReadinessProfileDescriptor platformReadinessProfile(String profileId) {
        String resolved = SdkText.trimToDefault(profileId, WayangPlatformReadinessProfiles.DEFAULT);
        List<WayangPlatformReadinessProfileDescriptor> profiles = platformReadinessProfiles();
        return profiles.stream()
                .filter(profile -> profile.profileId().equals(resolved))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown platform readiness profile '" + resolved + "'. Available profiles: "
                                + String.join(", ", profiles.stream()
                                        .map(WayangPlatformReadinessProfileDescriptor::profileId)
                                        .toList())
                                + "."));
    }

    @Override
    public WayangPlatformReadinessProfileRegistry platformReadinessProfileRegistry() {
        return readinessProfileRegistry;
    }

    @Override
    public WayangPlatformReadinessProfileRegistryConfigDiagnostics
            platformReadinessProfileRegistryConfigDiagnostics() {
        return config.readinessProfileRegistry().diagnostics();
    }

    @Override
    public WayangPlatformReadinessProfileExternalReaderProviderDiscoveryReport
            platformReadinessProfileExternalReaderProviderDiscovery() {
        return WayangPlatformReadinessProfileExternalReaderProviders.discoveryReport(config);
    }

    @Override
    public WayangPlatformReadinessProfileRegistryPreflightReport
            platformReadinessProfileRegistryPreflight() {
        return WayangPlatformReadinessProfileRegistryPreflightReport.of(
                platformReadinessProfileRegistryConfigDiagnostics(),
                platformReadinessProfileExternalReaderProviderDiscovery(),
                platformReadinessProfileRegistryResolution());
    }

    @Override
    public WayangPlatformReadinessProfileValidationReport platformReadinessProfileValidation() {
        return platformReadinessProfileRegistryResolution().validation();
    }

    @Override
    public WayangPlatformReadinessProfileValidationReport platformReadinessProfileValidation(String policyId) {
        return platformReadinessProfileRegistry()
                .resolve(WayangPlatformReadinessProfileValidationPolicies.policy(policyId))
                .validation();
    }

    @Override
    public WayangPlatformReadinessProfileValidationReport platformReadinessProfileValidation(
            WayangPlatformReadinessProfileValidationPolicy policy) {
        return platformReadinessProfileRegistry().resolve(policy).validation();
    }

    @Override
    public AgentRunResult run(AgentRunRequest request) {
        AgentRunPreparation preparation = runPlanner.prepare(request);
        AgentRunRequest configured = preparation.request();
        WorkspaceSnapshot workspace = preparation.workspace();
        HarnessPlan harness = preparation.harness();
        AgentRequest agentRequest = preparation.coreRequest();
        AgentRunReadiness readiness = preparation.readiness();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("requestId", agentRequest.requestId());
        metadata.put(AgentRunMetadata.TENANT, agentRequest.tenantId());
        if (agentRequest.sessionId() != null && !agentRequest.sessionId().isBlank()) {
            metadata.put(AgentRunMetadata.SESSION, agentRequest.sessionId());
        }
        if (agentRequest.userId() != null && !agentRequest.userId().isBlank()) {
            metadata.put("user", agentRequest.userId());
        }
        if (agentRequest.systemPrompt() != null && !agentRequest.systemPrompt().isBlank()) {
            metadata.put("systemPrompt", agentRequest.systemPrompt());
        }
        metadata.put("model", agentRequest.modelId() == null ? "backend-default" : agentRequest.modelId());
        metadata.put("workflow", agentRequest.context().getOrDefault("workflowId", "agent-direct"));
        metadata.put(
                AgentRunMetadata.SURFACE,
                agentRequest.context().getOrDefault(AgentRunMetadata.SURFACE_ID, configured.surfaceId()));
        AgentRunMetadata.putProfileAliases(metadata, AgentRunMetadata.profileContext(agentRequest.context()));
        metadata.put("surfacePolicy", agentRequest.context().get("surfacePolicy"));
        metadata.put("surfacePolicyAssessment", agentRequest.context().get("surfacePolicyAssessment"));
        metadata.put("skillAssessment", AgentRunSkillAssessmentContext.from(readiness.skillAssessment()));
        metadata.put("runReadiness", AgentRunReadinessContext.from(readiness));
        if (!configured.context().isEmpty()) {
            metadata.put("context", configured.context());
        }
        metadata.put("memoryEnabled", agentRequest.memoryConfig().conversationEnabled());
        metadata.put("maxSteps", agentRequest.getMaxSteps());
        metadata.put("skills", agentRequest.allowedSkills());
        metadata.put("sdkMode", config.mode().name().toLowerCase());
        if (workspace != null) {
            metadata.put("workspace", WorkspaceContext.from(workspace));
        }
        if (harness != null) {
            metadata.put("harness", HarnessContext.from(harness));
        }

        List<String> steps = new ArrayList<>();
        steps.add("Normalize SDK request into Wayang agent request");
        if (workspace != null) {
            steps.add("Inspect workspace and attach compact context to the agent request");
        }
        if (harness != null) {
            steps.add("Plan harness checks and attach verification context to the agent request");
        }
        steps.add("Route inference through Gollek backend adapter");
        steps.add("Route workflow execution through Gamelan backend adapter");
        steps.add("Expose skills, tools, MCP, RAG, and memory through Wayang boundaries");

        AgentRunResult result = new AgentRunResult(
                UUID.randomUUID().toString(),
                "Prepared Wayang agent run for: " + agentRequest.prompt(),
                true,
                "wayang-agent-over-gollek",
                steps,
                metadata);
        runLifecycle.record(result);
        return result;
    }

    @Override
    public AgentRunStatus runStatus(String runId) {
        return runLifecycle.status(runId);
    }

    @Override
    public AgentRunHistory runHistory(AgentRunHistoryQuery query) {
        return runLifecycle.history(query);
    }

    @Override
    public AgentRunStoreDiagnostics runStoreDiagnostics() {
        return runLifecycle.diagnostics();
    }

    @Override
    public AgentRunStoreVerification runStoreVerification() {
        return runLifecycle.verification();
    }

    @Override
    public AgentRunStoreCompactionPreview runStoreCompactionPreview() {
        return runLifecycle.compactionPreview();
    }

    @Override
    public AgentRunStoreCompactionResult compactRunStore() {
        return runLifecycle.compact();
    }

    @Override
    public AgentRunEvents runEvents(String runId) {
        return runEvents(runId, AgentRunEventsQuery.all());
    }

    @Override
    public AgentRunEvents runEvents(String runId, AgentRunEventsQuery query) {
        return runLifecycle.events(runId, query);
    }

    @Override
    public AgentRunForgetResult forgetRun(String runId) {
        return runLifecycle.forget(runId);
    }

    @Override
    public AgentRunCancelResult cancelRun(String runId, String reason) {
        return runLifecycle.cancel(runId, reason);
    }

    @Override
    public AgentRequest toCoreAgentRequest(AgentRunRequest request) {
        return runPlanner.toCoreAgentRequest(request);
    }

    private int availableSkillCount() {
        return (int) skillRegistry.list().stream()
                .filter(RegisteredSkill::availableForRuns)
                .count();
    }

    private WayangPlatformReadinessProfileDescriptor defaultReadinessProfile() {
        return defaultReadinessProfile(platformReadinessProfiles());
    }

    private WayangPlatformReadinessProfileDescriptor defaultReadinessProfile(
            List<WayangPlatformReadinessProfileDescriptor> profiles) {
        List<WayangPlatformReadinessProfileDescriptor> model = profiles == null ? List.of() : profiles;
        return model.stream()
                .filter(WayangPlatformReadinessProfileDescriptor::defaultProfile)
                .findFirst()
                .orElseGet(() -> model.stream()
                        .filter(profile -> WayangPlatformReadinessProfiles.DEFAULT.equals(profile.profileId()))
                        .findFirst()
                        .orElseGet(() -> model.stream()
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                        "No platform readiness profiles are available."))));
    }

    private WayangReadinessReport platformReadinessRegistryBlocked(
            WayangPlatformReadinessProfileRegistryResolution resolution) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("readinessProfileId", "unavailable");
        attributes.put("readinessProfileDefault", false);
        attributes.put("readinessProfileProduction", false);
        attributes.put("readinessProfileComponentIds", List.of(
                WayangPlatformReadinessProfileRegistryReadiness.READINESS_ID));
        attributes.put("readinessProfileRegistryValid", resolution.valid());
        attributes.put("readinessProfileRegistryActiveSourceId", resolution.activeSourceId());
        attributes.put("readinessProfileRegistryActiveSourceType", resolution.activeSourceType());
        attributes.put("readinessProfileRegistryFallbackUsed", resolution.fallbackUsed());
        return WayangReadinessReports.aggregate(
                WayangPlatformReadiness.READINESS_ID,
                List.of(WayangPlatformReadinessProfileRegistryReadiness.assess(resolution)),
                attributes);
    }

    private WayangReadinessReport platformReadinessRegistryConfigBlocked(
            WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics) {
        Map<String, Object> attributes = new LinkedHashMap<>();
        attributes.put("readinessProfileId", "unavailable");
        attributes.put("readinessProfileDefault", false);
        attributes.put("readinessProfileProduction", false);
        attributes.put("readinessProfileComponentIds", List.of(
                WayangPlatformReadinessProfileRegistryConfigReadiness.READINESS_ID));
        attributes.put("readinessProfileRegistryConfigValid", diagnostics.valid());
        attributes.put("readinessProfileRegistryConfigIssueCount", diagnostics.issueCount());
        return WayangReadinessReports.aggregate(
                WayangPlatformReadiness.READINESS_ID,
                List.of(WayangPlatformReadinessProfileRegistryConfigReadiness.assess(diagnostics)),
                attributes);
    }

}
