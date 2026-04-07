package tech.kayys.wayang.rag.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import tech.kayys.wayang.rag.runtime.RagPluginTenantStrategyResolver;
import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagPluginConfigAdminServiceTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty("rag.runtime.rag.plugins.selection-strategy");
        System.clearProperty("rag.runtime.rag.plugins.enabled");
        System.clearProperty("rag.runtime.rag.plugins.order");
        System.clearProperty("rag.runtime.rag.plugins.tenant-enabled");
        System.clearProperty("rag.runtime.rag.plugins.tenant-order");
        System.clearProperty("rag.runtime.rag.plugins.normalize-query.lowercase");
        System.clearProperty("rag.runtime.rag.plugins.normalize-query.max-query-length");
        System.clearProperty("rag.runtime.rag.plugins.lexical-rerank.original-weight");
        System.clearProperty("rag.runtime.rag.plugins.lexical-rerank.lexical-weight");
        System.clearProperty("rag.runtime.rag.plugins.lexical-rerank.annotate-metadata");
        System.clearProperty("rag.runtime.rag.plugins.safety-filter.blocked-terms");
        System.clearProperty("rag.runtime.rag.plugins.safety-filter.mask");
    }

    @Test
    void shouldUpdatePluginConfigLive() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        RagPluginConfigAdminService service = new RagPluginConfigAdminService();
        service.config = config;
        service.strategyResolver = new RagPluginTenantStrategyResolver(config);

        RagPluginConfigStatus status = service.update(new RagPluginConfigUpdate(
                "config",
                "normalize-query,safety-filter",
                "safety-filter,normalize-query",
                "tenant-a=safety-filter;tenant-b=*",
                "tenant-a=safety-filter,normalize-query",
                true,
                2048,
                "faiss",
                1536,
                0.45,
                0.55,
                false,
                "secret,token",
                "[MASK]"));

        assertEquals("config", status.config().selectionStrategy());
        assertEquals("normalize-query,safety-filter", status.config().enabledIds());
        assertEquals("safety-filter,normalize-query", status.config().order());
        assertEquals("tenant-a=safety-filter;tenant-b=*", status.config().tenantEnabledOverrides());
        assertEquals("tenant-a=safety-filter,normalize-query", status.config().tenantOrderOverrides());
        assertTrue(status.config().normalizeLowercase());
        assertEquals(2048, status.config().normalizeMaxQueryLength());
        assertEquals("faiss", status.config().vectorstoreBackend());
        assertEquals(1536, status.config().embeddingDimension());
        assertEquals(0.45, status.config().lexicalRerankOriginalWeight());
        assertEquals(0.55, status.config().lexicalRerankLexicalWeight());
        assertFalse(status.config().lexicalRerankAnnotateMetadata());
        assertEquals("secret,token", status.config().safetyBlockedTerms());
        assertEquals("[MASK]", status.config().safetyMask());
    }

    @Test
    void shouldReloadPluginConfigFromProperties() {
        System.setProperty("rag.runtime.rag.plugins.selection-strategy", "config");
        System.setProperty("rag.runtime.rag.plugins.enabled", "lexical-rerank");
        System.setProperty("rag.runtime.rag.plugins.order", "normalize-query,lexical-rerank");
        System.setProperty("rag.runtime.rag.plugins.tenant-enabled", "tenant-a=normalize-query,safety-filter");
        System.setProperty("rag.runtime.rag.plugins.tenant-order", "tenant-a=safety-filter,normalize-query");
        System.setProperty("rag.runtime.rag.plugins.normalize-query.lowercase", "true");
        System.setProperty("rag.runtime.rag.plugins.normalize-query.max-query-length", "1024");
        System.setProperty("rag.runtime.vectorstore.backend", "faiss");
        System.setProperty("rag.runtime.embedding.dimension", "1536");
        System.setProperty("rag.runtime.rag.plugins.lexical-rerank.original-weight", "0.4");
        System.setProperty("rag.runtime.rag.plugins.lexical-rerank.lexical-weight", "0.6");
        System.setProperty("rag.runtime.rag.plugins.lexical-rerank.annotate-metadata", "false");
        System.setProperty("rag.runtime.rag.plugins.safety-filter.blocked-terms", "pii,password");
        System.setProperty("rag.runtime.rag.plugins.safety-filter.mask", "[REDACT]");

        RagRuntimeConfig config = new RagRuntimeConfig();
        RagPluginConfigAdminService service = new RagPluginConfigAdminService();
        service.config = config;
        service.strategyResolver = new RagPluginTenantStrategyResolver(config);

        RagPluginConfigStatus status = service.reload();

        assertEquals("config", status.config().selectionStrategy());
        assertEquals("lexical-rerank", status.config().enabledIds());
        assertEquals("normalize-query,lexical-rerank", status.config().order());
        assertEquals("tenant-a=normalize-query,safety-filter", status.config().tenantEnabledOverrides());
        assertEquals("tenant-a=safety-filter,normalize-query", status.config().tenantOrderOverrides());
        assertTrue(status.config().normalizeLowercase());
        assertEquals(1024, status.config().normalizeMaxQueryLength());
        assertEquals("faiss", status.config().vectorstoreBackend());
        assertEquals(1536, status.config().embeddingDimension());
        assertEquals(0.4, status.config().lexicalRerankOriginalWeight());
        assertEquals(0.6, status.config().lexicalRerankLexicalWeight());
        assertFalse(status.config().lexicalRerankAnnotateMetadata());
        assertEquals("pii,password", status.config().safetyBlockedTerms());
        assertEquals("[REDACT]", status.config().safetyMask());
    }

    @Test
    void shouldRejectMalformedTenantOverrideSyntax() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        RagPluginConfigAdminService service = new RagPluginConfigAdminService();
        service.config = config;
        service.strategyResolver = new RagPluginTenantStrategyResolver(config);

        RagPluginConfigValidationException ex = assertThrows(RagPluginConfigValidationException.class,
                () -> service.update(new RagPluginConfigUpdate(
                        null,
                        "normalize-query",
                        "normalize-query",
                        "tenant-a=normalize-query",
                        "tenant-a",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));

        assertEquals("invalid_entry", ex.getCode());
        assertEquals("tenantOrderOverrides", ex.getField());
    }

    @Test
    void shouldRejectInvalidTenantOverrideValues() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        RagPluginConfigAdminService service = new RagPluginConfigAdminService();
        service.config = config;
        service.strategyResolver = new RagPluginTenantStrategyResolver(config);

        assertThrows(RagPluginConfigValidationException.class, () -> service.update(new RagPluginConfigUpdate(
                null,
                "normalize-query",
                "normalize-query",
                "tenant-a=normalize-query;tenant-a=safety-filter",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null)));

        assertThrows(RagPluginConfigValidationException.class, () -> service.update(new RagPluginConfigUpdate(
                null,
                "normalize-query",
                "normalize-query",
                null,
                "tenant-a=*",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null)));
    }

    @Test
    void shouldRejectUnknownSelectionStrategy() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        RagPluginConfigAdminService service = new RagPluginConfigAdminService();
        service.config = config;
        service.strategyResolver = new RagPluginTenantStrategyResolver(config);

        RagPluginConfigValidationException ex = assertThrows(RagPluginConfigValidationException.class,
                () -> service.update(new RagPluginConfigUpdate(
                        "unknown-strategy",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null)));

        assertEquals("unknown_strategy_id", ex.getCode());
        assertEquals("selectionStrategy", ex.getField());
    }
}
