package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
@SkillDescriptor(id = "inference", name = "LLM Inference", description = "Executes a prompt through the configured inference backend and returns the generated response.", version = "1.0.0", category = SkillCategory.INFERENCE, inputs = {
        @SkillDescriptor.Input(name = "prompt", description = "The prompt to send to the model"),
        @SkillDescriptor.Input(name = "model", required = false, description = "Optional model override"),
        @SkillDescriptor.Input(name = "systemPrompt", required = false, description = "System context"),
        @SkillDescriptor.Input(name = "maxTokens", type = "integer", required = false, description = "Max tokens to generate"),
        @SkillDescriptor.Input(name = "temperature", type = "number", required = false, description = "Sampling temperature")
}, outputs = {
        @SkillDescriptor.Output(name = "response", description = "The generated text response"),
        @SkillDescriptor.Output(name = "tokensUsed", type = "integer"),
        @SkillDescriptor.Output(name = "model")
}, triggers = { "infer", "generate text", "ask llm", "prompt", "complete" }, aliases = { "llm", "generate", "complete" }, priority = 10)
public class InferenceSkill implements AgentSkill {

    private static final Logger LOG = Logger.getLogger(InferenceSkill.class);

    @Inject
    InferenceBackend inferenceBackend;

    @Override
    public String id() {
        return "inference";
    }

    @Override
    public String name() {
        return "LLM Inference";
    }

    @Override
    public String description() {
        return "Executes a prompt through the configured inference backend.";
    }

    @Override
    public String category() {
        return SkillCategory.INFERENCE.name();
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean canHandle(Map<String, Object> inputs) {
        return inputs != null && inputs.containsKey("prompt");
    }

    @Override
    public Uni<Map<String, Object>> execute(Map<String, Object> context) {
        Map<String, Object> inputs = context == null ? Map.of() : context;
        String prompt = BuiltinSkillSupport.stringInput(inputs, "prompt");
        if (prompt == null || prompt.isBlank()) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("Input 'prompt' is required"));
        }
        if (inferenceBackend == null) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("Inference backend is not configured"));
        }

        String model = BuiltinSkillSupport.stringInput(inputs, "model");
        String systemPrompt = BuiltinSkillSupport.stringInput(inputs, "systemPrompt", "You are a helpful assistant.");
        int maxTokens = BuiltinSkillSupport.intInput(inputs, "maxTokens", 512);
        double temperature = BuiltinSkillSupport.doubleInput(inputs, "temperature", 0.7);
        String tenantId = BuiltinSkillSupport.stringInput(inputs, "tenantId");
        long start = System.currentTimeMillis();

        return inferenceBackend.infer(BuiltinSkillSupport.textRequest(
                        "skill-inference-" + java.util.UUID.randomUUID(),
                        model,
                        systemPrompt,
                        prompt,
                        maxTokens,
                        temperature,
                        tenantId))
                .map(response -> {
                    String content = BuiltinSkillSupport.responseContent(response);
                    Map<String, Object> outputs = new LinkedHashMap<>();
                    outputs.put("response", content);
                    outputs.put("tokensUsed", BuiltinSkillSupport.totalTokens(response));
                    outputs.put("durationMs", System.currentTimeMillis() - start);
                    outputs.put("model", BuiltinSkillSupport.responseModel(response, model));
                    return BuiltinSkillSupport.success(content, outputs);
                })
                .onFailure().recoverWithItem(error -> {
                    LOG.errorf(error, "Inference skill failed");
                    return BuiltinSkillSupport.error(error);
                });
    }
}
