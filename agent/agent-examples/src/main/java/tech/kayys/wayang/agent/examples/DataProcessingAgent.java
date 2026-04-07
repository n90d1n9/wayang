package tech.kayys.wayang.agent.examples;

import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.core.Agent;
import tech.kayys.wayang.agent.core.AgentContext;
import tech.kayys.wayang.agent.core.AgentResponse;
import tech.kayys.wayang.agent.spi.tool.ToolDefinition;

import java.util.*;

/**
 * Data Processing Agent - Demonstrates single-agent pattern for data operations.
 * Specializes in: query analysis, data transformation, output formatting
 */
public class DataProcessingAgent implements Agent {
    private static final Logger LOGGER = Logger.getLogger(DataProcessingAgent.class);
    private final String name;
    private final List<ToolDefinition> tools;

    public DataProcessingAgent(String name, List<ToolDefinition> tools) {
        this.name = name;
        this.tools = tools;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getType() {
        return "DataProcessingAgent";
    }

    @Override
    public AgentResponse execute(String query, AgentContext context) {
        long startTime = System.currentTimeMillis();
        List<AgentResponse.ReasoningStep> reasoningSteps = new ArrayList<>();
        
        try {
            reasoningSteps.add(new AgentResponse.ReasoningStep(1, "Parse data query", "Analyze structure"));
            LOGGER.infof("Agent %s processing query: %s", name, query);
            
            reasoningSteps.add(new AgentResponse.ReasoningStep(2, "Validate data source", "Check availability"));
            if (!query.toLowerCase().contains("from") && 
                !query.toLowerCase().contains("load") && 
                !query.toLowerCase().contains("query")) {
                reasoningSteps.add(new AgentResponse.ReasoningStep(3, "No valid data operation", "Treat as search"));
            }
            
            reasoningSteps.add(new AgentResponse.ReasoningStep(4, "Select appropriate tools", "Match query intent"));
            List<String> selectedTools = new ArrayList<>();
            if (query.toLowerCase().contains("load")) {
                selectedTools.add("load-model-from-repository");
            }
            if (query.toLowerCase().contains("config")) {
                selectedTools.add("configure-plugin");
            }
            if (query.toLowerCase().contains("monitor")) {
                selectedTools.add("monitor-inference");
            }
            if (selectedTools.isEmpty()) {
                selectedTools.add("run-inference");
            }
            
            reasoningSteps.add(new AgentResponse.ReasoningStep(5, "Execute tools", "Invoke " + selectedTools.size() + " tools"));
            reasoningSteps.add(new AgentResponse.ReasoningStep(6, "Format results", "Prepare output"));
            
            String answer = String.format(
                "Data Processing Agent completed query: '%s'\n" +
                "Selected %d tools: %s\n" +
                "Execution context: userId=%s, sessionId=%s",
                query,
                selectedTools.size(),
                selectedTools,
                context.getUserId(),
                context.getSessionId()
            );
            
            return AgentResponse.builder()
                    .agentName(name)
                    .finalAnswer(answer)
                    .reasoningSteps(reasoningSteps)
                    .toolInvocations(List.of())
                    .metadata(Map.of("toolCount", selectedTools.size()))
                    .executionTimeMs(System.currentTimeMillis() - startTime)
                    .build();
            
        } catch (Exception e) {
            LOGGER.errorf(e, "Error in DataProcessingAgent: %s", e.getMessage());
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
        return "You are a data processing specialist. Analyze queries for data operations, " +
               "select appropriate tools, and format results clearly.";
    }
}
