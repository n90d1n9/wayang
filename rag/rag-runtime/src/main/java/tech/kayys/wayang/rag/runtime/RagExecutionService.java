package tech.kayys.wayang.rag.runtime;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.kayys.wayang.rag.core.RagResponse;
import tech.kayys.wayang.rag.core.RagWorkflowInput;

/**
 * Service for executing RAG (Retrieval-Augmented Generation) workflows.
 * Acts as an entry point for processing RAG inputs and coordinating
 * the query execution through {@link RagQueryService}.
 */
@ApplicationScoped
public class RagExecutionService {

    private static final Logger LOG = LoggerFactory.getLogger(RagExecutionService.class);

    @Inject
    RagQueryService ragQueryService;

    public Uni<RagResponse> executeRagWorkflow(RagWorkflowInput input) {
        LOG.info("Executing native RAG workflow for tenant: {}", input.tenantId());
        return ragQueryService.query(input.tenantId(), input.query(), "default");
    }
}
