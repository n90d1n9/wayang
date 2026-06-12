package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.*;
import tech.kayys.gollek.engine.inference.InferenceService;
import tech.kayys.wayang.agent.spi.InferenceRequest;
import tech.kayys.wayang.agent.spi.Message;

/**
 * Built-in skill: LLM Inference.
 *
 * <p>
 * Executes an LLM prompt through the Gollek inference engine,
 * enabling agents to delegate sub-questions to different models.
 *
 * <p>
 * Input schema:
 * <ul>
 * <li>{@code prompt} (string, required) — the prompt to send to the LLM</li>
 * <li>{@code model} (string, optional) — override the default model</li>
 * <li>{@code systemPrompt} (string, optional) — system context</li>
 * <li>{@code maxTokens} (integer, optional, default 512)</li>
 * <li>{@code temperature} (number, optional, default 0.7)</li>
 * </ul>
 *
 * <p>
 * Output schema:
 * <ul>
 * <li>{@code response} (string) — LLM-generated text</li>
 * <li>{@code tokensUsed} (integer)</li>
 * <li>{@code durationMs} (long)</li>
 * <li>{@code model} (string)</li>
 * </ul>
 *
 * @author Bhangun
 */
@ApplicationScoped
@SkillDescriptor(id = "inference", name = "LLM Inference", description = "Execute a prompt through a configured LLM provider and return the generated response.", version = "1.0.0", category = SkillCategory.INFERENCE, inputs = {
        @SkillDescriptor.Input(name = "prompt", description = "The prompt to send to the LLM"),
        @SkillDescriptor.Input(name = "model", required = false, description = "Override the default model"),
        @SkillDescriptor.Input(name = "systemPrompt", required = false, description = "System context for the LLM"),
        @SkillDescriptor.Input(name = "maxTokens", type = "integer", required = false, description = "Max tokens to generate"),
        @SkillDescriptor.Input(name = "temperature", type = "number", required = false, description = "Sampling temperature")
}, outputs = {
        @SkillDescriptor.Output(name = "response", description = "The generated text response"),
        @SkillDescriptor.Output(name = "tokensUsed", type = "integer"),
        @SkillDescriptor.Output(name = "durationMs", type = "long")
}, triggers = { "infer", "generate text", "ask llm", "prompt",
        "complete" }, aliases = { "llm", "generate", "complete" })
public class InferenceSkill implements AgentSkill {

    private static final Logger LOG = Logger.getLogger(InferenceSkill.class);

    @Inject
    InferenceService inferenceService;

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
        return "Execute a prompt through a configured LLM provider and return the generated response.";
    }

    @Override
    public SkillCategory category() {
        return SkillCategory.INFERENCE;
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public Uni<SkillResult> execute(SkillContext ctx) {
        ctx.requireInput("prompt");
        long start = System.currentTimeMillis();

        String prompt = ctx.getStringInput("prompt");
        String model = ctx.getStringInput("model");
        String sysPrompt = ctx.getStringInput("systemPrompt", "You are a helpful assistant.");
        int maxTokens = ctx.getIntInput("maxTokens", 512);
        double temperature = ((Number) ctx.getInputs().getOrDefault("temperature", 0.7)).doubleValue();

        LOG.debugf("InferenceSkill: prompt length=%d, model=%s, tenant=%s",
                prompt.length(), model, ctx.getTenantId());

        InferenceRequest.Builder reqBuilder = InferenceRequest.builder()
                .requestId("skill-inference-" + ctx.getInvocationId())
                .message(Message.system(sysPrompt))
                .message(Message.user(prompt))
                .parameter("max_tokens", maxTokens)
                .parameter("temperature", temperature)
                .metadata("tenantId", ctx.getTenantId())
                .metadata("skillInvocation", ctx.getInvocationId());

        if (model != null && !model.isBlank())
            reqBuilder.model(model);

        return inferenceService.inferAsync(reqBuilder.build())
                .map(resp -> {
                    long duration = System.currentTimeMillis() - start;
                    return SkillResult.builder()
                            .skillId(id())
                            .invocationId(ctx.getInvocationId())
                            .status(SkillResult.Status.SUCCESS)
                            .observation(resp.getContent())
                            .output("response", resp.getContent())
                            .output("tokensUsed", resp.getTokensUsed())
                            .output("durationMs", duration)
                            .output("model", resp.getModel())
                            .durationMs(duration)
                            .build();
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.errorf(err, "InferenceSkill failed for invocation=%s", ctx.getInvocationId());
                    return SkillResult.failure(id(), err);
                });
    }

    @Override
    public SkillValidation validate(java.util.Map<String, Object> inputs) {
        if (!inputs.containsKey("prompt") || inputs.get("prompt") == null) {
            return SkillValidation.error("Missing required input: prompt");
        }
        Object prompt = inputs.get("prompt");
        if (prompt.toString().isBlank()) {
            return SkillValidation.error("Input 'prompt' must not be blank");
        }
        return SkillValidation.success();
    }
}
