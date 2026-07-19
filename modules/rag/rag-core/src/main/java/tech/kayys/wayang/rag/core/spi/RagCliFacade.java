package tech.kayys.wayang.rag.core.spi;

public interface RagCliFacade {
    void ingestUrl(String tenantId, String url) throws Exception;
    void query(String tenantId, String query, String collectionName, java.util.function.Consumer<String> answerConsumer) throws Exception;
    /**
     * Synchronously queries the RAG backend (useful for agent tools).
     */
    String querySync(String tenantId, String query, String collectionName) throws Exception;

    /**
     * Ingests a local workspace recursively.
     */
    void ingestWorkspace(String tenantId, java.nio.file.Path workspaceDir) throws Exception;
}
