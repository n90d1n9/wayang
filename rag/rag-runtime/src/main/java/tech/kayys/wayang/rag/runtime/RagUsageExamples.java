package tech.kayys.wayang.rag.runtime;

import tech.kayys.wayang.rag.core.CitationStyle;
import tech.kayys.wayang.rag.core.ConversationTurn;
import tech.kayys.wayang.rag.core.GenerationConfig;
import tech.kayys.wayang.rag.core.RagMode;
import tech.kayys.wayang.rag.core.RerankingModel;
import tech.kayys.wayang.rag.core.RetrievalConfig;
import tech.kayys.wayang.rag.core.SearchStrategy;

import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Complete examples showing how to use the RAG system
 */
public class RagUsageExamples {

        /**
         * Example 1: Simple document ingestion and query
         */
        public static void example1_SimpleRag(
                        DocumentIngestionService ingestionService,
                        RagQueryService queryService) {

                String tenantId = "acme-corp";

                // 1. Ingest documents
                List<Path> pdfPaths = List.of(
                                Path.of("/docs/product-manual.pdf"),
                                Path.of("/docs/faq.pdf"));

                Map<String, String> metadata = Map.of(
                                "collection", "product-docs",
                                "category", "support");

                ingestionService.ingestPdfDocuments(tenantId, pdfPaths, metadata)
                                .subscribe().with(
                                                result -> System.out.printf(
                                                                "Ingested %d documents, created %d segments%n",
                                                                result.documentsIngested(),
                                                                result.segmentsCreated()),
                                                error -> System.err.println("Ingestion failed: " + error));

                // 2. Query the documents
                String query = "How do I reset my password?";

                queryService.query(tenantId, query, "product-docs")
                                .subscribe().with(
                                                response -> {
                                                        System.out.println("Answer: " + response.answer());
                                                        System.out.println("\nSources:");
                                                        response.sourceDocuments().forEach(doc -> System.out
                                                                        .println("  - " + doc.getTitle()));
                                                },
                                                error -> System.err.println("Query failed: " + error));
        }

        /**
         * Example 2: Advanced RAG with custom configuration
         */
        public static void example2_AdvancedRag(RagQueryService queryService) {

                String tenantId = "enterprise-client";
                String query = "What are the security best practices for API integration?";

                // Custom retrieval configuration
                RetrievalConfig retrievalConfig = new RetrievalConfig(
                                10, // topK
                                0.75f, // minSimilarity
                                1024, // maxChunkSize
                                100, // chunkOverlap
                                true, // enableReranking
                                RerankingModel.COHERE_RERANK,
                                true, // enableHybridSearch
                                0.6f, // hybridAlpha (favor vector search)
                                true, // enableMultiQuery
                                5, // numQueryVariations
                                false, 2048, Map.of(), List.of(), true, true);

                // Custom generation configuration
                GenerationConfig generationConfig = new GenerationConfig(
                                "anthropic",
                                "claude-3-sonnet-20240229",
                                0.5f, // temperature (more focused)
                                2048, // maxTokens
                                0.95f, 0.0f, 0.0f,
                                List.of(),
                                "You are a security expert. Provide detailed, accurate answers with best practices.",
                                null,
                                false, true, CitationStyle.INLINE_NUMBERED,
                                false, true, Map.of());

                RagQueryRequest request = new RagQueryRequest(
                                tenantId,
                                query,
                                RagMode.STANDARD,
                                SearchStrategy.SEMANTIC_RERANK,
                                retrievalConfig,
                                generationConfig,
                                List.of("security-docs", "api-docs"),
                                Map.of("verified", true));

                queryService.advancedQuery(request)
                                .subscribe().with(
                                                response -> {
                                                        System.out.println("Answer with citations:");
                                                        System.out.println(response.answer());
                                                        System.out.println("\nMetrics:");
                                                        System.out.println("  Duration: " +
                                                                        response.metrics().totalDurationMs() + "ms");
                                                        System.out.println("  Docs retrieved: " +
                                                                        response.metrics().documentsRetrieved());
                                                        System.out.println("  Tokens generated: " +
                                                                        response.metrics().tokensGenerated());
                                                },
                                                error -> System.err.println("Query failed: " + error));
        }

        /**
         * Example 3: Conversational RAG
         */
        public static void example3_ConversationalRag(RagQueryService queryService) {

                String tenantId = "support-bot";
                String sessionId = UUID.randomUUID().toString();

                List<ConversationTurn> history = new ArrayList<>();

                // First turn
                String query1 = "What is our refund policy?";
                queryService.conversationalQuery(tenantId, query1, sessionId, history)
                                .subscribe().with(
                                                response -> {
                                                        history.add(new ConversationTurn(
                                                                        query1,
                                                                        response.answer(),
                                                                        Instant.now()));

                                                        // Second turn with context
                                                        String query2 = "How long does it take?";
                                                        queryService.conversationalQuery(tenantId, query2, sessionId,
                                                                        history)
                                                                        .subscribe().with(
                                                                                        response2 -> System.out.println(
                                                                                                        "Answer: " + response2
                                                                                                                        .answer()),
                                                                                        error -> System.err.println(
                                                                                                        "Query failed: " + error));
                                                },
                                                error -> System.err.println("Query failed: " + error));
        }

        /**
         * Example 4: Batch document ingestion
         */
        public static void example4_BatchIngestion(DocumentIngestionService ingestionService) {

                String tenantId = "knowledge-base";

                List<DocumentSource> sources = List.of(
                                new DocumentSource(
                                                SourceType.PDF,
                                                "/docs/handbook.pdf",
                                                null,
                                                Map.of("collection", "handbook", "version", "2024")),
                                new DocumentSource(
                                                SourceType.TEXT,
                                                null,
                                                "Company mission: We build great products...",
                                                Map.of("collection", "about", "type", "mission")),
                                new DocumentSource(
                                                SourceType.URL,
                                                "https://example.com/docs",
                                                null,
                                                Map.of("collection", "external", "source", "website")));

                ingestionService.batchIngest(tenantId, sources)
                                .subscribe().with(
                                                result -> System.out.printf(
                                                                "Batch ingestion completed: %d docs, %d segments in %dms%n",
                                                                result.documentsIngested(),
                                                                result.segmentsCreated(),
                                                                result.durationMs()),
                                                error -> System.err.println("Batch ingestion failed: " + error));
        }
}