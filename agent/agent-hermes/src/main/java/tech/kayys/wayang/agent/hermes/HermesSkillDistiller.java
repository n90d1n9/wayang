package tech.kayys.wayang.agent.hermes;

import tech.kayys.wayang.agent.spi.skills.SkillDefinition;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministically turns a successful run into a portable procedural skill.
 */
public final class HermesSkillDistiller {

    private final HermesSkillPromptRenderer promptRenderer;
    private final HermesSkillIdentityFactory identityFactory;

    public HermesSkillDistiller() {
        this(new HermesSkillPromptRenderer(), new HermesSkillIdentityFactory());
    }

    public HermesSkillDistiller(HermesSkillPromptRenderer promptRenderer) {
        this(promptRenderer, new HermesSkillIdentityFactory());
    }

    public HermesSkillDistiller(
            HermesSkillPromptRenderer promptRenderer,
            HermesSkillIdentityFactory identityFactory) {
        this.promptRenderer = promptRenderer == null ? new HermesSkillPromptRenderer() : promptRenderer;
        this.identityFactory = identityFactory == null ? new HermesSkillIdentityFactory() : identityFactory;
    }

    public SkillDefinition distill(HermesLearningSignal signal, HermesAgentModeConfig config) {
        HermesSkillIdentity identity = identityFactory.fromTask(signal.task());
        List<String> tools = tools(signal);
        Map<String, Object> metadata = HermesSkillRevisionMetadata.initial(
                identity.id(),
                signal,
                metadata(signal, config));
        return SkillDefinition.builder()
                .id(identity.id())
                .name(identity.name())
                .description(identity.description())
                .category(HermesAgentMode.LEARNED_SKILL_CATEGORY)
                .systemPrompt(promptRenderer.initialPrompt(signal, config))
                .userPromptTemplate("{{instruction}}\n\nContext:\n{{context}}")
                .temperature(0.35)
                .maxTokens(2048)
                .defaultProvider(config.preferredProvider())
                .fallbackProvider(config.fallbackProvider())
                .tools(tools)
                .metadata(metadata)
                .build();
    }

    public SkillDefinition refine(SkillDefinition existing, SkillDefinition candidate, HermesLearningSignal signal) {
        return refine(existing, candidate, signal, "skill refinement");
    }

    public SkillDefinition refine(
            SkillDefinition existing,
            SkillDefinition candidate,
            HermesLearningSignal signal,
            String mergeReason) {
        Set<String> tools = new LinkedHashSet<>(existing.tools());
        tools.addAll(candidate.tools());
        Map<String, Object> metadata = HermesSkillRevisionMetadata.refined(
                existing,
                candidate,
                signal,
                mergeReason);

        return SkillDefinition.builder()
                .id(existing.id())
                .name(nonBlank(existing.name(), candidate.name()))
                .description(nonBlank(existing.description(), candidate.description()))
                .category(existing.category())
                .systemPrompt(existing.systemPrompt() + promptRenderer.refinementPrompt(signal))
                .subSkillPrompts(existing.subSkillPrompts())
                .userPromptTemplate(nonBlank(existing.userPromptTemplate(), candidate.userPromptTemplate()))
                .temperature(existing.temperature())
                .maxTokens(existing.maxTokens())
                .defaultProvider(nonBlank(existing.defaultProvider(), candidate.defaultProvider()))
                .fallbackProvider(nonBlank(existing.fallbackProvider(), candidate.fallbackProvider()))
                .tools(new ArrayList<>(tools))
                .orchestration(existing.orchestration())
                .metadata(metadata)
                .build();
    }

    private Map<String, Object> metadata(HermesLearningSignal signal, HermesAgentModeConfig config) {
        HermesLearningQualityProfile quality = HermesLearningQualityProfile.from(signal, config);
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("version", "1.0.0");
        metadata.put("tags", "hermes learned procedural agentskills");
        metadata.put("source", "hermes-agent-mode");
        metadata.put("agentskills.compatible", "true");
        metadata.put("hermes.task", HermesText.oneLine(signal.task()));
        metadata.put("hermes.learnedAt", DateTimeFormatter.ISO_INSTANT.format(signal.observedAt()));
        metadata.put("hermes.requestId", signal.requestId());
        metadata.put("hermes.revision", "1");
        metadata.put("hermes.stepCount", String.valueOf(signal.steps().size()));
        metadata.put("hermes.learningQualityScore", String.format(Locale.ROOT, "%.2f", quality.qualityScore()));
        metadata.put("hermes.learningQualityThreshold", String.format(Locale.ROOT, "%.2f", quality.threshold()));
        metadata.put("hermes.learningQualityStepScore", String.format(Locale.ROOT, "%.2f", quality.stepScore()));
        metadata.put("hermes.learningQualityToolScore", String.format(Locale.ROOT, "%.2f", quality.toolScore()));
        metadata.put("hermes.learningQualityTaskScore", String.format(
                Locale.ROOT,
                "%.2f",
                quality.taskSpecificityScore()));
        metadata.put("hermes.learningQualityAnswerScore", String.format(
                Locale.ROOT,
                "%.2f",
                quality.answerCompletenessScore()));
        metadata.put("hermes.learningQualityNoisePenalty", String.format(
                Locale.ROOT,
                "%.2f",
                quality.noisePenalty()));
        metadata.put("hermes.learningQualityReusePotential", String.format(
                Locale.ROOT,
                "%.2f",
                quality.reusePotentialScore()));
        return metadata;
    }

    private List<String> tools(HermesLearningSignal signal) {
        if (!signal.toolIds().isEmpty()) {
            return signal.toolIds();
        }
        Object tools = signal.metadata().get("tools");
        if (tools instanceof List<?> list) {
            return list.stream()
                    .filter(value -> value != null && !value.toString().isBlank())
                    .map(Object::toString)
                    .distinct()
                    .toList();
        }
        return List.of();
    }

    private static String nonBlank(String primary, String fallback) {
        return primary == null || primary.isBlank() ? fallback : primary;
    }
}
