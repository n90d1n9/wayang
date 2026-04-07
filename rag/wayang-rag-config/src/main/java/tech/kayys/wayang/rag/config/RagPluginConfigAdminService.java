package tech.kayys.wayang.rag.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import tech.kayys.wayang.rag.runtime.RagPluginTenantStrategyResolver;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;

import java.time.Instant;

@ApplicationScoped
public class RagPluginConfigAdminService {

    @Inject
    RagRuntimeConfig config;
    @Inject
    RagPluginTenantStrategyResolver strategyResolver;

    public RagPluginConfigStatus status() {
        return new RagPluginConfigStatus(current(), Instant.now());
    }

    public RagPluginConfigStatus update(RagPluginConfigUpdate update) {
        if (update == null) {
            return status();
        }
        if (update.selectionStrategy() != null) {
            String strategy = update.selectionStrategy().trim();
            if (strategy.isEmpty()) {
                throw new RagPluginConfigValidationException(
                        "empty_strategy_id",
                        "selectionStrategy",
                        null,
                        update.selectionStrategy(),
                        "Strategy id must be non-empty.");
            }
            if (!strategyResolver.isKnownStrategy(strategy)) {
                throw new RagPluginConfigValidationException(
                        "unknown_strategy_id",
                        "selectionStrategy",
                        null,
                        strategy,
                        "Unknown strategy id. Available: " + String.join(",", strategyResolver.availableStrategyIds()));
            }
            config.setRagPluginSelectionStrategy(strategy);
        }
        if (update.enabledIds() != null) {
            config.setRagPluginEnabledIds(update.enabledIds());
        }
        if (update.order() != null) {
            config.setRagPluginOrder(update.order());
        }
        if (update.tenantEnabledOverrides() != null) {
            RagPluginConfigValidation.validateTenantOverrideSyntax(
                    "tenantEnabledOverrides",
                    update.tenantEnabledOverrides(),
                    true,
                    RagPluginConfigValidationException::new);
            config.setRagPluginTenantEnabledOverrides(update.tenantEnabledOverrides());
        }
        if (update.tenantOrderOverrides() != null) {
            RagPluginConfigValidation.validateTenantOverrideSyntax(
                    "tenantOrderOverrides",
                    update.tenantOrderOverrides(),
                    false,
                    RagPluginConfigValidationException::new);
            config.setRagPluginTenantOrderOverrides(update.tenantOrderOverrides());
        }
        if (update.normalizeLowercase() != null) {
            config.setRagPluginNormalizeLowercase(update.normalizeLowercase());
        }
        if (update.normalizeMaxQueryLength() != null) {
            config.setRagPluginNormalizeMaxQueryLength(update.normalizeMaxQueryLength());
        }
        if (update.vectorstoreBackend() != null) {
            config.setVectorstoreBackend(update.vectorstoreBackend());
        }
        if (update.embeddingDimension() != null) {
            config.setEmbeddingDimension(update.embeddingDimension());
        }
        if (update.lexicalRerankOriginalWeight() != null) {
            config.setRagPluginRerankOriginalWeight(update.lexicalRerankOriginalWeight());
        }
        if (update.lexicalRerankLexicalWeight() != null) {
            config.setRagPluginRerankLexicalWeight(update.lexicalRerankLexicalWeight());
        }
        if (update.lexicalRerankAnnotateMetadata() != null) {
            config.setRagPluginRerankAnnotateMetadata(update.lexicalRerankAnnotateMetadata());
        }
        if (update.safetyBlockedTerms() != null) {
            config.setRagPluginSafetyBlockedTerms(update.safetyBlockedTerms());
        }
        if (update.safetyMask() != null) {
            config.setRagPluginSafetyMask(update.safetyMask());
        }
        return status();
    }

    public RagPluginConfigStatus reload() {
        Config mp = ConfigProvider.getConfig();
        config.setRagPluginSelectionStrategy(readString(
                mp,
                "rag.runtime.rag.plugins.selection-strategy",
                config.getRagPluginSelectionStrategy()));
        config.setRagPluginEnabledIds(
                readString(mp, "rag.runtime.rag.plugins.enabled", config.getRagPluginEnabledIds()));
        config.setRagPluginOrder(readString(mp, "rag.runtime.rag.plugins.order", config.getRagPluginOrder()));
        config.setRagPluginTenantEnabledOverrides(readString(
                mp,
                "rag.runtime.rag.plugins.tenant-enabled",
                config.getRagPluginTenantEnabledOverrides()));
        config.setRagPluginTenantOrderOverrides(readString(
                mp,
                "rag.runtime.rag.plugins.tenant-order",
                config.getRagPluginTenantOrderOverrides()));
        config.setRagPluginNormalizeLowercase(readBoolean(
                mp,
                "rag.runtime.rag.plugins.normalize-query.lowercase",
                config.isRagPluginNormalizeLowercase()));
        config.setRagPluginNormalizeMaxQueryLength(readInt(
                mp,
                "rag.runtime.rag.plugins.normalize-query.max-query-length",
                config.getRagPluginNormalizeMaxQueryLength()));
        config.setVectorstoreBackend(readString(
                mp,
                "rag.runtime.vectorstore.backend",
                config.getVectorstoreBackend()));
        config.setEmbeddingDimension(readInt(
                mp,
                "rag.runtime.embedding.dimension",
                config.getEmbeddingDimension()));
        config.setRagPluginRerankOriginalWeight(readDouble(
                mp,
                "rag.runtime.rag.plugins.lexical-rerank.original-weight",
                config.getRagPluginRerankOriginalWeight()));
        config.setRagPluginRerankLexicalWeight(readDouble(
                mp,
                "rag.runtime.rag.plugins.lexical-rerank.lexical-weight",
                config.getRagPluginRerankLexicalWeight()));
        config.setRagPluginRerankAnnotateMetadata(readBoolean(
                mp,
                "rag.runtime.rag.plugins.lexical-rerank.annotate-metadata",
                config.isRagPluginRerankAnnotateMetadata()));
        config.setRagPluginSafetyBlockedTerms(readString(
                mp,
                "rag.runtime.rag.plugins.safety-filter.blocked-terms",
                config.getRagPluginSafetyBlockedTerms()));
        config.setRagPluginSafetyMask(readString(
                mp,
                "rag.runtime.rag.plugins.safety-filter.mask",
                config.getRagPluginSafetyMask()));
        return status();
    }

    private RagPluginConfigSnapshot current() {
        return new RagPluginConfigSnapshot(
                config.getRagPluginSelectionStrategy(),
                config.getRagPluginEnabledIds(),
                config.getRagPluginOrder(),
                config.getRagPluginTenantEnabledOverrides(),
                config.getRagPluginTenantOrderOverrides(),
                config.isRagPluginNormalizeLowercase(),
                config.getRagPluginNormalizeMaxQueryLength(),
                config.getVectorstoreBackend(),
                config.getEmbeddingDimension(),
                config.getRagPluginRerankOriginalWeight(),
                config.getRagPluginRerankLexicalWeight(),
                config.isRagPluginRerankAnnotateMetadata(),
                config.getRagPluginSafetyBlockedTerms(),
                config.getRagPluginSafetyMask());
    }

    private static String readString(Config config, String key, String fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .orElse(fallback);
    }

    private static boolean readBoolean(Config config, String key, boolean fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> parseBoolean(v, fallback))
                .orElse(fallback);
    }

    private static int readInt(Config config, String key, int fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> parseInt(v, fallback))
                .orElse(fallback);
    }

    private static double readDouble(Config config, String key, double fallback) {
        return config.getOptionalValue(key, String.class)
                .map(String::trim)
                .filter(v -> !v.isBlank())
                .map(v -> parseDouble(v, fallback))
                .orElse(fallback);
    }

    private static boolean parseBoolean(String value, boolean fallback) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        if ("true".equals(normalized)) {
            return true;
        }
        if ("false".equals(normalized)) {
            return false;
        }
        return fallback;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
