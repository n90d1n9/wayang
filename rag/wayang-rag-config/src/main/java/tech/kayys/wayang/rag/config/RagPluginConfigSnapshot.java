package tech.kayys.wayang.rag.config;

public record RagPluginConfigSnapshot(
                String selectionStrategy,
                String enabledIds,
                String order,
                String tenantEnabledOverrides,
                String tenantOrderOverrides,
                boolean normalizeLowercase,
                int normalizeMaxQueryLength,
                String vectorstoreBackend,
                int embeddingDimension,
                double lexicalRerankOriginalWeight,
                double lexicalRerankLexicalWeight,
                boolean lexicalRerankAnnotateMetadata,
                String safetyBlockedTerms,
                String safetyMask) {
}
