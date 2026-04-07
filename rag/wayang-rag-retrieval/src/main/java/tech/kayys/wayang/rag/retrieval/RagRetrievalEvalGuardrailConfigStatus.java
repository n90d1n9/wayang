package tech.kayys.wayang.rag.retrieval;

import java.time.Instant;

public record RagRetrievalEvalGuardrailConfigStatus(
        RagRetrievalEvalGuardrailConfig config,
        Instant updatedAt) {
}
