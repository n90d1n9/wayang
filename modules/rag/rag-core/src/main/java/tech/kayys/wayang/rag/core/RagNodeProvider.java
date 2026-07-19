package tech.kayys.wayang.rag.core;
import tech.kayys.wayang.rag.schema.*;

import tech.kayys.wayang.plugin.spi.node.NodeDefinition;
import tech.kayys.wayang.plugin.spi.node.NodeProvider;
import tech.kayys.wayang.schema.generator.SchemaGeneratorUtils;

import java.util.List;
import java.util.Map;

/**
 * Contributes all RAG-related node definitions to the unified catalog.
 *
 * Discovered at runtime by {@link java.util.ServiceLoader}.
 */
public class RagNodeProvider implements NodeProvider {

        @Override
        public String description() {
                return "RAG Node Provider";
        }

        @Override
        public String version() {
                return "1.0.0";
        }

        @Override
        public String id() {
                return "tech.kayys.wayang.rag.plugin";
        }

        @Override
        public String name() {
                return "RAG Providers";
        }

        @Override
        public List<NodeDefinition> nodes() {
                return List.of(
                                new NodeDefinition(
                                                "rag-executor", "RAG Query", "AI", "Retrieval",
                                                "Retrieval-Augmented Generation query executor",
                                                "search", "#10B981",
                                                SchemaGeneratorUtils.generateSchema(RagQueryConfig.class),
                                                SchemaGeneratorUtils.generateSchema(RagWorkflowInputSchema.class),
                                                SchemaGeneratorUtils.generateSchema(RagResponseSchema.class),
                                                Map.of()),
                                new NodeDefinition(
                                                "rag-retrieval-config", "RAG Retrieval", "AI", "Retrieval",
                                                "Configures document retrieval strategy",
                                                "database", "#10B981",
                                                SchemaGeneratorUtils.generateSchema(RagRetrievalConfig.class),
                                                null, null, Map.of()),
                                new NodeDefinition(
                                                "rag-generation-config", "RAG Generation", "AI", "Generation",
                                                "Configures response generation strategy",
                                                "sparkles", "#8B5CF6",
                                                SchemaGeneratorUtils.generateSchema(RagGenerationConfig.class),
                                                null, null, Map.of()),
                                new NodeDefinition(
                                                "rag-citation", "RAG Citation", "AI", "Retrieval",
                                                "Citation schema for RAG responses",
                                                "quote", "#10B981",
                                                SchemaGeneratorUtils.generateSchema(RagCitationSchema.class),
                                                null, null, Map.of()),
                                new NodeDefinition(
                                                "rag-source-document", "RAG Source Document", "AI", "Retrieval",
                                                "Source document reference for RAG",
                                                "file-text", "#10B981",
                                                SchemaGeneratorUtils.generateSchema(RagSourceDocumentSchema.class),
                                                null, null, Map.of()),
                                new NodeDefinition(
                                                "rag-metrics", "RAG Metrics", "AI", "Observability",
                                                "RAG execution metrics",
                                                "bar-chart", "#F59E0B",
                                                SchemaGeneratorUtils.generateSchema(RagMetricsSchema.class),
                                                null, null, Map.of()),
                                new NodeDefinition(
                                                "rag-response", "RAG Response", "AI", "Retrieval",
                                                "Complete RAG response structure",
                                                "message-circle", "#10B981",
                                                SchemaGeneratorUtils.generateSchema(RagResponseSchema.class),
                                                null, null, Map.of()),
                                new NodeDefinition(
                                                "rag-workflow-input", "RAG Workflow Input", "AI", "Retrieval",
                                                "Input schema for RAG workflows",
                                                "inbox", "#10B981",
                                                SchemaGeneratorUtils.generateSchema(RagWorkflowInputSchema.class),
                                                null, null, Map.of()));
        }
}
