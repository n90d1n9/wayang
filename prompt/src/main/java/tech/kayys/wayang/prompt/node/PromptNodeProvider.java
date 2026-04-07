package tech.kayys.wayang.prompt.node;

import tech.kayys.wayang.plugin.spi.node.NodeDefinition;
import tech.kayys.wayang.plugin.spi.node.NodeProvider;

import java.util.List;
import java.util.Map;

/**
 * Implementation of NodeProvider for prompt rendering nodes.
 */
public class PromptNodeProvider implements NodeProvider {

    @Override
    public String id() {
        return "tech.kayys.wayang.prompt";
    }

    @Override
    public String name() {
        return "Prompt Plugin";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Renders and manages prompt templates for AI workflows.";
    }

    @Override
    public List<NodeDefinition> nodes() {
        return List.of(
                new NodeDefinition(
                        PromptNodeTypes.PROMPT_RENDER,
                        "Render Prompt",
                        "AI",
                        "Transformation",
                        "Resolves, composes, and renders a chain of prompt templates using dynamic context.",
                        "text", // Icon
                        "#F59E0B", // Amber
                        PromptSchemas.PROMPT_RENDER_CONFIG,
                        "{}", // Input schema (managed by task)
                        "{}", // Output schema
                        Map.of(
                                "templateRefs", List.of())));
    }
}
