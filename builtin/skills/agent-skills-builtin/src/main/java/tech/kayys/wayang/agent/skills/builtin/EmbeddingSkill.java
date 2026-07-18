package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDescriptor;
import tech.kayys.wayang.embedding.EmbeddingRequest;
import tech.kayys.wayang.embedding.EmbeddingService;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
@SkillDescriptor(id = "embedding", name = "Embedding", description = "Generates vector embeddings for text using the active Wayang embedding service.", version = "1.0.0", category = SkillCategory.GENERATION, inputs = {
        @SkillDescriptor.Input(name = "text", required = false, description = "Single text string to embed"),
        @SkillDescriptor.Input(name = "texts", type = "array", required = false, description = "List of strings to batch-embed"),
        @SkillDescriptor.Input(name = "model", required = false, description = "Embedding model ID")
}, outputs = {
        @SkillDescriptor.Output(name = "dimension", type = "integer", description = "Vector dimensionality"),
        @SkillDescriptor.Output(name = "count", type = "integer", description = "Number of vectors generated"),
        @SkillDescriptor.Output(name = "model", description = "Model used")
}, triggers = { "embed", "embedding", "vector", "encode" }, priority = 55)
public class EmbeddingSkill implements AgentSkill {

    @Inject
    EmbeddingService embeddingService;

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
        return "Generates vector embeddings using the active Wayang embedding service.";
    }

    @Override
    public String category() {
        return SkillCategory.GENERATION.name();
    }

    @Override
    public boolean canHandle(Map<String, Object> inputs) {
        return inputs != null && (inputs.containsKey("text") || inputs.containsKey("texts"));
    }

    @Override
    public Uni<Map<String, Object>> execute(Map<String, Object> context) {
        Map<String, Object> inputs = context == null ? Map.of() : context;
        if (embeddingService == null) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("Embedding service is not configured"));
        }

        List<String> texts = texts(inputs);
        if (texts.isEmpty()) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("Input 'text' or 'texts' is required"));
        }

        String model = BuiltinSkillSupport.stringInput(inputs, "model");
        return embeddingService.embed(new EmbeddingRequest(texts, model, null, true))
                .map(response -> {
                    Map<String, Object> outputs = new LinkedHashMap<>();
                    outputs.put("dimension", response.dimension());
                    outputs.put("count", response.embeddings().size());
                    outputs.put("model", response.model());
                    outputs.put("provider", response.provider());
                    outputs.put("embeddings", response.embeddings().stream()
                            .map(BuiltinSkillSupport::boxedVector)
                            .toList());
                    return BuiltinSkillSupport.success(
                            "Generated " + response.embeddings().size()
                                    + " embedding(s) of dimension " + response.dimension(),
                            outputs);
                })
                .onFailure().recoverWithItem(BuiltinSkillSupport::error);
    }

    @SuppressWarnings("unchecked")
    private List<String> texts(Map<String, Object> inputs) {
        String text = BuiltinSkillSupport.stringInput(inputs, "text");
        if (text != null) {
            return List.of(text);
        }
        Object raw = inputs.get("texts");
        if (raw instanceof List<?> values) {
            return values.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
