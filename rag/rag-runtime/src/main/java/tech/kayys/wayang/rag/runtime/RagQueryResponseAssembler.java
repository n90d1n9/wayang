package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.RagResponse;
import tech.kayys.wayang.rag.core.RagResult;
import tech.kayys.wayang.rag.core.RagScoredChunk;
import tech.kayys.wayang.rag.core.SourceDocument;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

final class RagQueryResponseAssembler {

    private RagQueryResponseAssembler() {
    }

    static RagResponse toResponse(
            RagQueryWorkflowContext context,
            RagResult result,
            List<RagScoredChunk> chunks) {

        List<RagScoredChunk> safeChunks = RagScoredChunks.valid(chunks);
        List<SourceDocument> sourceDocuments = RagSourceDocuments.fromChunks(safeChunks);
        String answer = RagResponseContent.answer(result);

        return new RagResponse(
                context.query(),
                answer,
                sourceDocuments,
                List.of(),
                RagResponseMetrics.fromChunks(safeChunks, sourceDocuments.size()),
                RagResponseContent.context(result),
                Instant.now(),
                RagResponseMetadata.from(context),
                context.collections(),
                Optional.empty());
    }
}
