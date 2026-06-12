package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;

import java.util.List;

final class RagScoredChunks {

    private RagScoredChunks() {
    }

    static List<RagScoredChunk> fromResult(RagResult result) {
        return result == null ? List.of() : valid(result.chunks());
    }

    static List<RagScoredChunk> valid(List<RagScoredChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return List.of();
        }
        return chunks.stream()
                .filter(scored -> scored != null && scored.chunk() != null)
                .toList();
    }

    static float averageScore(List<RagScoredChunk> chunks) {
        List<RagScoredChunk> validChunks = valid(chunks);
        if (validChunks.isEmpty()) {
            return 0f;
        }
        double sum = validChunks.stream().mapToDouble(RagScoredChunk::score).sum();
        return (float) (sum / validChunks.size());
    }
}
