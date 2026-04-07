package tech.kayys.wayang.agent.skills.builtin;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import tech.kayys.wayang.agent.spi.*;
import tech.kayys.gollek.engine.inference.InferenceService;
import tech.kayys.wayang.agent.spi.Message;
import tech.kayys.wayang.agent.spi.InferenceRequest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Summarises long text using the Gollek inference engine.
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
 * <td>Text to summarise</td>
 * </tr>
 * <tr>
 * <td>prompt</td>
 * <td>yes*</td>
 * <td>Alias for {@code text}</td>
 * </tr>
 * <tr>
 * <td>style</td>
 * <td>no</td>
 * <td>bullet | paragraph | executive (default: paragraph)</td>
 * </tr>
 * <tr>
 * <td>max_words</td>
 * <td>no</td>
 * <td>Target summary length in words (default: 150)</td>
 * </tr>
 * <tr>
 * <td>model</td>
 * <td>no</td>
 * <td>Override the default inference model</td>
 * </tr>
 * <tr>
 * <td>focus</td>
 * <td>no</td>
 * <td>Topic/aspect to emphasise in the summary</td>
 * </tr>
 * </table>
 *
 * <p>
 * *At least one of {@code text} or {@code prompt} must be supplied.
 * </p>
 *
 * <h2>Outputs</h2>
 * <ul>
 * <li>{@code summary} – the generated summary text</li>
 * <li>{@code word_count} – approximate word count of the summary</li>
 * <li>{@code model} – model used</li>
 * </ul>
 */
@ApplicationScoped
@SkillDescriptor(id = "summarization", name = "Summarization", description = "Condenses long text into concise summaries using the Gollek inference engine.", version = "1.0.0", category = SkillCategory.REASONING, inputs = {
                @SkillDescriptor.Input(name = "text", description = "Text to summarise"),
                @SkillDescriptor.Input(name = "prompt", required = false, description = "Alias for text"),
                @SkillDescriptor.Input(name = "style", required = false, description = "bullet | paragraph | executive"),
                @SkillDescriptor.Input(name = "max_words", type = "integer", required = false, description = "Target length in words"),
                @SkillDescriptor.Input(name = "focus", required = false, description = "Topic to emphasize")
}, outputs = {
                @SkillDescriptor.Output(name = "summary", description = "The condensed text"),
                @SkillDescriptor.Output(name = "word_count", type = "integer", description = "Summary word count")
}, triggers = { "summarize", "summarise", "tldr", "condense", "brief",
                "shorten" }, aliases = { "summarize", "tldr", "condense" }, priority = 70)
public class SummarizationSkill implements AgentSkill {

        private static final Logger LOG = Logger.getLogger(SummarizationSkill.class);

        @Inject
        InferenceService inferenceService;

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
                return "Summarises text using the Gollek inference engine.";
        }

        @Override
        public String version() {
                return "1.0.0";
        }

        @Override
        public SkillCategory category() {
                return SkillCategory.REASONING;
        }

        @Override
        public boolean canHandle(Map<String, Object> inputs) {
                return inputs.containsKey("text") || inputs.containsKey("prompt");
        }

        @Override
        public Uni<SkillResult> execute(SkillContext ctx) {
                Instant start = Instant.now();
                String text = ctx.getStringInput("text",
                                ctx.getStringInput("prompt", null));
                if (text == null || text.isBlank()) {
                        return Uni.createFrom().item(SkillResult.builder()
                                        .skillId(id())
                                        .invocationId(ctx.invocationId())
                                        .status(SkillResult.Status.FAILURE)
                                        .observation("Input 'text' or 'prompt' is required")
                                        .build());
                }

                String style = ctx.getStringInput("style", "paragraph");
                int maxWords = ctx.getIntInput("max_words", 150);
                String model = ctx.getStringInput("model", "default");
                String focus = ctx.getStringInput("focus", null);

                String systemPrompt = buildSystemPrompt(style, maxWords, focus);

                InferenceRequest ir = InferenceRequest.builder()
                                .requestId(ctx.invocationId())
                                .model(model)
                                .message(Message.system(systemPrompt))
                                .message(Message.user(text))
                                .parameter("max_tokens", Math.min(maxWords * 5, 1024))
                                .parameter("temperature", 0.3)
                                .metadata("tenantId", ctx.tenantId())
                                .build();

                return inferenceService.inferAsync(ir).map(resp -> {
                        String summary = resp.getContent();
                        int words = summary.split("\\s+").length;
                        long durationMs = Duration.between(start, Instant.now()).toMillis();

                        return SkillResult.builder()
                                        .skillId(id())
                                        .invocationId(ctx.invocationId())
                                        .status(SkillResult.Status.SUCCESS)
                                        .observation(summary)
                                        .output("summary", summary)
                                        .output("word_count", words)
                                        .output("model", resp.getModel() != null ? resp.getModel() : model)
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

        private String buildSystemPrompt(String style, int maxWords, String focus) {
                String baseInstruction = switch (style.toLowerCase()) {
                        case "bullet" ->
                                "Summarise the text as a concise bullet-point list. Start each point with '- '.";
                        case "executive" ->
                                "Produce an executive summary: one sentence of the key takeaway, followed by 2-3 key points.";
                        default -> "Summarise the following text in a clear, concise paragraph.";
                };
                String wordLimit = " Target approximately " + maxWords + " words.";
                String focusHint = (focus != null && !focus.isBlank())
                                ? " Emphasise aspects related to: " + focus + "."
                                : "";
                return baseInstruction + wordLimit + focusHint + " Do not include preamble like 'Here is a summary:'";
        }
}
