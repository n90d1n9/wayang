package tech.kayys.wayang.rag.runtime.langchain;

import org.junit.jupiter.api.Test;

import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagRuntimeConfigTest {

    @Test
    void shouldExposeDefaults() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        assertFalse(config.isLogRequests());
        assertFalse(config.isLogResponses());
        assertEquals("postgres", config.getVectorstoreBackend());
        assertEquals(1536, config.getEmbeddingDimension());
        assertEquals("hash-1536", config.getEmbeddingModel());
        assertEquals("v1", config.getEmbeddingVersion());
        assertEquals("*", config.getRagPluginEnabledIds());
        assertEquals("", config.getRagPluginOrder());
        assertEquals("", config.getRagPluginTenantEnabledOverrides());
        assertEquals("", config.getRagPluginTenantOrderOverrides());
        assertEquals("config", config.getRagPluginSelectionStrategy());
        assertFalse(config.isRagPluginNormalizeLowercase());
        assertEquals(4096, config.getRagPluginNormalizeMaxQueryLength());
        assertEquals(0.7, config.getRagPluginRerankOriginalWeight());
        assertEquals(0.3, config.getRagPluginRerankLexicalWeight());
        assertTrue(config.isRagPluginRerankAnnotateMetadata());
        assertEquals("", config.getRagPluginSafetyBlockedTerms());
        assertEquals("[REDACTED]", config.getRagPluginSafetyMask());
        assertEquals(null, config.getEmbeddingSchemaHistoryPath());
        assertEquals(null, config.getSloAlertSnoozePath());
        assertEquals(null, config.getRetrievalEvalHistoryPath());
        assertEquals(1000, config.getRetrievalEvalHistoryMaxEvents());
        assertTrue(config.isRetrievalEvalGuardrailEnabled());
        assertEquals(20, config.getRetrievalEvalGuardrailWindowSize());
        assertEquals(0.05, config.getRetrievalEvalGuardrailRecallDropMax());
        assertEquals(0.05, config.getRetrievalEvalGuardrailMrrDropMax());
        assertEquals(150.0, config.getRetrievalEvalGuardrailLatencyP95IncreaseMaxMs());
        assertEquals(75.0, config.getRetrievalEvalGuardrailLatencyAvgIncreaseMaxMs());
        assertEquals(800.0, config.getSloEmbeddingLatencyP95Ms());
        assertEquals(1500.0, config.getSloSearchLatencyP95Ms());
        assertEquals(4000.0, config.getSloIngestLatencyP95Ms());
        assertEquals(0.05, config.getSloEmbeddingFailureRate());
        assertEquals(0.05, config.getSloSearchFailureRate());
        assertEquals(60000L, config.getSloIndexLagMs());
        assertEquals(0.10, config.getSloCompactionFailureRate());
        assertEquals(172800000L, config.getSloCompactionCycleStalenessMs());
        assertEquals(1.0, config.getSloSeverityWarningMultiplier());
        assertEquals(2.0, config.getSloSeverityCriticalMultiplier());
        assertEquals(0, config.getSloSeverityWarningByMetric().size());
        assertEquals(0, config.getSloSeverityCriticalByMetric().size());
        assertTrue(config.isSloAlertEnabled());
        assertEquals("warning", config.getSloAlertMinSeverity());
        assertEquals(300000L, config.getSloAlertCooldownMs());
    }

    @Test
    void shouldSetEmbeddingConfig() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        config.setEmbeddingModel("tfidf-512");
        config.setEmbeddingDimension(512);
        config.setEmbeddingVersion("v3");
        config.setRagPluginEnabledIds("normalize-query,rerank");
        config.setRagPluginOrder("rerank,normalize-query");
        config.setRagPluginTenantEnabledOverrides("tenant-a=normalize-query|safety-filter");
        config.setRagPluginTenantOrderOverrides("tenant-a=safety-filter|normalize-query");
        config.setRagPluginSelectionStrategy("config");
        config.setRagPluginNormalizeLowercase(true);
        config.setRagPluginNormalizeMaxQueryLength(1024);
        config.setRagPluginRerankOriginalWeight(0.4);
        config.setRagPluginRerankLexicalWeight(0.6);
        config.setRagPluginRerankAnnotateMetadata(false);
        config.setRagPluginSafetyBlockedTerms("secret,token");
        config.setRagPluginSafetyMask("[MASK]");
        config.setEmbeddingSchemaHistoryPath("/tmp/schema-history.ndjson");
        config.setSloAlertSnoozePath("/tmp/slo-alert-snooze.json");
        config.setRetrievalEvalHistoryPath("/tmp/retrieval-eval-history.ndjson");
        config.setRetrievalEvalHistoryMaxEvents(250);
        config.setRetrievalEvalGuardrailEnabled(false);
        config.setRetrievalEvalGuardrailWindowSize(12);
        config.setRetrievalEvalGuardrailRecallDropMax(0.08);
        config.setRetrievalEvalGuardrailMrrDropMax(0.09);
        config.setRetrievalEvalGuardrailLatencyP95IncreaseMaxMs(220);
        config.setRetrievalEvalGuardrailLatencyAvgIncreaseMaxMs(120);
        config.setSloEmbeddingLatencyP95Ms(321);
        config.setSloSearchLatencyP95Ms(654);
        config.setSloIngestLatencyP95Ms(987);
        config.setSloEmbeddingFailureRate(0.12);
        config.setSloSearchFailureRate(0.34);
        config.setSloIndexLagMs(42);
        config.setSloCompactionFailureRate(0.56);
        config.setSloCompactionCycleStalenessMs(9876L);
        config.setSloSeverityWarningMultiplier(1.5);
        config.setSloSeverityCriticalMultiplier(3.0);
        config.setSloSeverityWarningByMetric(java.util.Map.of("embedding_latency_p95_ms", 1.9));
        config.setSloSeverityCriticalByMetric(java.util.Map.of("embedding_latency_p95_ms", 3.9));
        config.setSloAlertEnabled(false);
        config.setSloAlertMinSeverity("critical");
        config.setSloAlertCooldownMs(65000L);

        assertEquals("tfidf-512", config.getEmbeddingModel());
        assertEquals(512, config.getEmbeddingDimension());
        assertEquals("v3", config.getEmbeddingVersion());
        assertEquals("normalize-query,rerank", config.getRagPluginEnabledIds());
        assertEquals("rerank,normalize-query", config.getRagPluginOrder());
        assertEquals("tenant-a=normalize-query|safety-filter", config.getRagPluginTenantEnabledOverrides());
        assertEquals("tenant-a=safety-filter|normalize-query", config.getRagPluginTenantOrderOverrides());
        assertEquals("config", config.getRagPluginSelectionStrategy());
        assertTrue(config.isRagPluginNormalizeLowercase());
        assertEquals(1024, config.getRagPluginNormalizeMaxQueryLength());
        assertEquals(0.4, config.getRagPluginRerankOriginalWeight());
        assertEquals(0.6, config.getRagPluginRerankLexicalWeight());
        assertFalse(config.isRagPluginRerankAnnotateMetadata());
        assertEquals("secret,token", config.getRagPluginSafetyBlockedTerms());
        assertEquals("[MASK]", config.getRagPluginSafetyMask());
        assertEquals("/tmp/schema-history.ndjson", config.getEmbeddingSchemaHistoryPath());
        assertEquals("/tmp/slo-alert-snooze.json", config.getSloAlertSnoozePath());
        assertEquals("/tmp/retrieval-eval-history.ndjson", config.getRetrievalEvalHistoryPath());
        assertEquals(250, config.getRetrievalEvalHistoryMaxEvents());
        assertFalse(config.isRetrievalEvalGuardrailEnabled());
        assertEquals(12, config.getRetrievalEvalGuardrailWindowSize());
        assertEquals(0.08, config.getRetrievalEvalGuardrailRecallDropMax());
        assertEquals(0.09, config.getRetrievalEvalGuardrailMrrDropMax());
        assertEquals(220.0, config.getRetrievalEvalGuardrailLatencyP95IncreaseMaxMs());
        assertEquals(120.0, config.getRetrievalEvalGuardrailLatencyAvgIncreaseMaxMs());
        assertEquals(321.0, config.getSloEmbeddingLatencyP95Ms());
        assertEquals(654.0, config.getSloSearchLatencyP95Ms());
        assertEquals(987.0, config.getSloIngestLatencyP95Ms());
        assertEquals(0.12, config.getSloEmbeddingFailureRate());
        assertEquals(0.34, config.getSloSearchFailureRate());
        assertEquals(42L, config.getSloIndexLagMs());
        assertEquals(0.56, config.getSloCompactionFailureRate());
        assertEquals(9876L, config.getSloCompactionCycleStalenessMs());
        assertEquals(1.5, config.getSloSeverityWarningMultiplier());
        assertEquals(3.0, config.getSloSeverityCriticalMultiplier());
        assertEquals(1.9, config.getSloSeverityWarningByMetric().get("embedding_latency_p95_ms"));
        assertEquals(3.9, config.getSloSeverityCriticalByMetric().get("embedding_latency_p95_ms"));
        assertFalse(config.isSloAlertEnabled());
        assertEquals("critical", config.getSloAlertMinSeverity());
        assertEquals(65000L, config.getSloAlertCooldownMs());
    }
}
