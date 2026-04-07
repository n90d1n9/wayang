package tech.kayys.wayang.rag.config;

public record RagPluginConfigUpdate(
                String selectionStrategy,
                String enabledIds,
                String order,
                String tenantEnabledOverrides,
                String tenantOrderOverrides,
                Boolean normalizeLowercase,
                Integer normalizeMaxQueryLength,
                String vectorstoreBackend,
                Integer embeddingDimension,
                Double lexicalRerankOriginalWeight,
                Double lexicalRerankLexicalWeight,
                Boolean lexicalRerankAnnotateMetadata,
                String safetyBlockedTerms,
                String safetyMask) {
}
