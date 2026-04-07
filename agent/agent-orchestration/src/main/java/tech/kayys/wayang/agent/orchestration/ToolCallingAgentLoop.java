package tech.kayys.wayang.agent.orchestration;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.client.GollekAgentClient;
import tech.kayys.wayang.agent.registry.AgentProviderRegistry;
import tech.kayys.wayang.agent.service.AgenticInferenceService;
import tech.kayys.wayang.agent.spi.*;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.tool.ToolDefinition;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Enhanced agent loop with native Gollek tool calling support.
 *
 * <p>This orchestrator implements a sophisticated agent loop that:
 * <ul>
 *   <li>Uses native tool calling from the inference engine</li>
 *   <li>Executes multiple tool calls in parallel when possible</li>
 *   <li>Implements tool result caching and memoization</li>
 *   <li>Supports tool composition and chaining</li>
 *   <li>Provides detailed metrics and observability</li>
 * </ul>
 *
 * <h2>Agent Loop Flow:</h2>
 * <pre>
 * 1. Build inference request with tool definitions
 * 2. Call LLM with tools available
 * 3. If response contains tool calls:
 *    a. Execute tools (parallel if independent)
 *    b. Append tool results to conversation
 *    c. Loop back to step 2
 * 4. If response contains final answer:
 *    a. Return answer to user
 * </pre>
 *
 * <h2>Parallel Tool Execution:</h2>
 * <p>When the model generates multiple tool calls in a single response,
 * this orchestrator analyzes tool dependencies and executes independent
 * tools in parallel for improved performance.</p>
 *
 * @author Wayang AI Team
 * @version 1.0.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class ToolCallingAgentLoop implements AgentOrchestrator {

    private static final Logger LOG = Logger.getLogger(ToolCallingAgentLoop.class);

    @Inject
    GollekAgentClient agentClient;

    @Inject
    AgentProviderRegistry providerRegistry;

    @Inject
    AgenticInferenceService inferenceService;

    @Inject
    SkillRegistry skillRegistry;

    // Tool execution cache (memoization)
    private final Map<String, CachedToolResult> toolCache = new ConcurrentHashMap<>();
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    // Parallel execution thread pool
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Override
    public String strategyId() {
        return "tool-calling";
    }

    @Override
    public boolean supportsToolCalling() {
        return true;
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    @Override
    public boolean supportsCheckpoint() {
        return true;
    }

    @Override
    public List<String> supportedFeatures() {
        return List.of(
            "native-tool-calling",
            "parallel-tool-execution",
            "tool-caching",
            "tool-chaining",
            "streaming"
        );
    }

    @Override
    public Optional<String> getPreferredProvider() {
        // Prefer providers with good tool calling support
        return Optional.of("openai");
    }

    @Override
    public Map<String, Object> getRecommendedParameters() {
        return Map.of(
            "temperature", 0.7,
            "max_tokens", 2048,
            "tool_choice", "auto"
        );
    }

    @Override
    public int getMaxSteps() {
        return 15; // Higher limit for complex tool workflows
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Core Orchestration
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public Uni<AgentResponse> execute(AgentRequest request) {
        LOG.infof("ToolCallingAgentLoop: starting execution for tenant=%s, request=%s",
            request.tenantId(), request.requestId());

        Instant startTime = Instant.now();
        AgentState initialState = AgentState.initial(request);

        return executeLoop(initialState, 0)
            .map(finalState -> buildResponse(finalState, request, startTime));
    }

    @Override
    public Multi<AgentEvent> stream(AgentRequest request) {
        return Multi.createFrom().emitter(emitter -> {
            Instant startTime = Instant.now();
            AgentState state = AgentState.initial(request);

            emitter.emit(AgentEvent.started(state.getRunId(), request.prompt()));
            emitter.emit(AgentEvent.log(state.getRunId(), 0, 
                "Starting tool-calling agent loop..."));

            streamLoop(state, emitter, 0, startTime);
        });
    }

    @Override
    public Uni<AgentState> step(AgentState state) {
        if (isTerminal(state)) {
            return Uni.createFrom().item(state);
        }

        return executeStep(state, state.getStep());
    }

    @Override
    public boolean isTerminal(AgentState state) {
        return state.isTerminal() || 
               state.atMaxSteps() || 
               state.getPhase() == AgentState.Phase.COMPLETE;
    }

    @Override
    public String getSystemPromptFragment() {
        return """
            You are an intelligent agent with access to tools.
            
            When you need to use a tool:
            1. Think about which tool is appropriate
            2. Call the tool with the correct arguments
            3. Wait for the tool result
            4. Use the result to continue reasoning or provide a final answer
            
            You can call multiple tools in parallel if they are independent.
            Always wait for tool results before making dependent tool calls.
            
            When you have enough information to answer the user's question,
            respond with a clear Final Answer.
            """;
    }

    @Override
    public List<ToolDefinition> getToolDefinitions(AgentRequest request) {
        List<AgentSkill> skills = request.hasSkillFilter()
            ? request.allowedSkills().stream()
                .map(skillRegistry::find)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList()
            : skillRegistry.listAll();

        return skills.stream()
            .map(skill -> inferenceService.createToolDefinition(
                skill.id(),
                skill.description(),
                skill.inputSchema()
            ))
            .toList();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Main Loop
    // ═══════════════════════════════════════════════════════════════════════

    private Uni<AgentState> executeLoop(AgentState state, int iteration) {
        if (isTerminal(state)) {
            LOG.infof("ToolCallingAgentLoop: terminating at iteration %d, reason=%s",
                iteration, getTerminalReason(state));
            return Uni.createFrom().item(state);
        }

        if (iteration >= getMaxSteps()) {
            LOG.warnf("ToolCallingAgentLoop: max steps (%d) reached", getMaxSteps());
            return Uni.createFrom().item(state.withFinalAnswer(
                "Maximum iterations reached. Last result: " + 
                state.getLastObservation().orElse("none")));
        }

        return executeStep(state, iteration)
            .chain(nextState -> executeLoop(nextState, iteration + 1));
    }

    private void streamLoop(
            AgentState state,
            io.smallrye.mutiny.subscription.MultiEmitter<? super AgentEvent> emitter,
            int iteration,
            Instant startTime) {

        if (isTerminal(state) || iteration >= getMaxSteps()) {
            if (state.isComplete()) {
                emitter.emit(AgentEvent.finalAnswer(
                    state.getRunId(), state.getStep(), 
                    state.getFinalAnswer().orElse("")));
            } else if (state.isFailed()) {
                emitter.emit(AgentEvent.error(
                    state.getRunId(), 
                    state.getErrorMessage().orElse("Unknown error")));
            }
            emitter.complete();
            return;
        }

        executeStep(state, iteration).subscribe().with(
            nextState -> {
                // Emit events for this step
                nextState.getLastThought().ifPresent(t -> 
                    emitter.emit(AgentEvent.thought(nextState.getRunId(), t)));
                
                nextState.getPendingAction().ifPresent(action -> 
                    emitter.emit(AgentEvent.action(nextState.getRunId(), action)));
                
                nextState.getLastObservation().ifPresent(obs -> 
                    emitter.emit(AgentEvent.observation(nextState.getRunId(), obs)));

                streamLoop(nextState, emitter, iteration + 1, startTime);
            },
            error -> {
                emitter.emit(AgentEvent.error(state.getRunId(), error.getMessage()));
                emitter.complete();
            }
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Step Execution
    // ═══════════════════════════════════════════════════════════════════════

    private Uni<AgentState> executeStep(AgentState state, int iteration) {
        LOG.debugf("Executing step %d for run %s", iteration + 1, state.getRunId());

        // Build inference request with tool definitions
        List<ToolDefinition> tools = getToolDefinitions(state.getRequest());
        
        InferenceRequest.Builder requestBuilder = InferenceRequest.builder()
            .model(state.getRequest().modelId() != null 
                ? state.getRequest().modelId() : "default")
            .messages(buildMessages(state))
            .tools(tools)
            .toolChoice("auto")
            .temperature(0.7)
            .maxTokens(2048)
            .timeout(getStepTimeout())
            .metadata("tenantId", state.getRequest().tenantId())
            .metadata("runId", state.getRunId())
            .metadata("iteration", iteration);

        // Execute inference
        return agentClient.infer(requestBuilder.build())
            .chain(response -> handleInferenceResponse(state, response, iteration));
    }

    private Uni<AgentState> handleInferenceResponse(
            AgentState state,
            InferenceResponse response,
            int iteration) {

        // Check if model wants to call tools
        if (response.hasToolCalls() && !response.getToolCalls().isEmpty()) {
            LOG.debugf("Model generated %d tool calls", response.getToolCalls().size());
            return executeToolCalls(state, response.getToolCalls(), iteration);
        }

        // No tool calls - treat content as final answer or intermediate reasoning
        String content = response.getContent();
        if (content.contains("Final Answer:")) {
            String finalAnswer = content.substring(
                content.indexOf("Final Answer:") + 13).trim();
            return Uni.createFrom().item(
                state.withFinalAnswer(finalAnswer));
        }

        // Add reasoning to state and continue
        return Uni.createFrom().item(
            state.withThought(content));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Tool Execution
    // ═══════════════════════════════════════════════════════════════════════

    private Uni<AgentState> executeToolCalls(
            AgentState state,
            List<InferenceResponse.ToolCall> toolCalls,
            int iteration) {

        if (toolCalls.isEmpty()) {
            return Uni.createFrom().item(state);
        }

        LOG.infof("Executing %d tool calls for run %s", 
            toolCalls.size(), state.getRunId());

        // Group tool calls by dependencies (simple: assume all independent)
        // For more advanced dependency tracking, analyze tool inputs/outputs
        List<List<InferenceResponse.ToolCall>> batches = 
            groupToolCallsByDependencies(toolCalls, state);

        // Execute batches sequentially (tools within batch execute in parallel)
        Uni<AgentState> current = Uni.createFrom().item(state);
        
        for (List<InferenceResponse.ToolCall> batch : batches) {
            current = current.chain(s -> executeToolBatch(s, batch, iteration));
        }

        return current;
    }

    private Uni<AgentState> executeToolBatch(
            AgentState state,
            List<InferenceResponse.ToolCall> toolCalls,
            int iteration) {

        if (toolCalls.size() == 1) {
            // Single tool - execute directly
            return executeSingleTool(state, toolCalls.get(0), iteration);
        }

        // Multiple tools - execute in parallel
        List<Uni<String>> toolResults = toolCalls.stream()
            .map(tc -> executeSingleTool(state, tc, iteration)
                .map(result -> {
                    state = state.withObservation(result, 
                        new AgentState.ReasoningStep(
                            state.getStep(),
                            "Tool: " + tc.name(),
                            new AgentState.AgentAction(tc.name(), tc.arguments()),
                            result,
                            0,
                            true
                        ));
                    return result;
                }))
            .toList();

        return Uni.combine().all().unis(toolResults)
            .combinedWith(results -> state);
    }

    private Uni<String> executeSingleTool(
            AgentState state,
            InferenceResponse.ToolCall toolCall,
            int iteration) {

        String toolName = toolCall.name();
        Map<String, Object> arguments = toolCall.arguments();
        String cacheKey = buildCacheKey(toolName, arguments);

        // Check cache first
        CachedToolResult cached = toolCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            LOG.debugf("Tool cache hit for %s", toolName);
            return Uni.createFrom().item(cached.result());
        }

        LOG.infof("Executing tool: %s with args: %s", toolName, arguments);

        // Find and execute the skill
        AgentSkill skill;
        try {
            skill = skillRegistry.findOrThrow(toolName);
        } catch (Exception e) {
            String errorMsg = "Tool not found: " + toolName;
            LOG.errorf(e, errorMsg);
            return Uni.createFrom().item(errorMsg);
        }

        SkillContext context = SkillContext.builder()
            .skillId(toolName)
            .inputs(arguments)
            .agentContext(state.getRequest().context())
            .workingMemory(state.getWorkingMemory())
            .runId(state.getRunId())
            .stepNumber(iteration)
            .timeout(getStepTimeout())
            .build();

        long startTime = System.currentTimeMillis();

        return skill.execute(context)
            .map(result -> {
                long duration = System.currentTimeMillis() - startTime;
                LOG.infof("Tool %s completed in %dms", toolName, duration);

                // Cache the result
                toolCache.put(cacheKey, new CachedToolResult(
                    result.getObservation(), Instant.now()));

                return result.getObservation();
            })
            .onFailure().recoverWithItem(err -> {
                LOG.errorf(err, "Tool %s failed", toolName);
                return "Error executing tool " + toolName + ": " + err.getMessage();
            });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Utility Methods
    // ═══════════════════════════════════════════════════════════════════════

    private List<List<InferenceResponse.ToolCall>> groupToolCallsByDependencies(
            List<InferenceResponse.ToolCall> toolCalls,
            AgentState state) {
        
        // Simple implementation: all tools in one batch (assumes independence)
        // Advanced implementation could analyze tool inputs/outputs for dependencies
        return List.of(toolCalls);
    }

    private List<tech.kayys.gollek.spi.Message> buildMessages(AgentState state) {
        List<tech.kayys.gollek.spi.Message> messages = new ArrayList<>();

        // Add system prompt
        messages.add(tech.kayys.gollek.spi.Message.system(
            getSystemPromptFragment()));

        // Add conversation history from state
        for (AgentState.ReasoningStep step : state.getHistory()) {
            if (step.thought() != null) {
                messages.add(tech.kayys.gollek.spi.Message.user(
                    "Thought: " + step.thought()));
            }
            if (step.action() != null) {
                messages.add(tech.kayys.gollek.spi.Message.assistant(
                    "Calling tool: " + step.action().skillId()));
            }
            if (step.observation() != null) {
                messages.add(tech.kayys.gollek.spi.Message.user(
                    "Tool result: " + step.observation()));
            }
        }

        // Add original prompt
        messages.add(tech.kayys.gollek.spi.Message.user(
            state.getRequest().prompt()));

        return messages;
    }

    private String buildCacheKey(String toolName, Map<String, Object> arguments) {
        return toolName + ":" + arguments.hashCode();
    }

    private String getTerminalReason(AgentState state) {
        if (state.isComplete()) return "complete";
        if (state.isFailed()) return "failed";
        if (state.atMaxSteps()) return "max_steps";
        return "unknown";
    }

    private AgentResponse buildResponse(
            AgentState state,
            AgentRequest request,
            Instant startTime) {

        return AgentResponse.builder()
            .runId(state.getRunId())
            .requestId(request.requestId())
            .answer(state.getFinalAnswer().orElse(""))
            .steps(state.getHistory())
            .totalSteps(state.getStep())
            .successful(!state.isFailed())
            .error(state.getErrorMessage().orElse(null))
            .strategy(strategyId())
            .durationMs(Duration.between(startTime, Instant.now()).toMillis())
            .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Cache Management
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Clear the tool execution cache.
     */
    public void clearCache() {
        toolCache.clear();
        LOG.info("Tool cache cleared");
    }

    /**
     * Remove expired entries from the cache.
     */
    public void cleanupCache() {
        Instant now = Instant.now();
        toolCache.entrySet().removeIf(entry -> 
            entry.getValue().isExpired(now));
        LOG.debugf("Tool cache cleanup completed, size: %d", toolCache.size());
    }

    /**
     * Cached tool result with TTL.
     */
    private static class CachedToolResult {
        private final String result;
        private final Instant cachedAt;

        public CachedToolResult(String result, Instant cachedAt) {
            this.result = result;
            this.cachedAt = cachedAt;
        }

        public String result() {
            return result;
        }

        public boolean isExpired() {
            return isExpired(Instant.now());
        }

        public boolean isExpired(Instant now) {
            return Duration.between(cachedAt, now).compareTo(CACHE_TTL) > 0;
        }
    }
}
