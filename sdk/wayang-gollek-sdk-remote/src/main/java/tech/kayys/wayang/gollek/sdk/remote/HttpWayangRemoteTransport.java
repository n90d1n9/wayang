package tech.kayys.wayang.gollek.sdk.remote;

import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.gollek.sdk.AgentRunRequest;
import tech.kayys.wayang.gollek.sdk.AgentRunCancelResult;
import tech.kayys.wayang.gollek.sdk.AgentRunEvents;
import tech.kayys.wayang.gollek.sdk.AgentRunEventsQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunForgetResult;
import tech.kayys.wayang.gollek.sdk.AgentRunHistory;
import tech.kayys.wayang.gollek.sdk.AgentRunHistoryQuery;
import tech.kayys.wayang.gollek.sdk.AgentRunPreview;
import tech.kayys.wayang.gollek.sdk.AgentRunReadiness;
import tech.kayys.wayang.gollek.sdk.AgentRunReadinessContext;
import tech.kayys.wayang.gollek.sdk.AgentRunResult;
import tech.kayys.wayang.gollek.sdk.AgentRunStatus;
import tech.kayys.wayang.gollek.sdk.AgentRunSkillAssessmentContext;
import tech.kayys.wayang.gollek.sdk.AgentRunSkillPreflight;
import tech.kayys.wayang.gollek.sdk.AgentSkillDiscovery;
import tech.kayys.wayang.gollek.sdk.AgentSkillQuery;
import tech.kayys.wayang.gollek.sdk.ComponentStatus;
import tech.kayys.wayang.gollek.sdk.HarnessPlan;
import tech.kayys.wayang.gollek.sdk.HarnessPlanRequest;
import tech.kayys.wayang.gollek.sdk.ProductSurface;
import tech.kayys.wayang.gollek.sdk.ProductSurfacePolicy;
import tech.kayys.wayang.gollek.sdk.RegisteredSkill;
import tech.kayys.wayang.gollek.sdk.SkillRegistry;
import tech.kayys.wayang.gollek.sdk.SurfacePolicyAssessment;
import tech.kayys.wayang.gollek.sdk.SurfacePolicyAssessmentContext;
import tech.kayys.wayang.gollek.sdk.SurfacePolicyContext;
import tech.kayys.wayang.gollek.sdk.SurfacePolicyPreflight;
import tech.kayys.wayang.gollek.sdk.WayangAgentRequestMapper;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdkConfig;
import tech.kayys.wayang.gollek.sdk.WayangPlatformStatus;
import tech.kayys.wayang.gollek.sdk.WayangProductCatalog;
import tech.kayys.wayang.gollek.sdk.WayangSkillCatalog;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchCatalog;
import tech.kayys.wayang.gollek.sdk.WayangWorkbenchModel;
import tech.kayys.wayang.gollek.sdk.WorkspaceInspectionRequest;
import tech.kayys.wayang.gollek.sdk.WorkspaceSnapshot;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HttpWayangRemoteTransport implements WayangRemoteTransport {

    private final URI endpoint;
    private final String apiKey;
    private final String defaultTenantId;
    private final String defaultModelId;
    private final HttpClient client;
    private final RemoteRunLifecycleMapper runLifecycle;
    private final RemoteSkillMapper skills;

    public HttpWayangRemoteTransport(WayangGollekSdkConfig config) {
        this(config, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build());
    }

    HttpWayangRemoteTransport(WayangGollekSdkConfig config, HttpClient client) {
        if (config == null || config.endpoint().isBlank()) {
            throw new RemoteWayangGollekException("Remote Wayang endpoint is required.");
        }
        this.endpoint = URI.create(config.endpoint());
        this.apiKey = config.apiKey();
        this.defaultTenantId = config.defaultTenantId();
        this.defaultModelId = config.defaultModelId();
        this.client = client;
        this.runLifecycle = new RemoteRunLifecycleMapper(endpoint);
        this.skills = new RemoteSkillMapper();
    }

    @Override
    public WayangPlatformStatus status() {
        RemoteResponse response = send(request("/status").GET().build());
        return new WayangPlatformStatus(
                "Wayang",
                "remote",
                new ComponentStatus("Gollek", "Inference, serving, and training engine", "remote", endpoint.toString(), 80),
                new ComponentStatus("Gamelan", "Workflow execution engine", "remote", endpoint.toString(), 80),
                new ComponentStatus("Agent Core", "Agentic orchestration and skills", "remote", endpoint.toString(), 80),
                new ComponentStatus("RAG Runtime", "Retrieval, plugins, and response assembly", "remote", endpoint.toString(), 80),
                new ComponentStatus("MCP", "Dynamic tool bridge", "remote", endpoint.toString(), 80),
                RemoteJson.intField(response.body(), "activeSkills", 0),
                List.of(
                        "Remote Wayang API reached at " + endpoint,
                        "Remote status endpoint returned HTTP " + response.statusCode()));
    }

    @Override
    public List<ProductSurface> productSurfaces() {
        return WayangProductCatalog.defaultSurfaces();
    }

    @Override
    public WayangWorkbenchModel workbench() {
        return new WayangWorkbenchModel(
                status(),
                productSurfaces(),
                WayangWorkbenchCatalog.remoteCommandPalette(),
                WayangWorkbenchCatalog.remoteCommands(),
                List.of(
                        "Map remote API responses to richer SDK result envelopes.",
                        "Map remote workspace snapshots into structured SDK models.",
                        "Add streaming remote run support.",
                        "Share auth, tenant, and audit metadata with platform APIs."));
    }

    @Override
    public SkillRegistry skillRegistry() {
        RemoteResponse response = sendWithoutStatusCheck(request("/skills").GET().build());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return WayangRemoteTransport.super.skillRegistry();
        }
        return skills.registry(response);
    }

    @Override
    public RegisteredSkill skill(String skillId) {
        String normalizedSkillId = skillId == null ? "" : skillId.trim();
        RemoteResponse response = sendWithoutStatusCheck(request("/skills/" + pathSegment(normalizedSkillId)).GET().build());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return skills.skill(response, normalizedSkillId);
        }
        AgentSkillQuery query = new AgentSkillQuery(
                null,
                null,
                null,
                null,
                normalizedSkillId,
                null,
                null,
                null);
        return skillDiscovery(query, "").skills().stream()
                .filter(skill -> skill.matchesIdOrAlias(normalizedSkillId))
                .findFirst()
                .orElseGet(() -> skillRegistry().require(normalizedSkillId));
    }

    @Override
    public AgentSkillDiscovery skillDiscovery(AgentSkillQuery query, String search) {
        AgentSkillQuery normalized = query == null ? AgentSkillQuery.all() : query;
        RemoteResponse response = sendWithoutStatusCheck(request(skillsPath(normalized, search)).GET().build());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return WayangRemoteTransport.super.skillDiscovery(normalized, search);
        }
        return skills.discovery(response, normalized, search);
    }

    @Override
    public WorkspaceSnapshot inspectWorkspace(WorkspaceInspectionRequest request) {
        WorkspaceInspectionRequest normalized = request == null ? WorkspaceInspectionRequest.current() : request;
        String body = RemoteJson.workspaceRequest(
                normalized.rootPath(),
                normalized.maxEntries(),
                normalized.includeHidden());
        RemoteResponse response = send(request("/workspace/inspect")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build());
        return new WorkspaceSnapshot(
                normalized.rootPath(),
                true,
                true,
                false,
                "",
                "",
                List.of(),
                List.of("remote"),
                List.of(),
                List.of(),
                List.of(
                        "Remote Wayang workspace inspection submitted to " + endpoint,
                        "Remote workspace endpoint returned HTTP " + response.statusCode(),
                        "Remote response preview: " + preview(response.body())));
    }

    @Override
    public HarnessPlan planHarness(HarnessPlanRequest request) {
        HarnessPlanRequest normalized = request == null ? HarnessPlanRequest.current() : request;
        String body = RemoteJson.harnessPlanRequest(
                normalized.rootPath(),
                normalized.maxChecks(),
                normalized.includeOptional());
        RemoteResponse response = send(request("/harness/plan")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build());
        WorkspaceSnapshot workspace = new WorkspaceSnapshot(
                normalized.rootPath(),
                true,
                true,
                false,
                "",
                "",
                List.of(),
                List.of("remote"),
                List.of(),
                List.of(),
                List.of("Remote Wayang harness planning submitted to " + endpoint));
        return new HarnessPlan(
                workspace,
                List.of(),
                List.of(
                        "Remote harness endpoint returned HTTP " + response.statusCode(),
                        "Remote response preview: " + preview(response.body())));
    }

    @Override
    public AgentRunPreview previewRun(AgentRunRequest request) {
        AgentRunRequest normalized = applyDefaults(request);
        AgentRequest agentRequest = new WayangAgentRequestMapper().toAgentRequest(normalized);
        SkillRegistry registry = skillRegistryOrDefault();
        return AgentRunPreview.from(
                normalized,
                agentRequest,
                SurfacePolicyPreflight.assess(normalized),
                AgentRunSkillPreflight.assess(registry, normalized));
    }

    @Override
    public AgentRunResult run(AgentRunRequest request) {
        AgentRunRequest normalized = applyDefaults(request);
        ProductSurfacePolicy surfacePolicy = WayangProductCatalog.policyFor(normalized.surfaceId());
        AgentRunReadiness readiness = AgentRunReadiness.assess(skillRegistryOrDefault(), normalized);
        SurfacePolicyAssessment surfaceAssessment = readiness.surfacePolicyAssessment();
        String body = RemoteJson.runRequest(
                normalized.prompt(),
                normalized.tenantId(),
                normalized.modelId(),
                normalized.workflowId(),
                normalized.surfaceId(),
                normalized.skills(),
                normalized.memoryEnabled(),
                normalized.maxSteps(),
                normalized.workspacePath(),
                normalized.workspaceEnabled(),
                normalized.workspaceMaxEntries(),
                normalized.harnessEnabled(),
                normalized.harnessMaxChecks(),
                normalized.harnessIncludeOptional(),
                normalized.sessionId(),
                normalized.userId(),
                normalized.context(),
                normalized.systemPrompt());
        RemoteResponse response = send(request("/runs")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build());

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("endpoint", endpoint.toString());
        metadata.put("httpStatus", response.statusCode());
        metadata.put("sdkMode", "remote");
        metadata.put("tenant", normalized.tenantId());
        if (!normalized.sessionId().isBlank()) {
            metadata.put("session", normalized.sessionId());
        }
        if (!normalized.userId().isBlank()) {
            metadata.put("user", normalized.userId());
        }
        if (!normalized.systemPrompt().isBlank()) {
            metadata.put("systemPrompt", normalized.systemPrompt());
        }
        metadata.put("model", normalized.modelId().isBlank() ? "backend-default" : normalized.modelId());
        metadata.put("workflow", normalized.workflowId().isBlank() ? "agent-direct" : normalized.workflowId());
        metadata.put("surface", normalized.surfaceId());
        metadata.put("surfacePolicy", SurfacePolicyContext.from(surfacePolicy));
        metadata.put("surfacePolicyAssessment", SurfacePolicyAssessmentContext.from(surfaceAssessment));
        metadata.put("skillAssessment", AgentRunSkillAssessmentContext.from(readiness.skillAssessment()));
        metadata.put("runReadiness", AgentRunReadinessContext.from(readiness));
        if (!normalized.context().isEmpty()) {
            metadata.put("context", normalized.context());
        }
        metadata.put("skills", normalized.skills());
        Map<String, Object> remoteMetadata = RemoteJson.objectField(response.body(), "metadata");
        if (!remoteMetadata.isEmpty()) {
            metadata.put("remoteMetadata", remoteMetadata);
        }
        metadata.put("responsePreview", preview(response.body()));
        RemoteRunLifecycleMapper.RemoteRunSubmission submission = runLifecycle.submission(response);
        metadata.put("remoteRunId", submission.runId());
        metadata.put("remoteRunState", submission.state().name());
        if (normalized.workspaceEnabled()) {
            metadata.put("workspace", Map.of(
                    "rootPath", normalized.workspacePath(),
                    "maxEntries", normalized.workspaceMaxEntries()));
        }
        if (normalized.harnessEnabled()) {
            metadata.put("harness", Map.of(
                    "rootPath", normalized.workspacePath(),
                    "maxChecks", normalized.harnessMaxChecks(),
                    "includeOptional", normalized.harnessIncludeOptional()));
        }

        List<String> steps = new ArrayList<>();
        steps.add("Normalize SDK request into remote Wayang API request");
        if (normalized.workspaceEnabled()) {
            steps.add("Pass workspace inspection request to remote Wayang API");
        }
        if (normalized.harnessEnabled()) {
            steps.add("Pass harness planning request to remote Wayang API");
        }
        steps.add("Submit run to POST /runs");
        steps.add("Return remote submission metadata through the SDK contract");

        return new AgentRunResult(
                submission.runId(),
                "Submitted Wayang agent run to remote API: " + endpoint,
                submission.successful(),
                submission.strategy(),
                steps,
                metadata,
                submission.handle());
    }

    @Override
    public AgentRunStatus runStatus(String runId) {
        String normalizedRunId = runId == null ? "" : runId.trim();
        RemoteResponse response = send(request(runLifecycle.statusPath(normalizedRunId))
                .GET()
                .build());
        return runLifecycle.status(
                response,
                normalizedRunId,
                "Remote run status endpoint returned HTTP " + response.statusCode() + ".");
    }

    @Override
    public AgentRunHistory runHistory(AgentRunHistoryQuery query) {
        AgentRunHistoryQuery normalized = query == null ? AgentRunHistoryQuery.all() : query;
        RemoteResponse response = send(request(runLifecycle.historyPath(normalized)).GET().build());
        return runLifecycle.history(response, normalized);
    }

    @Override
    public AgentRunEvents runEvents(String runId) {
        return runEvents(runId, AgentRunEventsQuery.all());
    }

    @Override
    public AgentRunEvents runEvents(String runId, AgentRunEventsQuery query) {
        String normalizedRunId = runId == null ? "" : runId.trim();
        AgentRunEventsQuery normalizedQuery = query == null ? AgentRunEventsQuery.all() : query;
        RemoteResponse response = send(request(runLifecycle.eventsPath(normalizedRunId, normalizedQuery)).GET().build());
        return runLifecycle.events(response, normalizedRunId, normalizedQuery);
    }

    @Override
    public AgentRunCancelResult cancelRun(String runId, String reason) {
        String normalizedRunId = runId == null ? "" : runId.trim();
        RemoteResponse response = send(request(runLifecycle.cancelPath(normalizedRunId))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                        RemoteJson.cancelRequest(reason),
                        StandardCharsets.UTF_8))
                .build());
        return runLifecycle.cancel(response, normalizedRunId, reason);
    }

    @Override
    public AgentRunForgetResult forgetRun(String runId) {
        String normalizedRunId = runId == null ? "" : runId.trim();
        RemoteResponse response = send(request(runLifecycle.forgetPath(normalizedRunId))
                .DELETE()
                .build());
        return runLifecycle.forget(response, normalizedRunId);
    }

    private HttpRequest.Builder request(String path) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(resolve(path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json")
                .header("User-Agent", "wayang-sdk-remote/1.0");
        if (!apiKey.isBlank()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        return builder;
    }

    private URI resolve(String path) {
        String base = endpoint.toString();
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        return URI.create(normalizedBase + path);
    }

    private RemoteResponse send(HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RemoteWayangGollekException(
                        "Remote Wayang API returned HTTP " + response.statusCode() + " for " + request.uri());
            }
            return new RemoteResponse(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new RemoteWayangGollekException("Unable to reach remote Wayang API at " + endpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteWayangGollekException("Interrupted while calling remote Wayang API at " + endpoint, e);
        }
    }

    private RemoteResponse sendWithoutStatusCheck(HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new RemoteResponse(response.statusCode(), response.body());
        } catch (IOException e) {
            throw new RemoteWayangGollekException("Unable to reach remote Wayang API at " + endpoint, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteWayangGollekException("Interrupted while calling remote Wayang API at " + endpoint, e);
        }
    }

    private AgentRunRequest applyDefaults(AgentRunRequest request) {
        AgentRunRequest normalized = request == null
                ? AgentRunRequest.builder()
                        .tenantId(defaultTenantId)
                        .modelId(defaultModelId)
                        .build()
                : AgentRunRequest.builder(request).build();
        String tenantId = "default".equals(normalized.tenantId()) && !"default".equals(defaultTenantId)
                ? defaultTenantId
                : normalized.tenantId();
        String modelId = normalized.modelId().isBlank() ? defaultModelId : normalized.modelId();
        String surfaceId = WayangProductCatalog.requireKnownSurfaceId(normalized.surfaceId());
        return AgentRunRequest.builder(normalized)
                .tenantId(tenantId)
                .modelId(modelId)
                .surfaceId(surfaceId)
                .build();
    }

    private SkillRegistry skillRegistryOrDefault() {
        try {
            return skillRegistry();
        } catch (RemoteWayangGollekException e) {
            return WayangSkillCatalog.defaultRegistry();
        }
    }

    private String preview(String value) {
        String normalized = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 160 ? normalized : normalized.substring(0, 160);
    }

    private static String pathSegment(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String skillsPath(AgentSkillQuery query, String search) {
        AgentSkillQuery normalized = query == null ? AgentSkillQuery.all() : query;
        StringBuilder output = new StringBuilder("/skills");
        String separator = "?";
        separator = appendQuery(output, separator, "surfaceId", normalized.surfaceId());
        separator = appendQuery(output, separator, "profileId", normalized.profileId());
        separator = appendQuery(output, separator, "category", normalized.category());
        separator = appendQuery(output, separator, "source", normalized.source());
        separator = appendQuery(output, separator, "state", normalized.state() == null ? "" : normalized.state().name());
        separator = appendQuery(output, separator, "skillId", normalized.skillId());
        separator = appendQuery(output, separator, "tag", normalized.tag());
        separator = appendQuery(output, separator, "inputKey", normalized.inputKey());
        separator = appendQuery(output, separator, "outputKey", normalized.outputKey());
        appendQuery(output, separator, "search", search);
        return output.toString();
    }

    private static String appendQuery(StringBuilder output, String separator, String key, String value) {
        if (key == null || value == null || value.isBlank()) {
            return separator;
        }
        output.append(separator)
                .append(pathSegment(key))
                .append("=")
                .append(pathSegment(value));
        return "&";
    }
}
