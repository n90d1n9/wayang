package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import tech.kayys.wayang.agent.spi.AgentSkill;
import tech.kayys.wayang.agent.spi.InferenceBackend;
import tech.kayys.wayang.agent.spi.skills.SkillCategory;
import tech.kayys.wayang.agent.spi.skills.SkillDescriptor;

import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
@SkillDescriptor(id = "summarization", name = "Summarization", description = "Condenses long text into concise summaries using the configured inference backend.", version = "1.0.0", category = SkillCategory.REASONING, inputs = {
        @SkillDescriptor.Input(name = "text", description = "Text to summarize"),
        @SkillDescriptor.Input(name = "prompt", required = false, description = "Alias for text"),
        @SkillDescriptor.Input(name = "style", required = false, description = "bullet | paragraph | executive"),
        @SkillDescriptor.Input(name = "max_words", type = "integer", required = false, description = "Target length in words"),
        @SkillDescriptor.Input(name = "focus", required = false, description = "Topic to emphasize")
}, outputs = {
        @SkillDescriptor.Output(name = "summary", description = "The condensed text"),
        @SkillDescriptor.Output(name = "word_count", type = "integer", description = "Summary word count")
}, triggers = { "summarize", "summarise", "tldr", "condense", "brief", "shorten" }, aliases = { "summarize", "tldr", "condense" }, priority = 70)
public class SummarizationSkill implements AgentSkill {

    @Inject
    InferenceBackend inferenceBackend;

    @Override
    public String id() {
        return "summarization";
    }

    @Override
    public String name() {
        return "Summarization";
    }

    @Override
    public String description() {
        return "Summarizes text using the configured inference backend.";
    }

    @Override
    public String category() {
        return SkillCategory.REASONING.name();
    }

    @Override
    public boolean canHandle(Map<String, Object> inputs) {
        return inputs != null && (inputs.containsKey("text") || inputs.containsKey("prompt"));
    }

    @Override
    public Uni<Map<String, Object>> execute(Map<String, Object> context) {
        Map<String, Object> inputs = context == null ? Map.of() : context;
        String text = BuiltinSkillSupport.stringInput(inputs, "text",
                BuiltinSkillSupport.stringInput(inputs, "prompt"));
        if (text == null || text.isBlank()) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("Input 'text' or 'prompt' is required"));
        }
        if (inferenceBackend == null) {
            return Uni.createFrom().item(BuiltinSkillSupport.failure("Inference backend is not configured"));
        }

        String style = BuiltinSkillSupport.stringInput(inputs, "style", "paragraph");
        int maxWords = BuiltinSkillSupport.intInput(inputs, "max_words", 150);
        String focus = BuiltinSkillSupport.stringInput(inputs, "focus");
        String model = BuiltinSkillSupport.stringInput(inputs, "model");
        long start = System.currentTimeMillis();

        return inferenceBackend.infer(BuiltinSkillSupport.textRequest(
                        "skill-summary-" + java.util.UUID.randomUUID(),
                        model,
                        buildSystemPrompt(style, maxWords, focus),
                        text,
                        Math.min(maxWords * 5, 1024),
                        0.3,
                        BuiltinSkillSupport.stringInput(inputs, "tenantId")))
                .map(response -> {
                    String summary = BuiltinSkillSupport.responseContent(response);
                    int words = summary.isBlank() ? 0 : summary.split("\\s+").length;
                    Map<String, Object> outputs = new LinkedHashMap<>();
                    outputs.put("summary", summary);
                    outputs.put("word_count", words);
                    outputs.put("model", BuiltinSkillSupport.responseModel(response, model));
                    outputs.put("durationMs", System.currentTimeMillis() - start);
                    return BuiltinSkillSupport.success(summary, outputs);
                })
                .onFailure().recoverWithItem(BuiltinSkillSupport::error);
    }

    private String buildSystemPrompt(String style, int maxWords, String focus) {
        String baseInstruction = switch (style.toLowerCase(java.util.Locale.ROOT)) {
            case "bullet" -> "Summarize the text as a concise bullet-point list. Start each point with '- '.";
            case "executive" -> "Produce an executive summary with one key takeaway and 2-3 key points.";
            default -> "Summarize the following text in a clear, concise paragraph.";
        };
        String focusHint = focus != null && !focus.isBlank()
                ? " Emphasize aspects related to: " + focus + "."
                : "";
        return baseInstruction + " Target approximately " + maxWords + " words." + focusHint
                + " Do not include preamble like 'Here is a summary:'.";
    }
}
