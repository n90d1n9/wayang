package tech.kayys.wayang.agent.spi.skills.rag;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Retrieved documents returned by a RAG skill retriever adapter.
 */
public record RagSkillRetrievalResult(List<RagSkillRetrievedDocument> documents) {

    private static final RagSkillRetrievalResult EMPTY = new RagSkillRetrievalResult(List.of());

    public RagSkillRetrievalResult {
        documents = documents == null
                ? List.of()
                : documents.stream()
                        .filter(document -> document != null && !document.content().isBlank())
                        .toList();
    }

    public static RagSkillRetrievalResult empty() {
        return EMPTY;
    }

    public boolean isEmpty() {
        return documents.isEmpty();
    }

    public String context() {
        return documents.stream()
                .map(RagSkillRetrievalResult::renderDocument)
                .collect(Collectors.joining("\n\n"));
    }

    private static String renderDocument(RagSkillRetrievedDocument document) {
        String title = document.title().isBlank() ? document.source() : document.title();
        if (title.isBlank()) {
            return document.content();
        }
        return title + ":\n" + document.content();
    }
}
