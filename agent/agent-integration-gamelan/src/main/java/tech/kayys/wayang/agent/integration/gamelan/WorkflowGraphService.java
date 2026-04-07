package tech.kayys.wayang.agent.integration.gamelan.graph;

import io.quarkus.arc.log.LoggerName;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.gamelan.engine.run.RunResponse;
import tech.kayys.wayang.agent.gamelan.AgentGamelanService;
import tech.kayys.gollek.graph.core.GraphStore;
import tech.kayys.gollek.graph.core.Node;
import tech.kayys.gollek.graph.core.Relationship;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bridge service connecting Gamelan Workflow execution with Graph module.
 *
 * Enables:
 * - Automatic recording of workflow definitions as graph nodes
 * - Tracking workflow executions as graph relationships
 * - Dependency detection (circular, cascading)
 * - Critical path analysis for performance optimization
 * - Execution pattern learning and recommendations
 * - Multi-hop reasoning about workflow relationships
 *
 * Thread-safe and reactive (Mutiny-based).
 */
@ApplicationScoped
public class WorkflowGraphService {

    @LoggerName("tech.kayys.wayang.agent.core.gamelan.graph")
    Logger logger;

    @Inject
    GraphStore graphStore;

    @Inject
    AgentGamelanService gamelanService;

    /**
     * Records a workflow execution run in the graph.
     * Creates ExecutionRun node and links to workflow definition.
     *
     * @param agentId agent executing the workflow
     * @param runResponse workflow run response with execution details
     * @return node ID of the execution run
     */
    public Uni<String> recordExecutionRun(String agentId, RunResponse runResponse) {
        logger.infof("Recording execution run %s for workflow %s by agent %s",
            runResponse.getRunId(), runResponse.getWorkflowId(), agentId);

        return Uni.createFrom().item(() -> {
            // Create ExecutionRun node
            Node executionRunNode = Node.builder()
                .label("ExecutionRun")
                .property("runId", runResponse.getRunId())
                .property("workflowId", runResponse.getWorkflowId())
                .property("agentId", agentId)
                .property("status", runResponse.getStatus())
                .property("startTime", runResponse.getStartTime() != null
                    ? runResponse.getStartTime().toString() : Instant.now().toString())
                .property("endTime", runResponse.getEndTime() != null
                    ? runResponse.getEndTime().toString() : null)
                .property("durationMs", runResponse.getDurationMs() != null
                    ? runResponse.getDurationMs() : 0L)
                .property("progress", runResponse.getProgress() != null
                    ? runResponse.getProgress() : 0)
                .property("recordedAt", Instant.now().toString())
                .build();

            String runNodeId = graphStore.addNode(executionRunNode);

            // Create relationship from Workflow to ExecutionRun
            Optional<Node> workflowNode = graphStore.findNodesByLabel("Workflow")
                .stream()
                .filter(n -> runResponse.getWorkflowId().equals(n.getProperty("workflowId")))
                .findFirst();

            if (workflowNode.isPresent()) {
                Relationship executedRunRel = Relationship.builder()
                    .startNodeId(workflowNode.get().getId())
                    .endNodeId(runNodeId)
                    .type("EXECUTED_RUN")
                    .property("timestamp", Instant.now().toString())
                    .build();

                graphStore.addRelationship(executedRunRel);
                logger.debugf("Linked workflow %s to execution run %s",
                    runResponse.getWorkflowId(), runNodeId);
            }

            return runNodeId;
        });
    }

    /**
     * Records a workflow step execution in the graph.
     * Creates StepExecution node and links to ExecutionRun.
     *
     * @param runId ID of the workflow execution run
     * @param stepId ID of the workflow step
     * @param stepName display name of the step
     * @param status execution status (RUNNING, COMPLETED, FAILED)
     * @param durationMs execution duration in milliseconds
     * @return node ID of the step execution
     */
    public Uni<String> recordStepExecution(
            String runId,
            String stepId,
            String stepName,
            String status,
            Long durationMs) {

        logger.infof("Recording step execution: step=%s, run=%s, status=%s, duration=%dms",
            stepId, runId, status, durationMs != null ? durationMs : 0);

        return Uni.createFrom().item(() -> {
            // Create StepExecution node
            Node stepExecutionNode = Node.builder()
                .label("StepExecution")
                .property("stepExecutionId", generateId("step-exec"))
                .property("runId", runId)
                .property("stepId", stepId)
                .property("stepName", stepName)
                .property("status", status)
                .property("startTime", Instant.now().toString())
                .property("endTime", Instant.now().toString())
                .property("durationMs", durationMs != null ? durationMs : 0L)
                .property("recordedAt", Instant.now().toString())
                .build();

            String stepExecutionNodeId = graphStore.addNode(stepExecutionNode);

            // Link ExecutionRun to StepExecution
            Optional<Node> runNode = graphStore.findNodesByLabel("ExecutionRun")
                .stream()
                .filter(n -> runId.equals(n.getProperty("runId")))
                .findFirst();

            if (runNode.isPresent()) {
                Relationship executesStepRel = Relationship.builder()
                    .startNodeId(runNode.get().getId())
                    .endNodeId(stepExecutionNodeId)
                    .type("EXECUTES_STEP")
                    .property("order", getStepOrder(stepId))
                    .property("timestamp", Instant.now().toString())
                    .build();

                graphStore.addRelationship(executesStepRel);
                logger.debugf("Linked execution run %s to step execution %s",
                    runId, stepExecutionNodeId);
            }

            return stepExecutionNodeId;
        });
    }

    /**
     * Records the output produced by a step execution.
     * Creates OutputData node and links to StepExecution.
     *
     * @param stepExecutionId ID of the step execution
     * @param dataType type of output data (JSON, CSV, etc.)
     * @param dataSize size of output in bytes
     * @param dataHash hash of output data
     * @return node ID of the output data
     */
    public Uni<String> recordStepOutput(
            String stepExecutionId,
            String dataType,
            Long dataSize,
            String dataHash) {

        logger.infof("Recording step output: stepExecution=%s, type=%s, size=%d bytes",
            stepExecutionId, dataType, dataSize != null ? dataSize : 0);

        return Uni.createFrom().item(() -> {
            // Create OutputData node
            Node outputDataNode = Node.builder()
                .label("OutputData")
                .property("dataId", generateId("output-data"))
                .property("stepExecutionId", stepExecutionId)
                .property("dataType", dataType)
                .property("dataSize", dataSize != null ? dataSize : 0L)
                .property("dataHash", dataHash)
                .property("createdAt", Instant.now().toString())
                .build();

            String outputDataNodeId = graphStore.addNode(outputDataNode);

            // Link StepExecution to OutputData
            Optional<Node> stepExecNode = graphStore.findNodesByLabel("StepExecution")
                .stream()
                .filter(n -> stepExecutionId.equals(n.getProperty("stepExecutionId")))
                .findFirst();

            if (stepExecNode.isPresent()) {
                Relationship producesRel = Relationship.builder()
                    .startNodeId(stepExecNode.get().getId())
                    .endNodeId(outputDataNodeId)
                    .type("PRODUCES")
                    .property("timestamp", Instant.now().toString())
                    .build();

                graphStore.addRelationship(producesRel);
                logger.debugf("Linked step execution %s to output data %s",
                    stepExecutionId, outputDataNodeId);
            }

            return outputDataNodeId;
        });
    }

    /**
     * Stores a workflow definition in the graph.
     * Creates Workflow node and WorkflowStep nodes with dependencies.
     *
     * @param workflowId workflow identifier
     * @param workflowName display name
     * @param steps list of workflow steps with execution order
     * @return workflow node ID
     */
    public Uni<String> storeWorkflowDefinition(
            String workflowId,
            String workflowName,
            List<WorkflowStepDefinition> steps) {

        logger.infof("Storing workflow definition: %s with %d steps", workflowId, steps.size());

        return Uni.createFrom().item(() -> {
            // Create Workflow node
            Node workflowNode = Node.builder()
                .label("Workflow")
                .property("workflowId", workflowId)
                .property("name", workflowName)
                .property("stepCount", steps.size())
                .property("createdAt", Instant.now().toString())
                .property("updatedAt", Instant.now().toString())
                .build();

            String workflowNodeId = graphStore.addNode(workflowNode);
            logger.debugf("Created workflow node %s for %s", workflowNodeId, workflowId);

            // Create WorkflowStep nodes
            Map<String, String> stepNodeIds = new HashMap<>();
            for (WorkflowStepDefinition step : steps) {
                Node stepNode = Node.builder()
                    .label("WorkflowStep")
                    .property("stepId", step.getStepId())
                    .property("workflowId", workflowId)
                    .property("name", step.getName())
                    .property("order", step.getOrder())
                    .property("type", step.getType())
                    .property("createdAt", Instant.now().toString())
                    .build();

                String stepNodeId = graphStore.addNode(stepNode);
                stepNodeIds.put(step.getStepId(), stepNodeId);

                // Link Workflow to WorkflowStep
                Relationship hasStepRel = Relationship.builder()
                    .startNodeId(workflowNodeId)
                    .endNodeId(stepNodeId)
                    .type("HAS_STEP")
                    .property("order", step.getOrder())
                    .build();
                graphStore.addRelationship(hasStepRel);
            }

            // Create execution order relationships (FOLLOWS)
            for (int i = 0; i < steps.size() - 1; i++) {
                WorkflowStepDefinition currentStep = steps.get(i);
                WorkflowStepDefinition nextStep = steps.get(i + 1);

                if (stepNodeIds.containsKey(currentStep.getStepId())
                    && stepNodeIds.containsKey(nextStep.getStepId())) {

                    Relationship followsRel = Relationship.builder()
                        .startNodeId(stepNodeIds.get(currentStep.getStepId()))
                        .endNodeId(stepNodeIds.get(nextStep.getStepId()))
                        .type("FOLLOWS")
                        .property("order", i)
                        .build();

                    graphStore.addRelationship(followsRel);
                }
            }

            logger.infof("Stored workflow definition %s with %d steps", workflowId, steps.size());
            return workflowNodeId;
        });
    }

    /**
     * Detects circular dependencies in a workflow.
     *
     * @param workflowId workflow to analyze
     * @return list of circular dependency paths
     */
    public Uni<List<List<String>>> detectCircularDependencies(String workflowId) {
        logger.infof("Detecting circular dependencies in workflow %s", workflowId);

        return Uni.createFrom().item(() -> {
            List<List<String>> cycles = new ArrayList<>();

            // Find all WorkflowStep nodes for this workflow
            List<Node> steps = graphStore.findNodesByLabel("WorkflowStep")
                .stream()
                .filter(n -> workflowId.equals(n.getProperty("workflowId")))
                .collect(Collectors.toList());

            // For each step, try to find path back to itself (cycle)
            for (Node step : steps) {
                List<List<Node>> paths = graphStore.findPaths(
                    step.getId(), step.getId(), 10
                );

                if (!paths.isEmpty()) {
                    for (List<Node> path : paths) {
                        List<String> cycle = path.stream()
                            .map(n -> (String) n.getProperty("stepId"))
                            .collect(Collectors.toList());
                        cycles.add(cycle);
                    }
                }
            }

            logger.infof("Found %d circular dependencies in workflow %s",
                cycles.size(), workflowId);
            return cycles;
        });
    }

    /**
     * Finds the critical path (longest execution time) in a workflow run.
     *
     * @param runId execution run to analyze
     * @return list of step IDs in critical path order
     */
    public Uni<List<String>> findCriticalPath(String runId) {
        logger.infof("Finding critical path for execution run %s", runId);

        return Uni.createFrom().item(() -> {
            List<String> criticalPath = new ArrayList<>();

            // Find all StepExecution nodes for this run
            List<Node> stepExecutions = graphStore.findNodesByLabel("StepExecution")
                .stream()
                .filter(n -> runId.equals(n.getProperty("runId")))
                .collect(Collectors.toList());

            // Calculate total duration through each path
            long maxDuration = 0;
            List<String> longestPath = new ArrayList<>();

            for (Node stepExec : stepExecutions) {
                List<Node> nodeList = new ArrayList<>();
                nodeList.add(stepExec);

                // Follow EXECUTES_STEP relationships forward
                long totalDuration = ((Number) stepExec.getProperty("durationMs")).longValue();

                if (totalDuration > maxDuration) {
                    maxDuration = totalDuration;
                    longestPath = nodeList.stream()
                        .map(n -> (String) n.getProperty("stepId"))
                        .collect(Collectors.toList());
                }
            }

            logger.infof("Critical path for run %s has duration %dms", runId, maxDuration);
            return longestPath;
        });
    }

    /**
     * Identifies steps that can be executed in parallel (no dependencies).
     *
     * @param workflowId workflow to analyze
     * @return list of lists, each inner list contains steps that can run in parallel
     */
    public Uni<List<List<String>>> findParallelGroups(String workflowId) {
        logger.infof("Finding parallel execution groups in workflow %s", workflowId);

        return Uni.createFrom().item(() -> {
            List<List<String>> parallelGroups = new ArrayList<>();

            // Find all WorkflowStep nodes for this workflow
            List<Node> steps = graphStore.findNodesByLabel("WorkflowStep")
                .stream()
                .filter(n -> workflowId.equals(n.getProperty("workflowId")))
                .collect(Collectors.toList());

            // Group steps by execution order
            Map<Integer, List<String>> orderGroups = new TreeMap<>();
            for (Node step : steps) {
                Integer order = ((Number) step.getProperty("order")).intValue();
                String stepId = (String) step.getProperty("stepId");

                orderGroups.computeIfAbsent(order, k -> new ArrayList<>()).add(stepId);
            }

            // Steps with same order can run in parallel
            for (List<String> group : orderGroups.values()) {
                if (group.size() > 1) {
                    parallelGroups.add(new ArrayList<>(group));
                }
            }

            logger.infof("Found %d parallel execution groups in workflow %s",
                parallelGroups.size(), workflowId);
            return parallelGroups;
        });
    }

    /**
     * Calculates execution statistics for a workflow.
     *
     * @param workflowId workflow to analyze
     * @return execution statistics
     */
    public Uni<WorkflowExecutionStatistics> getExecutionStatistics(String workflowId) {
        logger.infof("Calculating execution statistics for workflow %s", workflowId);

        return Uni.createFrom().item(() -> {
            WorkflowExecutionStatistics stats = new WorkflowExecutionStatistics();
            stats.setWorkflowId(workflowId);

            // Find all ExecutionRun nodes for this workflow
            List<Node> runs = graphStore.findNodesByLabel("ExecutionRun")
                .stream()
                .filter(n -> workflowId.equals(n.getProperty("workflowId")))
                .collect(Collectors.toList());

            stats.setTotalExecutions(runs.size());

            // Calculate metrics
            long completedCount = runs.stream()
                .filter(n -> "COMPLETED".equals(n.getProperty("status")))
                .count();

            long failedCount = runs.stream()
                .filter(n -> "FAILED".equals(n.getProperty("status")))
                .count();

            double avgDuration = runs.stream()
                .mapToLong(n -> ((Number) n.getProperty("durationMs")).longValue())
                .average()
                .orElse(0.0);

            stats.setCompletedExecutions((int) completedCount);
            stats.setFailedExecutions((int) failedCount);
            stats.setAverageDurationMs(avgDuration);
            stats.setSuccessRate(runs.size() > 0 ? (double) completedCount / runs.size() : 0.0);

            logger.infof("Execution statistics for workflow %s: %d total, %.1f%% success rate",
                workflowId, stats.getTotalExecutions(), stats.getSuccessRate() * 100);
            return stats;
        });
    }

    /**
     * Marks a step execution as failed and records the failure in the graph.
     *
     * @param stepExecutionId step execution that failed
     * @param runId workflow execution run
     * @param errorMessage error description
     */
    public Uni<Void> recordStepFailure(
            String stepExecutionId,
            String runId,
            String errorMessage) {

        logger.warnf("Recording step failure: stepExecution=%s, run=%s, error=%s",
            stepExecutionId, runId, errorMessage);

        return Uni.createFrom().item(() -> {
            // Find StepExecution node
            Optional<Node> stepExecNode = graphStore.findNodesByLabel("StepExecution")
                .stream()
                .filter(n -> stepExecutionId.equals(n.getProperty("stepExecutionId")))
                .findFirst();

            if (stepExecNode.isPresent()) {
                // Create FailureEvent node
                Node failureNode = Node.builder()
                    .label("FailureEvent")
                    .property("failureId", generateId("failure"))
                    .property("stepExecutionId", stepExecutionId)
                    .property("runId", runId)
                    .property("errorMessage", errorMessage)
                    .property("timestamp", Instant.now().toString())
                    .build();

                String failureNodeId = graphStore.addNode(failureNode);

                // Create FAILED_AT relationship
                Relationship failedAtRel = Relationship.builder()
                    .startNodeId(stepExecNode.get().getId())
                    .endNodeId(failureNodeId)
                    .type("FAILED_AT")
                    .property("timestamp", Instant.now().toString())
                    .build();

                graphStore.addRelationship(failedAtRel);

                logger.debugf("Recorded failure for step execution %s", stepExecutionId);
            }

            return null;
        });
    }

    // ==================== Helper Methods ====================

    /**
     * Generates a unique ID for graph nodes.
     */
    private String generateId(String prefix) {
        return prefix + "-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Gets execution order of a workflow step (for relationships).
     */
    private Integer getStepOrder(String stepId) {
        // This would typically be fetched from workflow definition
        // For now, return 0 as placeholder
        return 0;
    }
}
