package tech.kayys.wayang.agent.core.coordinator;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.client.GollekAgentClient;
import tech.kayys.wayang.agent.core.service.AgenticInferenceService;
import tech.kayys.wayang.agent.spi.*;
import tech.kayys.gollek.spi.Message;
import tech.kayys.gollek.spi.inference.InferenceRequest;
import tech.kayys.gollek.spi.inference.InferenceResponse;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Multi-agent collaboration coordinator for complex task decomposition and distribution.
 *
 * <p>This coordinator enables multiple specialized agents to work together on complex tasks by:
 * <ul>
 *   <li>Decomposing tasks into subtasks for different agents</li>
 *   <li>Coordinating inter-agent communication</li>
 *   <li>Aggregating results from multiple agents</li>
 *   <li>Managing agent roles and responsibilities</li>
 *   <li>Handling conflicts and consensus building</li>
 * </ul>
 *
 * <h2>Agent Roles:</h2>
 * <ul>
 *   <li><b>Planner:</b> Breaks down complex tasks into subtasks</li>
 *   <li><b>Executor:</b> Executes specific subtasks</li>
 *   <li><b>Reviewer:</b> Validates and critiques results</li>
 *   <li><b>Synthesizer:</b> Combines results into final answer</li>
 * </ul>
 *
 * <h2>Coordination Patterns:</h2>
 * <ul>
 *   <li><b>Sequential:</b> Agents work in sequence, each building on previous results</li>
 *   <li><b>Parallel:</b> Multiple agents work on independent subtasks simultaneously</li>
 *   <li><b>Hierarchical:</b> Manager agent delegates to worker agents</li>
 *   <li><b>Consensus:</b> Multiple agents vote on best solution</li>
 * </ul>
 *
 * @author Wayang AI Team
 * @version 0.1.0
 * @since 2026-03-28
 */
@ApplicationScoped
public class MultiAgentCoordinator {

    private static final Logger LOG = Logger.getLogger(MultiAgentCoordinator.class);

    @Inject
    GollekAgentClient agentClient;

    @Inject
    AgenticInferenceService inferenceService;

    @Inject
    Instance<AgentOrchestrator> availableOrchestrators;

    // Registered agents with their roles and capabilities
    private final Map<String, AgentInstance> registeredAgents = new ConcurrentHashMap<>();
    private final Map<String, AgentRole> agentRoles = new ConcurrentHashMap<>();

    // Agent types
    public static final String ROLE_PLANNER = "planner";
    public static final String ROLE_EXECUTOR = "executor";
    public static final String ROLE_REVIEWER = "reviewer";
    public static final String ROLE_SYNTHESIZER = "synthesizer";
    public static final String ROLE_SPECIALIST = "specialist";

    /**
     * Register an agent instance with a specific role.
     *
     * @param agentId unique identifier for this agent instance
     * @param role the agent's role
     * @param orchestrator the orchestrator strategy for this agent
     * @param modelId the model ID to use
     */
    public void registerAgent(
            String agentId,
            String role,
            AgentOrchestrator orchestrator,
            String modelId) {
        
        AgentInstance agent = new AgentInstance(
            agentId, role, orchestrator, modelId, true);
        registeredAgents.put(agentId, agent);
        agentRoles.put(agentId, AgentRole.fromRoleString(role));
        
        LOG.infof("Registered agent %s with role %s", agentId, role);
    }

    /**
     * Register an agent with default orchestrator based on role.
     *
     * @param agentId unique identifier for this agent instance
     * @param role the agent's role
     * @param modelId the model ID to use
     */
    public void registerAgent(String agentId, String role, String modelId) {
        AgentOrchestrator orchestrator = selectOrchestratorForRole(role);
        registerAgent(agentId, role, orchestrator, modelId);
    }

    /**
     * Unregister an agent instance.
     *
     * @param agentId the agent ID to unregister
     */
    public void unregisterAgent(String agentId) {
        registeredAgents.remove(agentId);
        agentRoles.remove(agentId);
        LOG.infof("Unregistered agent %s", agentId);
    }

    /**
     * Execute a multi-agent workflow with automatic task decomposition.
     *
     * @param request the agent request
     * @return Uni containing the aggregated response
     */
    public Uni<AgentResponse> executeMultiAgent(AgentRequest request) {
        LOG.infof("Starting multi-agent execution for request %s", request.requestId());

        Instant startTime = Instant.now();
        String runId = UUID.randomUUID().toString();

        // Ensure we have the required agents
        ensureAgentsRegistered();

        // Phase 1: Planning
        return planTask(request, runId)
            .chain(plan -> {
                LOG.infof("Task plan: %d subtasks", plan.subtasks().size());
                
                // Phase 2: Execute subtasks with appropriate agents
                return executeSubtasks(request, plan, runId);
            })
            .chain(results -> {
                // Phase 3: Review results
                return reviewResults(request, results, runId);
            })
            .chain(reviewedResults -> {
                // Phase 4: Synthesize final answer
                return synthesizeFinalAnswer(request, reviewedResults, runId);
            })
            .map(finalAnswer -> buildResponse(finalAnswer, request, runId, startTime))
            .onFailure().recoverWithUni(err -> {
                LOG.errorf(err, "Multi-agent execution failed");
                return Uni.createFrom().item(AgentResponse.builder()
                        .runId(runId)
                        .requestId(request.requestId())
                        .answer("Error: " + err.getMessage())
                        .steps(List.of())
                        .totalSteps(0)
                        .successful(false)
                        .error(err.getMessage())
                        .strategy("multi-agent")
                        .durationMs(Duration.between(startTime, Instant.now()).toMillis())
                        .build());
            });
    }

    /**
     * Execute a specific subtask with a designated agent.
     *
     * @param subtask the subtask to execute
     * @param agentId the agent to execute it with
     * @param parentRunId the parent run ID for tracking
     * @return Uni containing the subtask result
     */
    public Uni<SubtaskResult> executeSubtask(
            Subtask subtask,
            String agentId,
            String parentRunId) {
        
        AgentInstance agent = registeredAgents.get(agentId);
        if (agent == null) {
            return Uni.createFrom().failure(
                new IllegalStateException("Agent not found: " + agentId));
        }

        LOG.infof("Executing subtask '%s' with agent %s (%s)", 
            subtask.name(), agentId, agent.role());

        AgentRequest subtaskRequest = AgentRequest.builder()
            .requestId(parentRunId + "-" + subtask.name())
            .tenantId(parentRunId) // Use parent run ID as tenant for grouping
            .prompt(subtask.description())
            .modelId(agent.modelId())
            .context(Map.of(
                "parent_run_id", parentRunId,
                "subtask_name", subtask.name(),
                "subtask_index", subtask.index()
            ))
            .build();

        Instant startTime = Instant.now();

        return agent.orchestrator().execute(subtaskRequest)
            .map(response -> new SubtaskResult(
                subtask.name(),
                response.answer(),
                response.totalSteps(),
                response.successful(),
                Duration.between(startTime, Instant.now()).toMillis(),
                subtask.assignedTo()
            ));
    }

    /**
     * Get agents by role.
     *
     * @param role the role to filter by
     * @return list of agent IDs with the specified role
     */
    public List<String> getAgentsByRole(String role) {
        return registeredAgents.entrySet().stream()
            .filter(e -> role.equals(e.getValue().role()))
            .filter(e -> e.getValue().enabled())
            .map(Map.Entry::getKey)
            .toList();
    }

    /**
     * Get the best agent for a specific task type.
     *
     * @param taskType the type of task
     * @return agent ID or null if none available
     */
    public String selectAgentForTask(String taskType) {
        // Simple heuristic: select based on task type
        String role = switch (taskType.toLowerCase()) {
            case "planning", "decomposition" -> ROLE_PLANNER;
            case "execution", "implementation" -> ROLE_EXECUTOR;
            case "review", "validation", "critique" -> ROLE_REVIEWER;
            case "synthesis", "aggregation" -> ROLE_SYNTHESIZER;
            default -> ROLE_SPECIALIST;
        };

        List<String> agents = getAgentsByRole(role);
        return agents.isEmpty() ? null : agents.get(0);
    }

    /**
     * Enable or disable an agent instance.
     *
     * @param agentId the agent ID
     * @param enabled whether to enable or disable
     */
    public void setAgentEnabled(String agentId, boolean enabled) {
        AgentInstance agent = registeredAgents.get(agentId);
        if (agent != null) {
            registeredAgents.put(agentId, 
                new AgentInstance(agent.id(), agent.role(), 
                    agent.orchestrator(), agent.modelId(), enabled));
            LOG.infof("Agent %s %s", agentId, enabled ? "enabled" : "disabled");
        }
    }

    /**
     * Get status of all registered agents.
     *
     * @return map of agent ID to status
     */
    public Map<String, AgentStatus> getAgentStatus() {
        return registeredAgents.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> new AgentStatus(
                    e.getValue().role(),
                    e.getValue().enabled(),
                    e.getValue().modelId()
                )
            ));
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Internal Methods
    // ═══════════════════════════════════════════════════════════════════════

    private Uni<TaskPlan> planTask(AgentRequest request, String runId) {
        String plannerAgent = getAgentsByRole(ROLE_PLANNER).stream().findFirst()
            .orElseGet(() -> {
                // Auto-register a planner if none exists
                String id = "auto-planner-" + UUID.randomUUID().toString().substring(0, 8);
                registerAgent(id, ROLE_PLANNER, "default");
                return id;
            });

        String planningPrompt = """
            Break down the following task into clear, executable subtasks.
            
            Original task: %s
            
            For each subtask, specify:
            1. Name: Short identifier
            2. Description: What needs to be done
            3. Assigned role: planner, executor, reviewer, or synthesizer
            
            Format your response as a numbered list.
            """.formatted(request.prompt());

        return executeWithAgent(plannerAgent, planningPrompt, runId + "-plan")
            .map(response -> parseTaskPlan(response));
    }

    private Uni<List<SubtaskResult>> executeSubtasks(
            AgentRequest request,
            TaskPlan plan,
            String runId) {

        List<Uni<SubtaskResult>> subtaskUnis = plan.subtasks().stream()
            .map(subtask -> {
                String agentId = subtask.assignedTo() != null 
                    ? subtask.assignedTo()
                    : selectAgentForTask(subtask.type());
                
                if (agentId == null) {
                    // Default to executor role
                    agentId = getAgentsByRole(ROLE_EXECUTOR).stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException(
                            "No executor agent available"));
                }

                return executeSubtask(subtask, agentId, runId);
            })
            .toList();

        return Uni.combine().all().unis(subtaskUnis)
            .combinedWith(results -> {
                @SuppressWarnings("unchecked")
                List<SubtaskResult> typedResults = (List<SubtaskResult>) results;
                return typedResults;
            });
    }

    private Uni<List<SubtaskResult>> reviewResults(
            AgentRequest request,
            List<SubtaskResult> results,
            String runId) {

        String reviewerAgent = getAgentsByRole(ROLE_REVIEWER).stream().findFirst().orElse(null);
        if (reviewerAgent == null) {
            // No reviewer - return results as-is
            return Uni.createFrom().item(results);
        }

        String reviewPrompt = """
            Review the following subtask results for quality and accuracy.
            Identify any errors, inconsistencies, or improvements needed.
            
            Subtask results:
            %s
            
            Provide a brief assessment of each result's quality.
            """.formatted(formatResults(results));

        return executeWithAgent(reviewerAgent, reviewPrompt, runId + "-review")
            .map(review -> {
                // For now, return original results (could apply corrections based on review)
                LOG.infof("Review completed: %s", review);
                return results;
            });
    }

    private Uni<String> synthesizeFinalAnswer(
            AgentRequest request,
            List<SubtaskResult> results,
            String runId) {

        String synthesizerAgent = getAgentsByRole(ROLE_SYNTHESIZER).stream().findFirst()
            .orElseGet(() -> {
                String id = "auto-synthesizer-" + UUID.randomUUID().toString().substring(0, 8);
                registerAgent(id, ROLE_SYNTHESIZER, "default");
                return id;
            });

        String synthesizePrompt = """
            Synthesize the following subtask results into a coherent final answer.
            
            Original question: %s
            
            Subtask results:
            %s
            
            Combine these results into a comprehensive, well-structured final answer.
            """.formatted(request.prompt(), formatResults(results));

        return executeWithAgent(synthesizerAgent, synthesizePrompt, runId + "-synthesize");
    }

    private Uni<String> executeWithAgent(String agentId, String prompt, String requestId) {
        AgentInstance agent = registeredAgents.get(agentId);
        if (agent == null) {
            return Uni.createFrom().failure(
                new IllegalStateException("Agent not found: " + agentId));
        }

        AgentRequest request = AgentRequest.builder()
            .requestId(requestId)
            .tenantId(requestId)
            .prompt(prompt)
            .modelId(agent.modelId())
            .build();

        return agent.orchestrator().execute(request)
            .map(response -> response.answer());
    }

    private void ensureAgentsRegistered() {
        if (registeredAgents.isEmpty()) {
            LOG.info("No agents registered - auto-registering default agents");
            registerAgent("default-planner", ROLE_PLANNER, "default");
            registerAgent("default-executor", ROLE_EXECUTOR, "default");
            registerAgent("default-synthesizer", ROLE_SYNTHESIZER, "default");
        }
    }

    private AgentOrchestrator selectOrchestratorForRole(String role) {
        // Select appropriate orchestrator based on role
        return switch (role) {
            case ROLE_PLANNER -> availableOrchestrators.stream()
                .filter(o -> "plan-and-execute".equals(o.strategyId()))
                .findFirst()
                .orElse(availableOrchestrators.iterator().next());
            case ROLE_REVIEWER -> availableOrchestrators.stream()
                .filter(o -> "reflexion".equals(o.strategyId()))
                .findFirst()
                .orElse(availableOrchestrators.iterator().next());
            default -> availableOrchestrators.iterator().next();
        };
    }

    private TaskPlan parseTaskPlan(String planText) {
        // Simple parsing - in production, use structured output
        List<Subtask> subtasks = new ArrayList<>();
        String[] lines = planText.split("\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.matches("^\\d+\\..*")) {
                subtasks.add(new Subtask(
                    "task-" + i,
                    line.replaceAll("^\\d+\\.\\s*", ""),
                    "execution",
                    i,
                    null
                ));
            }
        }
        
        return new TaskPlan(subtasks);
    }

    private String formatResults(List<SubtaskResult> results) {
        return results.stream()
            .map(r -> String.format("- %s: %s", r.subtaskName(), r.result()))
            .collect(Collectors.joining("\n"));
    }

    private AgentResponse buildResponse(
            String finalAnswer,
            AgentRequest request,
            String runId,
            Instant startTime) {

        return AgentResponse.builder()
            .runId(runId)
            .requestId(request.requestId())
            .answer(finalAnswer)
            .totalSteps(registeredAgents.size()) // Number of agents involved
            .successful(true)
            .strategy("multi-agent")
            .durationMs(Duration.between(startTime, Instant.now()).toMillis())
            .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // Records and Classes
    // ═══════════════════════════════════════════════════════════════════════

    public record AgentInstance(
        String id,
        String role,
        AgentOrchestrator orchestrator,
        String modelId,
        boolean enabled
    ) {}

    public record AgentStatus(
        String role,
        boolean enabled,
        String modelId
    ) {}

    public record TaskPlan(List<Subtask> subtasks) {}

    public record Subtask(
        String name,
        String description,
        String type,
        int index,
        String assignedTo
    ) {}

    public record SubtaskResult(
        String subtaskName,
        String result,
        int steps,
        boolean successful,
        long durationMs,
        String executedBy
    ) {}

    public enum AgentRole {
        PLANNER, EXECUTOR, REVIEWER, SYNTHESIZER, SPECIALIST;

        public static AgentRole fromRoleString(String role) {
            try {
                return valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                return SPECIALIST;
            }
        }
    }
}
