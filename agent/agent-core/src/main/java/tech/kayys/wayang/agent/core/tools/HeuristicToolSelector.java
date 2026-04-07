package tech.kayys.wayang.agent.core.tools;

import org.jboss.logging.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple heuristic-based tool selector using keyword matching.
 * Used as fallback when LLM selection fails or as primary selector in resource-constrained environments.
 */
public class HeuristicToolSelector implements ToolSelector {

    private static final Logger LOGGER = Logger.getLogger(HeuristicToolSelector.class);

    private static final Map<String, List<String>> KEYWORD_TOOL_MAP = Map.ofEntries(
        Map.entry("search", List.of("search", "find", "look", "query", "fetch")),
        Map.entry("database", List.of("database", "query", "sql", "table", "record")),
        Map.entry("calculate", List.of("calculate", "compute", "math", "sum", "average")),
        Map.entry("send", List.of("send", "email", "message", "notify", "alert")),
        Map.entry("retrieve", List.of("get", "fetch", "retrieve", "load", "read")),
        Map.entry("update", List.of("update", "modify", "change", "set", "edit")),
        Map.entry("delete", List.of("delete", "remove", "destroy", "erase")),
        Map.entry("list", List.of("list", "all", "show", "display", "enumerate"))
    );

    @Override
    public List<ToolDefinition> selectTools(String query, List<ToolDefinition> availableTools) {
        if (availableTools.isEmpty()) {
            return List.of();
        }

        String lowerQuery = query.toLowerCase();
        Map<ToolDefinition, Double> scores = new HashMap<>();

        for (ToolDefinition tool : availableTools) {
            double score = calculateToolScore(tool, lowerQuery);
            if (score > 0) {
                scores.put(tool, score);
            }
        }

        if (scores.isEmpty()) {
            LOGGER.debugf("No tools selected by heuristic for query: %s, using first available", query);
            return List.of(availableTools.get(0));
        }

        // Return tools sorted by score (highest first)
        List<ToolDefinition> selected = scores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        LOGGER.debugf("Heuristic selected %d tools for query: %s", selected.size(), query);
        return selected;
    }

    /**
     * Calculate relevance score for a tool given the query.
     */
    private double calculateToolScore(ToolDefinition tool, String query) {
        double score = 0;

        String toolName = tool.name().toLowerCase();
        String toolDesc = tool.description().toLowerCase();

        // Exact name match
        if (query.contains(toolName)) {
            score += 10;
        }

        // Name substring match
        if (toolName.length() >= 3 && query.contains(toolName.substring(0, Math.min(3, toolName.length())))) {
            score += 5;
        }

        // Description keywords
        score += scoreKeywordMatches(toolDesc, query);

        // Parameter relevance
        if (!tool.parameters().isEmpty()) {
            String paramsStr = String.join(" ", tool.parameters().keySet()).toLowerCase();
            score += scoreKeywordMatches(paramsStr, query) * 0.5;
        }

        return score;
    }

    /**
     * Score keyword matches between description and query.
     */
    private double scoreKeywordMatches(String description, String query) {
        double score = 0;

        for (Map.Entry<String, List<String>> entry : KEYWORD_TOOL_MAP.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (description.contains(keyword) && query.contains(keyword)) {
                    score += 2;
                }
            }
        }

        return score;
    }
}
