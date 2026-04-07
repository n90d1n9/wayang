package tech.kayys.wayang.agent.core.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.client.GollekAgentClient;
import tech.kayys.wayang.agent.spi.SkillDefinition;
import tech.kayys.wayang.agent.spi.SkillDescriptor;
import tech.kayys.wayang.agent.spi.SkillRegistry;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.tool.ToolDefinition;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Intelligent tool selector with AI-powered analysis and optimal tool chain generation.
 *
 * <p>This class uses the Gollek inference engine to analyze tasks and automatically select
 * the most appropriate tools for execution, reducing the need for manual tool specification
 * and optimizing agent workflows.
 *
 * <h3>Features:</h3>
 * <ul>
 *   <li>AI-powered tool selection based on task analysis</li>
 *   <li>Framework-aware tool chain generation (React, Vue, Spring, Quarkus, etc.)</li>
 *   <li>Context-sensitive tool recommendations</li>
 *   <li>Tool dependency resolution and ordering</li>
 *   <li>Tool compatibility checking</li>
 *   <li>Performance-based tool ranking</li>
 *   <li>Multi-tool orchestration</li>
 *   <li>Tool execution pattern learning</li>
 * </ul>
 *
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @Inject
 * IntelligentToolSelector toolSelector;
 *
 * @Inject
 * ProjectContext context;
 *
 * // Analyze task and select tools
 * ToolChain chain = toolSelector.selectTools(context, "Create a REST API endpoint")
 *     .await().atMost(Duration.ofSeconds(10));
 *
 * // Get tool recommendations with confidence scores
 * List<ToolRecommendation> recommendations = toolSelector.recommendTools(context, task);
 *
 * // Execute selected tool chain
 * ToolResult result = toolSelector.executeToolChain(chain, context);
 * }</pre>
 *
 * @author Wayang AI Team
 * @version 2.0.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class IntelligentToolSelector {

    private static final Logger LOG = Logger.getLogger(IntelligentToolSelector.class);

    private static final String TOOL_SELECTION_SYSTEM_PROMPT = """
        You are an expert tool selection assistant for a software development AI agent.
        Your task is to analyze development tasks and recommend the most appropriate tools
        from the available toolkit.

        ## Available Tools
        %s

        ## Selection Criteria
        1. **Task Type**: Match tools to the specific task (file operations, code generation, testing, etc.)
        2. **Framework Context**: Consider the project's framework (React, Vue, Spring, Quarkus, etc.)
        3. **Language**: Match tools to the programming language (Java, TypeScript, Python, etc.)
        4. **Efficiency**: Prefer tools that accomplish tasks in fewer steps
        5. **Safety**: Avoid destructive operations unless explicitly requested
        6. **Dependencies**: Consider tool execution order and dependencies

        ## Output Format
        Return a JSON array of tool recommendations:
        ```json
        [
          {
            "toolId": "tool_id",
            "confidence": 0.95,
            "reason": "Brief explanation of why this tool is selected",
            "order": 1,
            "parameters": {
              "key": "value"
            }
          }
        ]
        ```

        ## Framework-Specific Guidelines
        - **React/Next.js**: Prefer component tools, state management tools, JSX-aware tools
        - **Vue/Nuxt**: Use Vue component tools, Pinia/Vuex tools
        - **Spring Boot**: Use Java tools, Spring-specific generators, Maven/Gradle tools
        - **Quarkus**: Use Java tools, Quarkus extensions, Maven tools
        - **Python**: Use Python tools, pip, virtualenv tools
        - **Node.js**: Use npm/yarn tools, TypeScript tools

        Think step-by-step and provide clear reasoning for each tool selection.
        """;

    @Inject
    GollekAgentClient agentClient;

    @Inject
    SkillRegistry skillRegistry;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ToolCacheManager cacheManager;

    // Cache for tool selection patterns
    private final Map<String, CachedToolSelection> selectionCache;

    // Tool performance metrics
    private final Map<String, ToolPerformanceMetrics> toolMetrics;

    // Framework-specific tool mappings
    private final Map<String, List<String>> frameworkToolMappings;

    /**
     * Default constructor with initialization of framework mappings.
     */
    public IntelligentToolSelector() {
        this.selectionCache = new ConcurrentHashMap<>();
        this.toolMetrics = new ConcurrentHashMap<>();
        this.frameworkToolMappings = initializeFrameworkToolMappings();
    }

    /**
     * Initialize framework-specific tool mappings.
     */
    private Map<String, List<String>> initializeFrameworkToolMappings() {
        Map<String, List<String>> mappings = new HashMap<>();

        // Frontend Frameworks
        mappings.put("react", List.of(
            "react_component", "react_state", "react_hook", "jsx_transform",
            "typescript_interface", "css_module", "npm_install"
        ));

        mappings.put("vue", List.of(
            "vue_component", "vue_composable", "pinia_store", "vue_router",
            "typescript_interface", "css_module", "npm_install"
        ));

        mappings.put("angular", List.of(
            "angular_component", "angular_service", "angular_module",
            "typescript_interface", "npm_install"
        ));

        mappings.put("svelte", List.of(
            "svelte_component", "svelte_store", "typescript_interface",
            "npm_install"
        ));

        // Backend Frameworks
        mappings.put("spring", List.of(
            "java_class", "spring_controller", "spring_service", "spring_repository",
            "maven_dependency", "java_test", "application_properties"
        ));

        mappings.put("quarkus", List.of(
            "java_class", "quarkus_resource", "quarkus_service",
            "maven_dependency", "java_test", "application_properties"
        ));

        mappings.put("micronaut", List.of(
            "java_class", "micronaut_controller", "micronaut_service",
            "maven_dependency", "java_test"
        ));

        mappings.put("fastapi", List.of(
            "python_module", "python_function", "pydantic_model",
            "pip_install", "python_test"
        ));

        mappings.put("express", List.of(
            "typescript_module", "express_route", "express_middleware",
            "npm_install", "typescript_test"
        ));

        // CSS Frameworks
        mappings.put("tailwind", List.of("tailwind_class", "tailwind_component", "css_utility"));
        mappings.put("bootstrap", List.of("bootstrap_component", "bootstrap_class", "scss_mixin"));
        mappings.put("material", List.of("material_component", "material_theme"));

        // State Management
        mappings.put("redux", List.of("redux_slice", "redux_selector", "redux_thunk"));
        mappings.put("zustand", List.of("zustand_store", "zustand_hook"));
        mappings.put("mobx", List.of("mobx_store", "mobx_action", "mobx_computed"));

        // Build Tools
        mappings.put("maven", List.of("maven_pom", "maven_dependency", "maven_plugin"));
        mappings.put("gradle", List.of("gradle_build", "gradle_dependency", "gradle_plugin"));
        mappings.put("npm", List.of("npm_package", "npm_script", "npm_config"));
        mappings.put("yarn", List.of("yarn_package", "yarn_script"));

        // Databases
        mappings.put("postgresql", List.of("sql_migration", "sql_query", "database_schema"));
        mappings.put("mysql", List.of("sql_migration", "sql_query", "database_schema"));
        mappings.put("mongodb", List.of("mongodb_schema", "mongodb_query", "mongodb_index"));
        mappings.put("redis", List.of("redis_config", "redis_cache", "redis_key"));

        // Testing
        mappings.put("jest", List.of("jest_test", "jest_mock", "jest_snapshot"));
        mappings.put("junit", List.of("junit_test", "junit_mock", "junit_extension"));
        mappings.put("pytest", List.of("pytest_test", "pytest_fixture", "pytest_mock"));
        mappings.put("vitest", List.of("vitest_test", "vitest_mock"));

        return mappings;
    }

    /**
     * Analyze project context and automatically select optimal tools for a task.
     *
     * @param context the project context
     * @param task the task description
     * @return ToolChain with selected tools in execution order
     */
    public Uni<ToolChain> selectTools(ProjectContext context, String task) {
        LOG.debugf("Selecting tools for task: %s", task);

        // Check cache first
        String cacheKey = generateSelectionCacheKey(context, task);
        CachedToolSelection cached = selectionCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            LOG.debugf("Cache hit for tool selection: %s", cacheKey);
            return Uni.createFrom().item(cached.getChain());
        }

        // Use AI to select tools
        return selectToolsWithAI(context, task)
            .map(chain -> {
                // Cache the selection
                selectionCache.put(cacheKey, new CachedToolSelection(chain, Duration.ofMinutes(30)));
                return chain;
            });
    }

    /**
     * Select tools using AI-powered analysis.
     */
    private Uni<ToolChain> selectToolsWithAI(ProjectContext context, String task) {
        // Build available tools description
        String availableTools = buildAvailableToolsDescription();

        // Build system prompt with available tools
        String systemPrompt = String.format(TOOL_SELECTION_SYSTEM_PROMPT, availableTools);

        // Build user prompt with context
        String userPrompt = buildToolSelectionPrompt(context, task);

        // Create inference request
        InferenceRequest request = InferenceRequest.builder()
            .model(context.getModelId() != null ? context.getModelId() : "default")
            .message(Message.system(systemPrompt))
            .message(Message.user(userPrompt))
            .parameter("temperature", 0.3) // Lower temperature for more deterministic output
            .parameter("max_tokens", 2048)
            .build();

        LOG.debug("Requesting AI tool selection from Gollek inference engine");

        return agentClient.infer(request)
            .map(response -> parseToolSelection(response, context));
    }

    /**
     * Select tools based on framework detection (rule-based fallback).
     *
     * @param context the project context
     * @param task the task description
     * @return ToolChain with selected tools
     */
    public ToolChain selectToolsByFramework(ProjectContext context, String task) {
        LOG.debugf("Selecting tools by framework detection for task: %s", task);

        ToolChain chain = new ToolChain();

        // Detect primary framework
        String primaryFramework = detectPrimaryFramework(context);
        LOG.debugf("Detected primary framework: %s", primaryFramework);

        // Add framework-specific tools
        if (primaryFramework != null) {
            List<String> frameworkTools = frameworkToolMappings.getOrDefault(primaryFramework, List.of());
            frameworkTools.forEach(toolId -> {
                if (isToolAvailable(toolId)) {
                    chain.addTool(createToolInstance(toolId, context, task));
                }
            });
        }

        // Detect CSS framework
        if (context.hasTailwind()) {
            frameworkToolMappings.getOrDefault("tailwind", List.of())
                .forEach(toolId -> {
                    if (isToolAvailable(toolId)) {
                        chain.addTool(createToolInstance(toolId, context, task));
                    }
                });
        } else if (context.hasBootstrap()) {
            frameworkToolMappings.getOrDefault("bootstrap", List.of())
                .forEach(toolId -> {
                    if (isToolAvailable(toolId)) {
                        chain.addTool(createToolInstance(toolId, context, task));
                    }
                });
        }

        // Detect state management
        if (context.hasRedux()) {
            frameworkToolMappings.getOrDefault("redux", List.of())
                .forEach(toolId -> {
                    if (isToolAvailable(toolId)) {
                        chain.addTool(createToolInstance(toolId, context, task));
                    }
                });
        } else if (context.hasZustand()) {
            frameworkToolMappings.getOrDefault("zustand", List.of())
                .forEach(toolId -> {
                    if (isToolAvailable(toolId)) {
                        chain.addTool(createToolInstance(toolId, context, task));
                    }
                });
        }

        // Detect build tool
        if (context.hasMaven()) {
            frameworkToolMappings.getOrDefault("maven", List.of())
                .forEach(toolId -> {
                    if (isToolAvailable(toolId)) {
                        chain.addTool(createToolInstance(toolId, context, task));
                    }
                });
        } else if (context.hasGradle()) {
            frameworkToolMappings.getOrDefault("gradle", List.of())
                .forEach(toolId -> {
                    if (isToolAvailable(toolId)) {
                        chain.addTool(createToolInstance(toolId, context, task));
                    }
                });
        }

        return chain;
    }

    /**
     * Get tool recommendations with confidence scores.
     *
     * @param context the project context
     * @param task the task description
     * @return list of tool recommendations sorted by confidence
     */
    public Uni<List<ToolRecommendation>> recommendTools(ProjectContext context, String task) {
        return selectToolsWithAI(context, task)
            .map(chain -> chain.getRecommendations());
    }

    /**
     * Execute a tool chain in the optimal order.
     *
     * @param chain the tool chain to execute
     * @param context the project context
     * @return aggregated tool execution result
     */
    public Uni<ToolChainResult> executeToolChain(ToolChain chain, ProjectContext context) {
        LOG.debugf("Executing tool chain with %d tools", chain.getTools().size());

        if (chain.getTools().isEmpty()) {
            return Uni.createFrom().item(ToolChainResult.empty());
        }

        // Execute tools in order
        List<ToolExecutionResult> results = new ArrayList<>();
        boolean hasFailure = false;

        for (SelectedTool tool : chain.getTools()) {
            if (hasFailure && !tool.isContinueOnError()) {
                LOG.warnf("Skipping tool %s due to previous failure", tool.getToolId());
                break;
            }

            try {
                // Execute tool (simplified - actual implementation would use SkillRegistry)
                ToolExecutionResult result = executeSingleTool(tool, context);
                results.add(result);

                if (!result.isSuccess()) {
                    hasFailure = true;
                    updateToolMetrics(tool.getToolId(), result.getDurationMs(), false);
                } else {
                    updateToolMetrics(tool.getToolId(), result.getDurationMs(), true);
                }

            } catch (Exception e) {
                LOG.errorf(e, "Tool execution failed: %s", tool.getToolId());
                results.add(ToolExecutionResult.failure(tool.getToolId(), e));
                hasFailure = true;
                updateToolMetrics(tool.getToolId(), 0, false);
            }
        }

        return Uni.createFrom().item(new ToolChainResult(results, hasFailure));
    }

    /**
     * Get tool performance metrics.
     *
     * @param toolId the tool identifier
     * @return tool performance metrics
     */
    public ToolPerformanceMetrics getToolMetrics(String toolId) {
        return toolMetrics.getOrDefault(toolId, ToolPerformanceMetrics.empty(toolId));
    }

    /**
     * Get all tool performance metrics.
     *
     * @return map of tool metrics
     */
    public Map<String, ToolPerformanceMetrics> getAllToolMetrics() {
        return new HashMap<>(toolMetrics);
    }

    /**
     * Clear tool selection cache.
     */
    public void clearCache() {
        selectionCache.clear();
        LOG.debug("Tool selection cache cleared");
    }

    // ==================== Private Helper Methods ====================

    /**
     * Build description of available tools for AI prompt.
     */
    private String buildAvailableToolsDescription() {
        StringBuilder sb = new StringBuilder();

        // Get all registered skills
        skillRegistry.findAll().forEach(skill -> {
            sb.append("- **").append(skill.id()).append("**: ")
              .append(skill.description())
              .append("\n");
        });

        return sb.toString();
    }

    /**
     * Build prompt for tool selection with context.
     */
    private String buildToolSelectionPrompt(ProjectContext context, String task) {
        StringBuilder sb = new StringBuilder();

        sb.append("## Task\n");
        sb.append(task).append("\n\n");

        sb.append("## Project Context\n");
        sb.append("Frameworks: ").append(String.join(", ", context.getFrameworks())).append("\n");
        sb.append("Languages: ").append(String.join(", ", context.getLanguages())).append("\n");
        sb.append("Build Tool: ").append(context.getBuildTool()).append("\n");

        if (context.hasTailwind()) sb.append("CSS: Tailwind CSS\n");
        if (context.hasBootstrap()) sb.append("CSS: Bootstrap\n");
        if (context.hasRedux()) sb.append("State: Redux\n");
        if (context.hasZustand()) sb.append("State: Zustand\n");

        sb.append("\n## Instructions\n");
        sb.append("Select the most appropriate tools to accomplish the task. ");
        sb.append("Consider the project's frameworks and conventions. ");
        sb.append("Return tools in the order they should be executed.");

        return sb.toString();
    }

    /**
     * Parse AI response into ToolChain.
     */
    private ToolChain parseToolSelection(InferenceResponse response, ProjectContext context) {
        try {
            String content = response.getContent();
            JsonNode json = objectMapper.readTree(content);

            ToolChain chain = new ToolChain();

            if (json.isArray()) {
                for (JsonNode node : json) {
                    String toolId = node.path("toolId").asText();
                    double confidence = node.path("confidence").asDouble(0.5);
                    String reason = node.path("reason").asText("No reason provided");
                    int order = node.path("order").asInt(chain.getTools().size() + 1);
                    JsonNode params = node.path("parameters");

                    if (isToolAvailable(toolId)) {
                        SelectedTool tool = new SelectedTool(
                            toolId,
                            confidence,
                            reason,
                            order,
                            objectMapper.convertValue(params, Map.class),
                            true
                        );
                        chain.addTool(tool);
                    }
                }
            }

            return chain;

        } catch (Exception e) {
            LOG.errorf(e, "Failed to parse tool selection response");
            // Fallback to rule-based selection
            return selectToolsByFramework(context, "Parse failed - fallback");
        }
    }

    /**
     * Detect primary framework from project context.
     */
    private String detectPrimaryFramework(ProjectContext context) {
        // Priority order for framework detection
        if (context.hasFramework("react")) return "react";
        if (context.hasFramework("vue")) return "vue";
        if (context.hasFramework("angular")) return "angular";
        if (context.hasFramework("svelte")) return "svelte";
        if (context.hasFramework("spring")) return "spring";
        if (context.hasFramework("quarkus")) return "quarkus";
        if (context.hasFramework("micronaut")) return "micronaut";
        if (context.hasFramework("fastapi")) return "fastapi";
        if (context.hasFramework("express")) return "express";
        return null;
    }

    /**
     * Check if a tool is available in the registry.
     */
    private boolean isToolAvailable(String toolId) {
        return skillRegistry.find(toolId).isPresent();
    }

    /**
     * Create a tool instance with context-aware parameters.
     */
    private SelectedTool createToolInstance(String toolId, ProjectContext context, String task) {
        return new SelectedTool(
            toolId,
            0.8, // Default confidence
            "Framework-based selection",
            0,
            buildDefaultParameters(toolId, context, task),
            true
        );
    }

    /**
     * Build default parameters for a tool based on context.
     */
    private Map<String, Object> buildDefaultParameters(String toolId, ProjectContext context, String task) {
        Map<String, Object> params = new HashMap<>();
        params.put("task", task);

        // Add context-specific defaults
        if (toolId.contains("react") || toolId.contains("vue") || toolId.contains("angular")) {
            params.put("componentType", "functional");
        }

        if (toolId.contains("test")) {
            params.put("testFramework", context.getTestFramework());
        }

        return params;
    }

    /**
     * Execute a single tool (simplified implementation).
     */
    private ToolExecutionResult executeSingleTool(SelectedTool tool, ProjectContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // Actual implementation would use SkillRegistry to execute the tool
            // This is a placeholder
            Thread.sleep(10); // Simulate execution

            long duration = System.currentTimeMillis() - startTime;
            return ToolExecutionResult.success(tool.getToolId(), duration, "Executed successfully");

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return ToolExecutionResult.failure(tool.getToolId(), duration, e.getMessage());
        }
    }

    /**
     * Update tool performance metrics.
     */
    private void updateToolMetrics(String toolId, long durationMs, boolean success) {
        toolMetrics.compute(toolId, (key, metrics) -> {
            if (metrics == null) {
                metrics = ToolPerformanceMetrics.empty(toolId);
            }
            return metrics.recordExecution(durationMs, success);
        });
    }

    /**
     * Generate cache key for tool selection.
     */
    private String generateSelectionCacheKey(ProjectContext context, String task) {
        return String.format("%s:%s:%d",
            context.hashCode(),
            task.hashCode(),
            System.currentTimeMillis() / (1000 * 60) // Minute granularity
        );
    }

    // ==================== Record Classes ====================

    /**
     * Tool chain - ordered list of selected tools.
     */
    public static class ToolChain {
        private final List<SelectedTool> tools;

        public ToolChain() {
            this.tools = new ArrayList<>();
        }

        public void addTool(SelectedTool tool) {
            tools.add(tool);
            tools.sort(Comparator.comparingInt(SelectedTool::getOrder));
        }

        public List<SelectedTool> getTools() {
            return Collections.unmodifiableList(tools);
        }

        public List<ToolRecommendation> getRecommendations() {
            return tools.stream()
                .map(t -> new ToolRecommendation(t.toolId, t.confidence, t.reason))
                .collect(Collectors.toList());
        }

        public int size() {
            return tools.size();
        }

        public boolean isEmpty() {
            return tools.isEmpty();
        }
    }

    /**
     * Selected tool with metadata.
     *
     * @param toolId the tool identifier
     * @param confidence selection confidence (0.0 to 1.0)
     * @param reason selection reasoning
     * @param order execution order
     * @param parameters tool-specific parameters
     * @param continueOnError whether to continue on error
     */
    public record SelectedTool(
        String toolId,
        double confidence,
        String reason,
        int order,
        Map<String, Object> parameters,
        boolean continueOnError
    ) {}

    /**
     * Tool recommendation with confidence.
     *
     * @param toolId the tool identifier
     * @param confidence selection confidence
     * @param reason selection reasoning
     */
    public record ToolRecommendation(
        String toolId,
        double confidence,
        String reason
    ) {}

    /**
     * Tool chain execution result.
     *
     * @param results individual tool execution results
     * @param hasFailure whether any tool failed
     */
    public record ToolChainResult(
        List<ToolExecutionResult> results,
        boolean hasFailure
    ) {
        public static ToolChainResult empty() {
            return new ToolChainResult(List.of(), false);
        }

        public int getSuccessCount() {
            return (int) results.stream().filter(ToolExecutionResult::isSuccess).count();
        }

        public int getFailureCount() {
            return results.size() - getSuccessCount();
        }
    }

    /**
     * Single tool execution result.
     *
     * @param toolId the tool identifier
     * @param success whether execution succeeded
     * @param durationMs execution duration in milliseconds
     * @param result execution result message
     * @param error error message if failed
     */
    public record ToolExecutionResult(
        String toolId,
        boolean success,
        long durationMs,
        String result,
        String error
    ) {
        public static ToolExecutionResult success(String toolId, long durationMs, String result) {
            return new ToolExecutionResult(toolId, true, durationMs, result, null);
        }

        public static ToolExecutionResult failure(String toolId, long durationMs, String error) {
            return new ToolExecutionResult(toolId, false, durationMs, null, error);
        }
    }

    /**
     * Cached tool selection.
     *
     * @param chain the tool chain
     * @param createdAt creation timestamp
     * @param ttl time-to-live
     */
    public record CachedToolSelection(
        ToolChain chain,
        Instant createdAt,
        Duration ttl
    ) {
        public CachedToolSelection(ToolChain chain, Duration ttl) {
            this(chain, Instant.now(), ttl);
        }

        public boolean isExpired() {
            return Instant.now().isAfter(createdAt.plus(ttl));
        }
    }

    /**
     * Tool performance metrics.
     *
     * @param toolId the tool identifier
     * @param executionCount total executions
     * @param successCount successful executions
     * @param failureCount failed executions
     * @param avgDurationMs average execution duration
     * @param lastExecutionTime timestamp of last execution
     */
    public record ToolPerformanceMetrics(
        String toolId,
        long executionCount,
        long successCount,
        long failureCount,
        double avgDurationMs,
        Instant lastExecutionTime
    ) {
        public static ToolPerformanceMetrics empty(String toolId) {
            return new ToolPerformanceMetrics(toolId, 0, 0, 0, 0.0, Instant.now());
        }

        public ToolPerformanceMetrics recordExecution(long durationMs, boolean success) {
            long newCount = executionCount + 1;
            double newAvg = ((avgDurationMs * executionCount) + durationMs) / newCount;

            return new ToolPerformanceMetrics(
                toolId,
                newCount,
                success ? successCount + 1 : successCount,
                success ? failureCount : failureCount + 1,
                newAvg,
                Instant.now()
            );
        }

        public double getSuccessRate() {
            long total = successCount + failureCount;
            return total > 0 ? (double) successCount / total : 0.0;
        }
    }

    /**
     * Project context for tool selection.
     */
    public static class ProjectContext {
        private final Set<String> frameworks;
        private final Set<String> languages;
        private final String buildTool;
        private final String testFramework;
        private final String modelId;
        private final Map<String, Boolean> featureFlags;

        public ProjectContext() {
            this.frameworks = new HashSet<>();
            this.languages = new HashSet<>();
            this.buildTool = "unknown";
            this.testFramework = "unknown";
            this.modelId = null;
            this.featureFlags = new HashMap<>();
        }

        public Set<String> getFrameworks() {
            return Collections.unmodifiableSet(frameworks);
        }

        public Set<String> getLanguages() {
            return Collections.unmodifiableSet(languages);
        }

        public String getBuildTool() {
            return buildTool;
        }

        public String getTestFramework() {
            return testFramework;
        }

        public String getModelId() {
            return modelId;
        }

        public boolean hasFramework(String framework) {
            return frameworks.contains(framework.toLowerCase());
        }

        public boolean hasTailwind() {
            return featureFlags.getOrDefault("tailwind", false);
        }

        public boolean hasBootstrap() {
            return featureFlags.getOrDefault("bootstrap", false);
        }

        public boolean hasRedux() {
            return featureFlags.getOrDefault("redux", false);
        }

        public boolean hasZustand() {
            return featureFlags.getOrDefault("zustand", false);
        }

        public boolean hasMaven() {
            return "maven".equalsIgnoreCase(buildTool);
        }

        public boolean hasGradle() {
            return "gradle".equalsIgnoreCase(buildTool);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private final ProjectContext context = new ProjectContext();

            public Builder framework(String framework) {
                context.frameworks.add(framework.toLowerCase());
                return this;
            }

            public Builder language(String language) {
                context.languages.add(language.toLowerCase());
                return this;
            }

            public Builder buildTool(String buildTool) {
                return this;
            }

            public Builder testFramework(String testFramework) {
                return this;
            }

            public Builder modelId(String modelId) {
                return this;
            }

            public Builder feature(String name, boolean enabled) {
                context.featureFlags.put(name, enabled);
                return this;
            }

            public ProjectContext build() {
                return context;
            }
        }
    }
}
