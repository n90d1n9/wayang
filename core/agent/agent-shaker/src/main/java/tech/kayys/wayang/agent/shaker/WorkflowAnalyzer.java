package tech.kayys.wayang.agent.shaker;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes workflow definitions to identify required components for tree-shaking.
 *
 * <p>
 * This analyzer reads workflow YAML definitions and determines:
 * <ul>
 *   <li>Which node types are used (prompt, tool, RAG, vector, guardrails, etc.)</li>
 *   <li>Which orchestrators are needed (ReAct, Plan/Execute, Reflexion, etc.)</li>
 *   <li>Which tools/skills are referenced</li>
 *   <li>Which memory tiers are required</li>
 *   <li>Which backends are configured (gollek, gamelan, etc.)</li>
 * </ul>
 *
 * <p>
 * The analysis results drive the tree-shaking process to generate minimal
 * agent JARs containing only the components actually used by the workflow.
 * </p>
 *
 * <h3>Usage:</h3>
 * <pre>{@code
 * WorkflowAnalyzer analyzer = new WorkflowAnalyzer();
 * DependencyReport report = analyzer.analyze(Path.of("workflow.yaml"));
 *
 * System.out.println("Required executors: " + report.requiredExecutors());
 * System.out.println("Required orchestrators: " + report.requiredOrchestrators());
 * System.out.println("Required tools: " + report.requiredTools());
 * }</pre>
 *
 * @author Wayang Team
 * @version 0.1.0
 * @since 2026-04-06
 */
public class WorkflowAnalyzer {

    private static final Yaml YAML = new Yaml();

    /**
     * Analyze a workflow definition file.
     *
     * @param workflowPath path to workflow YAML file
     * @return dependency report with required components
     * @throws IOException if file cannot be read
     */
    public DependencyReport analyze(Path workflowPath) throws IOException {
        String content = Files.readString(workflowPath);
        return analyze(content);
    }

    /**
     * Analyze workflow definition content.
     *
     * @param workflowContent workflow YAML content
     * @return dependency report with required components
     */
    @SuppressWarnings("unchecked")
    public DependencyReport analyze(String workflowContent) {
        Map<String, Object> workflow = YAML.load(workflowContent);

        Set<String> requiredExecutors = new LinkedHashSet<>();
        Set<String> requiredOrchestrators = new LinkedHashSet<>();
        Set<String> requiredTools = new LinkedHashSet<>();
        Set<String> requiredSkills = new LinkedHashSet<>();
        Set<String> requiredMemoryTiers = new LinkedHashSet<>();
        Set<String> requiredBackends = new LinkedHashSet<>();
        Set<String> requiredGuardrails = new LinkedHashSet<>();
        Set<String> requiredVectorStores = new LinkedHashSet<>();

        // Analyze workflow structure
        if (workflow.containsKey("metadata")) {
            Map<String, Object> metadata = (Map<String, Object>) workflow.get("metadata");
            if (metadata.containsKey("backend")) {
                requiredBackends.add(metadata.get("backend").toString());
            }
            if (metadata.containsKey("orchestrator")) {
                requiredOrchestrators.add(metadata.get("orchestrator").toString());
            }
        }

        // Analyze nodes
        if (workflow.containsKey("nodes")) {
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) workflow.get("nodes");
            for (Map<String, Object> node : nodes) {
                String nodeType = (String) node.get("type");
                if (nodeType != null) {
                    requiredExecutors.add(normalizeNodeType(nodeType));
                }

                // Extract tools/skills referenced in node
                if (node.containsKey("tools")) {
                    List<String> tools = (List<String>) node.get("tools");
                    requiredTools.addAll(tools);
                }

                if (node.containsKey("skills")) {
                    List<String> skills = (List<String>) node.get("skills");
                    requiredSkills.addAll(skills);
                }

                // Extract memory requirements
                if (node.containsKey("memory")) {
                    Map<String, Object> memory = (Map<String, Object>) node.get("memory");
                    if (memory.containsKey("tiers")) {
                        List<String> tiers = (List<String>) memory.get("tiers");
                        requiredMemoryTiers.addAll(tiers);
                    }
                }

                // Extract guardrails
                if (node.containsKey("guardrails")) {
                    List<String> guardrails = (List<String>) node.get("guardrails");
                    requiredGuardrails.addAll(guardrails);
                }

                // Extract vector store configuration
                if (node.containsKey("vector")) {
                    Map<String, Object> vector = (Map<String, Object>) node.get("vector");
                    if (vector.containsKey("store")) {
                        requiredVectorStores.add(vector.get("store").toString());
                    }
                }
            }
        }

        // Default orchestrator if not specified
        if (requiredOrchestrators.isEmpty()) {
            requiredOrchestrators.add("react");  // Default
        }

        // Default backend if not specified
        if (requiredBackends.isEmpty()) {
            requiredBackends.add("gollek");
            requiredBackends.add("gamelan");
        }

        return new DependencyReport(
            requiredExecutors,
            requiredOrchestrators,
            requiredTools,
            requiredSkills,
            requiredMemoryTiers,
            requiredBackends,
            requiredGuardrails,
            requiredVectorStores
        );
    }

    /**
     * Normalize node type to executor name.
     */
    private String normalizeNodeType(String nodeType) {
        return switch (nodeType.toLowerCase()) {
            case "prompt" -> "prompt-executor";
            case "tool", "tool_call" -> "tool-executor";
            case "rag", "retrieval" -> "rag-executor";
            case "vector_search", "vector-search" -> "vector-search-executor";
            case "vector_upsert", "vector-upsert" -> "vector-upsert-executor";
            case "guardrails", "guardrail" -> "guardrails-executor";
            case "hitl", "human_task", "human-task" -> "hitl-executor";
            case "memory", "memory_store" -> "memory-executor";
            case "agent", "agent_call" -> "agent-executor";
            case "llm", "inference" -> "inference-executor";
            case "code", "code_execution" -> "code-executor";
            default -> nodeType + "-executor";
        };
    }

    /**
     * Scan a directory for workflow definitions and analyze all of them.
     *
     * @param workflowsDir directory containing workflow YAML files
     * @return aggregated dependency report
     * @throws IOException if directory cannot be read
     */
    public DependencyReport analyzeAll(Path workflowsDir) throws IOException {
        DependencyReport aggregated = DependencyReport.empty();

        try (var stream = Files.walk(workflowsDir)) {
            List<Path> workflowFiles = stream
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                .collect(Collectors.toList());

            for (Path workflowFile : workflowFiles) {
                DependencyReport report = analyze(workflowFile);
                aggregated = aggregated.merge(report);
            }
        }

        return aggregated;
    }
}
