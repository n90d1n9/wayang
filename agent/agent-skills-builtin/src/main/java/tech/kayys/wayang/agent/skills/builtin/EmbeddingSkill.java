package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.*;
import tech.kayys.wayang.agent.spi.EmbeddingRequest;
import tech.kayys.wayang.agent.spi.InferenceEngine;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Generates dense vector embeddings for one or more text inputs using the
 * Gollek inference engine's embedding endpoint.
 *
 * <p>
 * Embeddings can be used downstream by other skills (e.g. {@code rag},
 * {@code memory_store}) to perform semantic similarity search.
 * </p>
 *
 * <h2>Inputs</h2>
 * <table border="1">
 * <tr>
 * <th>Key</th>
 * <th>Required</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>text</td>
 * <td>yes*</td>
 * <td>Single text string to embed</td>
 * </tr>
 * <tr>
 * <td>texts</td>
 * <td>yes*</td>
 * <td>List of strings to batch-embed</td>
 * </tr>
 * <tr>
 * <td>model</td>
 * <td>no</td>
 * <td>Embedding model id (default: provider default)</td>
 * </tr>
 * </table>
 * <p>
 * *One of {@code text} or {@code texts} must be provided.
 * </p>
 *
 * <h2>Outputs</h2>
 * <ul>
 * <li>{@code embeddings} – list of {@code float[]} vectors (one per input)</li>
 * <li>{@code dimension} – vector dimensionality</li>
 * <li>{@code model} – model used</li>
 * </ul>
 */
@ApplicationScoped
@SkillDescriptor(id = "embedding", name = "Embedding", description = "Generates vector embeddings for text using the Gollek embedding endpoint.", version = "1.0.0", category = SkillCategory.GENERATION, inputs = {
        @SkillDescriptor.Input(name = "text", required = false, description = "Single text string to embed"),
        @SkillDescriptor.Input(name = "texts", type = "array", required = false, description = "List of strings to batch-embed"),
        @SkillDescriptor.Input(name = "model", required = false, description = "Embedding model ID")
}, outputs = {
        @SkillDescriptor.Output(name = "dimension", type = "integer", description = "Vector dimensionality"),
        @SkillDescriptor.Output(name = "count", type = "integer", description = "Number of vectors generated"),
        @SkillDescriptor.Output(name = "model", description = "Model used")
}, triggers = { "embed", "embedding", "vector", "encode" }, priority = 55)
public class EmbeddingSkill implements AgentSkill {

    private static final Logger LOG = Logger.getLogger(EmbeddingSkill.class);

    @Inject
    InferenceEngine inferenceEngine;

    @Override
    public String id() {
        return "embedding";
    }

    @Override
    public String name() {
        return "Embedding";
    }

    @Override
    public String description() {
        return "Generates vector embeddings using the Gollek engine.";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public SkillCategory category() {
        return SkillCategory.GENERATION;
    }

    @Override
    public boolean canHandle(Map<String, Object> inputs) {
        return inputs.containsKey("text") || inputs.containsKey("texts");
    }

    @Override
    @SuppressWarnings("unchecked")
    public Uni<SkillResult> execute(SkillContext ctx) {
        Instant start = Instant.now();

        List<String> inputs;
        String text = ctx.getStringInput("text", null);
        if (text != null) {
            inputs = List.of(text);
        } else {
            Object raw = ctx.inputs().get("texts");
            if (raw instanceof List) {
                inputs = (List<String>) raw;
            } else {
                return Uni.createFrom().item(SkillResult.builder()
                        .skillId(id())
                        .invocationId(ctx.invocationId())
                        .status(SkillResult.Status.FAILURE)
                        .observation("Input 'text' or 'texts' is required")
                        .build());
            }
        }

        String model = ctx.getStringInput("model", "default");

        EmbeddingRequest embReq = EmbeddingRequest.builder()
                .requestId(ctx.invocationId())
                .model(model)
                .inputs(inputs)
                .build();

        return inferenceEngine.executeEmbedding(model, embReq).map(resp -> {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            String observation = "Generated " + resp.embeddings().size() + " embedding(s) of dimension "
                    + resp.dimension();
            return SkillResult.builder()
                    .skillId(id())
                    .invocationId(ctx.invocationId())
                    .status(SkillResult.Status.SUCCESS)
                    .observation(observation)
                    .output("dimension", resp.dimension())
                    .output("model", model)
                    .output("count", resp.embeddings().size())
                    .durationMs(durationMs)
                    .build();
        }).onFailure().recoverWithItem(err -> SkillResult.builder()
                .skillId(id())
                .invocationId(ctx.invocationId())
                .status(SkillResult.Status.ERROR)
                .observation(err.getMessage())
                .error(err)
                .build());
    }
}
