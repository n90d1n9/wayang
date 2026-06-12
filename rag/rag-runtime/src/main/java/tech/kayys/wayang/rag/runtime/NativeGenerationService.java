package tech.kayys.wayang.rag.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagQuery;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;

/**
 * Service for generating natural language responses based on retrieved context.
 * Supports multiple generation modes including extractive (returning specific
 * sentences)
 * and contextual (synthesizing an answer from provided chunks).
 */
@ApplicationScoped
public class NativeGenerationService {

    public String generate(RagQuery query, List<RagScoredChunk> context, GenerationConfig generationConfig) {
        NativeGenerationMode mode = NativeGenerationMode.from(generationConfig);
        List<RagScoredChunk> safeContext = RagScoredChunks.valid(context);
        return switch (mode) {
            case EXTRACTIVE -> generateExtractive(query, safeContext, generationConfig);
            case CONTEXT -> generateContext(query, safeContext, generationConfig);
        };
    }

    private String generateContext(RagQuery query, List<RagScoredChunk> context, GenerationConfig generationConfig) {
        StringBuilder sb = new StringBuilder();
        if (generationConfig != null && generationConfig.systemPrompt() != null
                && !generationConfig.systemPrompt().isBlank()) {
            sb.append(generationConfig.systemPrompt()).append("\n\n");
        }
        sb.append("Q: ").append(queryText(query)).append("\n");
        sb.append("Context:\n");
        for (RagScoredChunk scored : context) {
            sb.append("- ").append(scored.chunk().text()).append("\n");
        }
        return sb.toString().trim();
    }

    private String generateExtractive(RagQuery query, List<RagScoredChunk> context, GenerationConfig generationConfig) {
        if (context.isEmpty()) {
            return "No relevant context found for: " + queryText(query);
        }
        RagScoredChunk top = context.getFirst();
        String text = top.chunk().text() == null ? "" : top.chunk().text();
        int cut = text.indexOf('.');
        String answer = cut > 0 ? text.substring(0, cut + 1) : text;
        if (generationConfig != null && generationConfig.enableCitations()) {
            return answer + " [source: " + top.chunk().id() + "]";
        }
        return answer;
    }

    private static String queryText(RagQuery query) {
        return query == null || query.text() == null ? "" : query.text();
    }
}
