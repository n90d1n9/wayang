package tech.kayys.wayang.rag.retrieval;

import com.fasterxml.jackson.databind.ObjectMapper;

import tech.kayys.wayang.rag.runtime.RagRuntimeConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagRetrievalEvalGuardrailServiceTest {

        @Test
        void shouldReportRegressionBreaches(@TempDir Path tempDir) {
                RagRuntimeConfig config = new RagRuntimeConfig();
                config.setRetrievalEvalHistoryPath(tempDir.resolve("eval-history.ndjson").toString());
                config.setRetrievalEvalGuardrailEnabled(true);
                config.setRetrievalEvalGuardrailWindowSize(20);
                config.setRetrievalEvalGuardrailRecallDropMax(0.05);
                config.setRetrievalEvalGuardrailMrrDropMax(0.05);
                config.setRetrievalEvalGuardrailLatencyP95IncreaseMaxMs(20);
                config.setRetrievalEvalGuardrailLatencyAvgIncreaseMaxMs(10);

                RagRetrievalEvalHistoryService history = new RagRetrievalEvalHistoryService(
                                config,
                                new ObjectMapper().findAndRegisterModules(),
                                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC")));

                history.append(run(0.8, 0.7, 50, 30, Instant.parse("2026-01-01T00:00:01Z")));
                history.append(run(0.6, 0.5, 80, 45, Instant.parse("2026-01-01T00:00:02Z")));

                RagRetrievalEvalGuardrailService service = new RagRetrievalEvalGuardrailService(
                                config,
                                history,
                                null,
                                Clock.fixed(Instant.parse("2026-01-01T00:00:03Z"), ZoneId.of("UTC")));

                RagRetrievalEvalGuardrailStatus status = service.evaluate("tenant-a", "dataset-a", null);
                assertFalse(status.healthy());
                assertEquals("regression_detected", status.reason());
                assertEquals(4, status.breaches().size());
                assertTrue(status.breaches().stream().anyMatch(b -> b.metric().equals("recall_at_k_delta")));
                assertTrue(status.breaches().stream().anyMatch(b -> b.metric().equals("mrr_delta")));
                assertTrue(status.breaches().stream().anyMatch(b -> b.metric().equals("latency_p95_ms_delta")));
                assertTrue(status.breaches().stream().anyMatch(b -> b.metric().equals("latency_avg_ms_delta")));
        }

        @Test
        void shouldReturnHealthyWhenDisabled(@TempDir Path tempDir) {
                RagRuntimeConfig config = new RagRuntimeConfig();
                config.setRetrievalEvalHistoryPath(tempDir.resolve("eval-history.ndjson").toString());
                config.setRetrievalEvalGuardrailEnabled(false);

                RagRetrievalEvalHistoryService history = new RagRetrievalEvalHistoryService(
                                config,
                                new ObjectMapper().findAndRegisterModules(),
                                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC")));

                history.append(run(0.8, 0.7, 50, 30, Instant.parse("2026-01-01T00:00:01Z")));
                history.append(run(0.6, 0.5, 80, 45, Instant.parse("2026-01-01T00:00:02Z")));

                RagRetrievalEvalGuardrailService service = new RagRetrievalEvalGuardrailService(
                                config,
                                history,
                                null,
                                Clock.fixed(Instant.parse("2026-01-01T00:00:03Z"), ZoneId.of("UTC")));

                RagRetrievalEvalGuardrailStatus status = service.evaluate("tenant-a", "dataset-a", null);
                assertTrue(status.healthy());
                assertFalse(status.enabled());
                assertEquals("guardrail_disabled", status.reason());
                assertEquals(0, status.breaches().size());
        }

        private static RagRetrievalEvalResponse run(
                        double recall,
                        double mrr,
                        double p95,
                        double avg,
                        Instant at) {
                return new RagRetrievalEvalResponse(
                                "dataset-a",
                                "tenant-a",
                                5,
                                0.2,
                                "documentid",
                                5,
                                4,
                                recall,
                                mrr,
                                p95,
                                avg,
                                at,
                                List.of());
        }
}
