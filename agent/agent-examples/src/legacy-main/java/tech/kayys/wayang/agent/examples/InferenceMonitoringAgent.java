package tech.kayys.wayang.agent.examples;

import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.Agent;
import tech.kayys.wayang.agent.core.AgentContext;
import tech.kayys.wayang.agent.core.AgentResponse;
import tech.kayys.wayang.agent.spi.tool.ToolDefinition;

import java.util.*;

/**
 * Inference Monitoring Agent - Demonstrates single-agent for monitoring tasks.
 * Specializes in: health checks, performance monitoring, alerting
 */
public class InferenceMonitoringAgent implements Agent {
    private static final Logger LOGGER = Logger.getLogger(InferenceMonitoringAgent.class);
    private final String name;
    private final List<ToolDefinition> tools;

    public InferenceMonitoringAgent(String name, List<ToolDefinition> tools) {
        this.name = name;
        this.tools = tools;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return "InferenceMonitoringAgent";
    }

    @Override
    public AgentResponse execute(String query, AgentContext context) {
        long startTime = System.currentTimeMillis();
        List<AgentResponse.ReasoningStep> reasoningSteps = new ArrayList<>();
        
        try {
            reasoningSteps.add(new AgentResponse.ReasoningStep(1, "Analyzing monitoring query", "Parse request"));
            LOGGER.infof("Agent %s processing monitoring request: %s", name, query);
            
            reasoningSteps.add(new AgentResponse.ReasoningStep(2, "Collecting system metrics", "Gather data"));
            
            // Simulate metrics collection
            Map<String, Object> metrics = collectMetrics();
            reasoningSteps.add(new AgentResponse.ReasoningStep(3, "Evaluating health status", "Analyze metrics"));
            
            String healthStatus = (Boolean) metrics.get("healthy") ? "HEALTHY" : "WARNING";
            
            reasoningSteps.add(new AgentResponse.ReasoningStep(4, "Generating report", "Format output"));
            
            String answer = String.format(
                "Inference System Health Report:\n" +
                "- Status: %s\n" +
                "- Average Latency: %.2fms\n" +
                "- Success Rate: %.1f%%\n" +
                "- Models Loaded: %d\n" +
                "- Recommendation: %s",
                healthStatus,
                (Double) metrics.get("avgLatency"),
                (Double) metrics.get("successRate"),
                (Integer) metrics.get("modelsLoaded"),
                (String) metrics.get("recommendation")
            );
            
            return AgentResponse.builder()
                    .agentName(name)
                    .finalAnswer(answer)
                    .reasoningSteps(reasoningSteps)
                    .metadata(metrics)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            
        } catch (Exception e) {
            LOGGER.errorf(e, "Error in InferenceMonitoringAgent: %s", e.getMessage());
            return AgentResponse.builder()
                    .agentName(name)
                    .finalAnswer("Error: " + e.getMessage())
                    .reasoningSteps(reasoningSteps)
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public String getSystemPrompt() {
        return "You are an inference system monitor. Analyze system health, performance metrics, " +
               "and provide actionable recommendations for optimization.";
    }

    private Map<String, Object> collectMetrics() {
        return Map.of(
            "healthy", true,
            "avgLatency", 245.5,
            "successRate", 99.8,
            "modelsLoaded", 5,
            "recommendation", "System running optimally. Continue current configuration.",
            "lastUpdate", System.currentTimeMillis()
        );
    }
}
