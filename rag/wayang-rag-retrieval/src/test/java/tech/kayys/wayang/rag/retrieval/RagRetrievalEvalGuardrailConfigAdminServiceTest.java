package tech.kayys.wayang.rag.retrieval;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RagRetrievalEvalGuardrailConfigAdminServiceTest {

    @AfterEach
    void clearProperties() {
        System.clearProperty("rag.runtime.eval.retrieval.guardrail.enabled");
        System.clearProperty("rag.runtime.eval.retrieval.guardrail.window-size");
        System.clearProperty("rag.runtime.eval.retrieval.guardrail.recall-drop-max");
        System.clearProperty("rag.runtime.eval.retrieval.guardrail.mrr-drop-max");
        System.clearProperty("rag.runtime.eval.retrieval.guardrail.latency-p95-increase-max-ms");
        System.clearProperty("rag.runtime.eval.retrieval.guardrail.latency-avg-increase-max-ms");
    }

    @Test
    void shouldUpdateConfigLive() {
        RagRuntimeConfig config = new RagRuntimeConfig();
        RagRetrievalEvalGuardrailConfigAdminService service = new RagRetrievalEvalGuardrailConfigAdminService();
        service.config = config;

        RagRetrievalEvalGuardrailConfigStatus status = service.update(new RagRetrievalEvalGuardrailConfigUpdate(
                false,
                12,
                0.08,
                0.09,
                220.0,
                140.0));

        assertEquals(false, status.config().enabled());
        assertEquals(12, status.config().windowSize());
        assertEquals(0.08, status.config().recallDropMax());
        assertEquals(0.09, status.config().mrrDropMax());
        assertEquals(220.0, status.config().latencyP95IncreaseMaxMs());
        assertEquals(140.0, status.config().latencyAvgIncreaseMaxMs());
    }

    @Test
    void shouldReloadConfigFromProperties() {
        System.setProperty("rag.runtime.eval.retrieval.guardrail.enabled", "false");
        System.setProperty("rag.runtime.eval.retrieval.guardrail.window-size", "15");
        System.setProperty("rag.runtime.eval.retrieval.guardrail.recall-drop-max", "0.11");
        System.setProperty("rag.runtime.eval.retrieval.guardrail.mrr-drop-max", "0.12");
        System.setProperty("rag.runtime.eval.retrieval.guardrail.latency-p95-increase-max-ms", "260");
        System.setProperty("rag.runtime.eval.retrieval.guardrail.latency-avg-increase-max-ms", "130");

        RagRuntimeConfig config = new RagRuntimeConfig();
        RagRetrievalEvalGuardrailConfigAdminService service = new RagRetrievalEvalGuardrailConfigAdminService();
        service.config = config;

        RagRetrievalEvalGuardrailConfigStatus status = service.reload();

        assertEquals(false, status.config().enabled());
        assertEquals(15, status.config().windowSize());
        assertEquals(0.11, status.config().recallDropMax());
        assertEquals(0.12, status.config().mrrDropMax());
        assertEquals(260.0, status.config().latencyP95IncreaseMaxMs());
        assertEquals(130.0, status.config().latencyAvgIncreaseMaxMs());
    }
}
