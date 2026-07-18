package tech.kayys.wayang.agent.spi.tools;

import java.util.List;
import java.util.Map;

/**
 * Selects appropriate tools for a given query.
 * Implementations can use heuristics, LLM reasoning, or hybrid approaches.
 */
public interface ToolSelector {

    /**
     * Select tools that are appropriate for answering the given query.
     * 
     * @param query The user query
     * @param availableTools All tools that can be used
     * @return Selected tools in order of preference
     */
    List<ToolDefinition> selectTools(String query, List<ToolDefinition> availableTools);

    /**
     * Tool definition with metadata needed for selection and invocation.
     */
    record ToolDefinition(
        String name,
        String description,
        Map<String, ParameterSchema> parameters,
        boolean required
    ) {}

    /**
     * Schema for a tool parameter.
     */
    record ParameterSchema(
        String type, // string, number, boolean, array, object
        String description,
        boolean required,
        Object defaultValue
    ) {}
}
