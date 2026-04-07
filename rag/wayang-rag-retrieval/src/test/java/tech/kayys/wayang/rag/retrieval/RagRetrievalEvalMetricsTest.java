package tech.kayys.wayang.rag.retrieval;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RagRetrievalEvalMetricsTest {

    @Test
    void shouldRecordRunAndGuardrailMetrics() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        RagRetrievalEvalMetrics metrics = new RagRetrievalEvalMetrics(registry);
        metrics.initGauges();

        RagRetrievalEvalResponse response = new RagRetrievalEvalResponse(
                "dataset-a",
                "tenant-a",
                5,
                0.2,
                "documentid",
                10,
                8,
                0.7,
                0.6,
                55.0,
                32.0,
                Instant.parse("2026-01-01T00:00:00Z"),
                List.of());

        metrics.recordRun(response);

        assertEquals(1.0, registry.find("wayang.rag.eval.retrieval.run.count").counter().count());
        assertEquals(10.0, registry.find("wayang.rag.eval.retrieval.query.count").counter().count());
        assertEquals(8.0, registry.find("wayang.rag.eval.retrieval.hit.count").counter().count());
        assertEquals(0.7, registry.find("wayang.rag.eval.retrieval.last.recall_at_k").gauge().value(), 1e-6);

        RagRetrievalEvalGuardrailStatus guardrail = new RagRetrievalEvalGuardrailStatus(
                true,
                false,
                "tenant-a",
                "dataset-a",
                20,
                2,
                "regression_detected",
                List.of(new RagRetrievalEvalGuardrailBreach("mrr_delta", -0.2, -0.1, "bad")),
                null,
                Instant.parse("2026-01-01T00:00:00Z"));

        metrics.recordGuardrail(guardrail);

        assertEquals(1.0, registry.find("wayang.rag.eval.retrieval.guardrail.check.count").counter().count());
        assertEquals(1.0, registry.find("wayang.rag.eval.retrieval.guardrail.breach.count").counter().count());
        assertEquals(0.0, registry.find("wayang.rag.eval.retrieval.guardrail.last.healthy").gauge().value(), 1e-6);
        assertEquals(1.0, registry.find("wayang.rag.eval.retrieval.guardrail.last.breach_count").gauge().value(), 1e-6);
    }
}
