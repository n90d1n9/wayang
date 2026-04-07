package tech.kayys.wayang.rag;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.plugin.api.RagPluginTuningConfig;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class LexicalRerankPlugin implements RagPipelinePlugin {

    @Inject
    RagPluginTuningConfig tuningConfig;

    public LexicalRerankPlugin() {
    }

    public LexicalRerankPlugin(RagPluginTuningConfig tuningConfig) {
        this.tuningConfig = tuningConfig;
    }

    @Override
    public String id() {
        return "lexical-rerank";
    }

    @Override
    public int order() {
        return 300;
    }

    @Override
    public List<RagScoredChunk> afterRetrieve(RagPluginExecutionContext context, List<RagScoredChunk> chunks) {
        if (chunks == null || chunks.size() <= 1) {
            return chunks == null ? List.of() : chunks;
        }

        Set<String> queryTokens = tokenize(context.query());
        if (queryTokens.isEmpty()) {
            return chunks;
        }

        double originalWeight = sanitizeWeight(tuningConfig.lexicalRerankOriginalWeight(), 0.7);
        double lexicalWeight = sanitizeWeight(tuningConfig.lexicalRerankLexicalWeight(), 0.3);
        boolean annotateMetadata = tuningConfig.lexicalRerankAnnotateMetadata();

        List<Scored> rescored = new ArrayList<>(chunks.size());
        for (RagScoredChunk chunk : chunks) {
            double lexicalScore = lexicalSimilarity(queryTokens, tokenize(chunk.chunk().text()));
            double composite = (chunk.score() * originalWeight) + (lexicalScore * lexicalWeight);
            RagScoredChunk scoredChunk = chunk;
            if (annotateMetadata) {
                scoredChunk = new RagScoredChunk(annotate(chunk.chunk(), lexicalScore, composite), chunk.score());
            }
            rescored.add(new Scored(scoredChunk, composite));
        }

        rescored.sort((left, right) -> {
            int byComposite = Double.compare(right.compositeScore(), left.compositeScore());
            if (byComposite != 0) {
                return byComposite;
            }
            return Double.compare(right.chunk().score(), left.chunk().score());
        });

        return rescored.stream().map(Scored::chunk).collect(Collectors.toList());
    }

    private static double lexicalSimilarity(Set<String> queryTokens, Set<String> chunkTokens) {
        if (queryTokens.isEmpty() || chunkTokens.isEmpty()) {
            return 0.0;
        }
        long overlap = queryTokens.stream().filter(chunkTokens::contains).count();
        return overlap / (double) queryTokens.size();
    }

    private static RagChunk annotate(RagChunk chunk, double lexicalScore, double compositeScore) {
        Map<String, Object> metadata = new HashMap<>();
        if (chunk.metadata() != null) {
            metadata.putAll(chunk.metadata());
        }
        metadata.put("plugin.lexical_rerank.lexical_score", lexicalScore);
        metadata.put("plugin.lexical_rerank.composite_score", compositeScore);
        return new RagChunk(
                chunk.id(),
                chunk.documentId(),
                chunk.chunkIndex(),
                chunk.text(),
                Map.copyOf(metadata));
    }

    private static Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return java.util.Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toSet());
    }

    private static double sanitizeWeight(double value, double fallback) {
        if (!Double.isFinite(value) || value < 0.0) {
            return fallback;
        }
        return value;
    }

    private record Scored(RagScoredChunk chunk, double compositeScore) {
    }
}
