package tech.kayys.wayang.agent.spi.skills.rag;

import io.smallrye.mutiny.Uni;

/**
 * Adapter boundary for retrieving context used by the built-in RAG skill.
 */
public interface RagSkillRetriever {

    Uni<RagSkillRetrievalResult> retrieve(RagSkillRetrievalRequest request);
}
