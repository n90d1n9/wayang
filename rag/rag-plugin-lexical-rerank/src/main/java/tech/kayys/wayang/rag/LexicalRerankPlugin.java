package tech.kayys.wayang.rag;

import tech.kayys.wayang.rag.plugin.api.RagPipelinePlugin;
import tech.kayys.wayang.rag.plugin.api.RagPluginTuningConfig;
import tech.kayys.wayang.rag.plugin.api.RagPluginExecutionContext;
import tech.kayys.wayang.rag.plugin.api.RagPluginSupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import tech.kayys.wayang.rag.core.RagChunk;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@ApplicationScoped
public class LexicalRerankPlugin implements RagPipelinePlugin {

    @Inject
    RagPluginTuningConfig tuningConfig = RagPluginTuningConfig.defaults();

    public LexicalRerankPlugin() {
    }

    public LexicalRerankPlugin(RagPluginTuningConfig tuningConfig) {
        this.tuningConfig = tuningConfig == null ? RagPluginTuningConfig.defaults() : tuningConfig;
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

        Set<String> queryTokens = RagPluginSupport.tokenizeLowercase(context.query());
        if (queryTokens.isEmpty()) {
            return chunks;
        }

        double originalWeight = RagPluginSupport.nonNegativeOr(tuningConfig.lexicalRerankOriginalWeight(), 0.7);
        double lexicalWeight = RagPluginSupport.nonNegativeOr(tuningConfig.lexicalRerankLexicalWeight(), 0.3);
        boolean annotateMetadata = tuningConfig.lexicalRerankAnnotateMetadata();

        List<Scored> rescored = new ArrayList<>(chunks.size());
        for (RagScoredChunk chunk : chunks) {
            double lexicalScore = lexicalSimilarity(
                    queryTokens,
                    RagPluginSupport.tokenizeLowercase(chunk.chunk().text()));
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
        return new RagChunk(
                chunk.id(),
                chunk.documentId(),
                chunk.chunkIndex(),
                chunk.text(),
                RagPluginSupport.metadataWith(chunk.metadata(), Map.of(
                        "plugin.lexical_rerank.lexical_score", lexicalScore,
                        "plugin.lexical_rerank.composite_score", compositeScore)));
    }

    private record Scored(RagScoredChunk chunk, double compositeScore) {
    }
}
