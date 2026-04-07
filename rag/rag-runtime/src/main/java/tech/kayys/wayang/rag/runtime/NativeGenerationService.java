package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;
import java.util.Locale;

/**
 * Service for generating natural language responses based on retrieved context.
 * Supports multiple generation modes including extractive (returning specific
 * sentences)
 * and contextual (synthesizing an answer from provided chunks).
 */
@ApplicationScoped
public class NativeGenerationService {

    public String generate(RagQuery query, List<RagScoredChunk> context, GenerationConfig generationConfig) {
        String mode = selectMode(generationConfig);
        return switch (mode) {
            case "extractive" -> generateExtractive(query, context, generationConfig);
            case "context", "concat" -> generateContext(query, context, generationConfig);
            default -> generateContext(query, context, generationConfig);
        };
    }

    private String selectMode(GenerationConfig generationConfig) {
        if (generationConfig == null || generationConfig.provider() == null) {
            return "context";
        }
        String provider = generationConfig.provider().trim().toLowerCase(Locale.ROOT);
        if (provider.contains("extract")) {
            return "extractive";
        }
        return "context";
    }

    private String generateContext(RagQuery query, List<RagScoredChunk> context, GenerationConfig generationConfig) {
        StringBuilder sb = new StringBuilder();
        if (generationConfig != null && generationConfig.systemPrompt() != null
                && !generationConfig.systemPrompt().isBlank()) {
            sb.append(generationConfig.systemPrompt()).append("\n\n");
        }
        sb.append("Q: ").append(query.text()).append("\n");
        sb.append("Context:\n");
        for (RagScoredChunk scored : context) {
            sb.append("- ").append(scored.chunk().text()).append("\n");
        }
        return sb.toString().trim();
    }

    private String generateExtractive(RagQuery query, List<RagScoredChunk> context, GenerationConfig generationConfig) {
        if (context == null || context.isEmpty()) {
            return "No relevant context found for: " + query.text();
        }
        RagScoredChunk top = context.getFirst();
        String text = top.chunk().text();
        int cut = text.indexOf('.');
        String answer = cut > 0 ? text.substring(0, cut + 1) : text;
        if (generationConfig != null && generationConfig.enableCitations()) {
            return answer + " [source: " + top.chunk().id() + "]";
        }
        return answer;
    }
}
