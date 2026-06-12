package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tech.kayys.wayang.gollek.sdk.AgentRunRequest;
import tech.kayys.wayang.gollek.sdk.WayangRunSpec;
import tech.kayys.wayang.gollek.sdk.WayangSpecApi;

import java.io.InputStream;
import java.util.List;

/**
 * Picocli mixin that resolves run request options over an optional Wayang run spec.
 *
 * <p>The mixin performs CLI-specific overlay and text-source handling while
 * delegating spec lookup to {@link WayangSpecApi}.</p>
 */
final class WayangRunRequestOptions {

    @Parameters(index = "0", arity = "0..1", description = "Prompt or task for the agent.")
    String prompt;

    @Option(names = "--spec", paramLabel = "<path>", description = "Load run defaults from a Java properties spec file.")
    String specPath;

    @Option(names = "--profile", description = "Product profile id to use as run defaults.")
    String profileId;

    @Option(names = "--prompt-file", paramLabel = "<path>", description = "Read the run prompt from a UTF-8 file.")
    String promptFile;

    @Option(names = "--stdin", description = "Read the run prompt from standard input.")
    boolean promptFromStdin;

    @Option(names = "--system", description = "System prompt to attach to the core agent request.")
    String systemPrompt;

    @Option(names = "--system-file", paramLabel = "<path>", description = "Read the system prompt from a UTF-8 file.")
    String systemPromptFile;

    @Option(names = {"-t", "--tenant"}, description = "Tenant id.")
    String tenantId;

    @Option(names = {"-m", "--model"}, description = "Gollek model id or backend alias.")
    String modelId;

    @Option(names = "--session", description = "Session id for conversational or coding-agent continuity.")
    String sessionId;

    @Option(names = "--user", description = "End-user id to attach to the run request.")
    String userId;

    @Option(names = {"-w", "--workflow"}, description = "Gamelan workflow id.")
    String workflowId;

    @Option(names = "--surface", description = "Product surface id for this run.")
    String surfaceId;

    @Option(names = {"-s", "--skill"}, description = "Allowed skill id. Can be repeated.")
    List<String> skills;

    @Option(
            names = {"-C", "--context"},
            paramLabel = "<key=value>",
            description = "Additional run context. Can be repeated for RAG namespaces, MCP hints, product ids, etc.")
    List<String> contextEntries;

    @Option(names = "--no-memory", description = "Disable memory context for this run.")
    boolean noMemory;

    @Option(names = "--max-steps", description = "Maximum agent reasoning/tool steps.")
    Integer maxSteps;

    @Option(
            names = "--workspace",
            arity = "0..1",
            fallbackValue = ".",
            paramLabel = "<path>",
            description = "Inspect workspace context before running. Uses current directory when no path is provided.")
    String workspacePath;

    @Option(names = "--workspace-max-entries", description = "Maximum workspace top-level paths to attach.")
    Integer workspaceMaxEntries;

    @Option(names = "--harness", description = "Attach planned verification checks to the agent request.")
    boolean harnessEnabled;

    @Option(names = "--harness-max-checks", description = "Maximum harness checks to attach.")
    Integer harnessMaxChecks;

    @Option(names = "--harness-required-only", description = "Attach only high-confidence required checks.")
    boolean harnessRequiredOnly;

    WayangRunSpec readSpecOrDefault(WayangSpecApi specs) {
        if (profileId != null && profileId.isBlank()) {
            throw new IllegalArgumentException("--profile requires a non-empty id.");
        }
        return specs.readSpecOrDefault(specPath, profileId);
    }

    AgentRunRequest toRequest(AgentRunRequest specRequest, InputStream in) {
        String resolvedPrompt = WayangCliTextSources.required(
                "Prompt",
                prompt,
                promptFile,
                promptFromStdin,
                in,
                specRequest.prompt());
        String resolvedSystemPrompt = WayangCliTextSources.optional(
                "System prompt",
                systemPrompt,
                systemPromptFile,
                specRequest.systemPrompt());
        AgentRunRequest.Builder request = AgentRunRequest.builder(specRequest)
                .prompt(resolvedPrompt)
                .systemPrompt(resolvedSystemPrompt)
                .memoryEnabled(noMemory ? false : specRequest.memoryEnabled());
        if (tenantId != null) {
            request.tenantId(tenantId);
        }
        if (modelId != null) {
            request.modelId(modelId);
        }
        if (sessionId != null) {
            request.sessionId(sessionId);
        }
        if (userId != null) {
            request.userId(userId);
        }
        if (workflowId != null) {
            request.workflowId(workflowId);
        }
        if (surfaceId != null) {
            request.surfaceId(surfaceId);
        }
        if (skills != null) {
            request.skills(skills);
        }
        WayangCliContextEntries.parse(contextEntries).forEach(request::context);
        if (maxSteps != null) {
            request.maxSteps(maxSteps);
        }
        if (workspaceMaxEntries != null) {
            request.workspaceMaxEntries(workspaceMaxEntries);
        }
        if (harnessEnabled) {
            request.harness(true);
        }
        if (harnessMaxChecks != null) {
            request.harnessMaxChecks(harnessMaxChecks);
        }
        if (harnessRequiredOnly) {
            request.harnessIncludeOptional(false);
        }
        if (workspacePath != null) {
            request.workspace(workspacePath);
        }
        return request.build();
    }
}
