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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagRetrievalEvalHistoryServiceTest {

        @Test
        void shouldRetainOnlyConfiguredMaxAndExposeTrend(@TempDir Path tempDir) {
                RagRuntimeConfig config = new RagRuntimeConfig();
                config.setRetrievalEvalHistoryPath(tempDir.resolve("eval-history.ndjson").toString());
                config.setRetrievalEvalHistoryMaxEvents(3);

                RagRetrievalEvalHistoryService service = new RagRetrievalEvalHistoryService(
                                config,
                                new ObjectMapper().findAndRegisterModules(),
                                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC")));

                service.append(run("r1", 0.2, 0.3, 80, 40, Instant.parse("2026-01-01T00:00:01Z")));
                service.append(run("r2", 0.4, 0.5, 70, 35, Instant.parse("2026-01-01T00:00:02Z")));
                service.append(run("r3", 0.6, 0.7, 60, 30, Instant.parse("2026-01-01T00:00:03Z")));
                service.append(run("r4", 0.8, 0.9, 50, 25, Instant.parse("2026-01-01T00:00:04Z")));

                List<RagRetrievalEvalRun> history = service.history("tenant-a", "dataset-a", 10);
                assertEquals(3, history.size());
                assertEquals(0.4, history.get(0).recallAtK(), 1e-9);
                assertEquals(0.8, history.get(2).recallAtK(), 1e-9);

                RagRetrievalEvalTrendResponse trend = service.trend("tenant-a", "dataset-a", 10);
                assertEquals(3, trend.runCount());
                assertNotNull(trend.latest());
                assertNotNull(trend.previous());
                assertEquals(0.2, trend.recallAtKDelta(), 1e-9);
                assertEquals(0.2, trend.mrrDelta(), 1e-9);
                assertEquals(-10.0, trend.latencyP95MsDelta(), 1e-9);
                assertEquals(-5.0, trend.latencyAvgMsDelta(), 1e-9);
        }

        @Test
        void shouldLoadPersistedHistoryOnRestart(@TempDir Path tempDir) {
                Path historyPath = tempDir.resolve("eval-history.ndjson");

                RagRuntimeConfig config = new RagRuntimeConfig();
                config.setRetrievalEvalHistoryPath(historyPath.toString());
                config.setRetrievalEvalHistoryMaxEvents(10);

                RagRetrievalEvalHistoryService writer = new RagRetrievalEvalHistoryService(
                                config,
                                new ObjectMapper().findAndRegisterModules(),
                                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC")));

                writer.append(run("r1", 0.5, 0.6, 42, 30, Instant.parse("2026-01-01T00:00:05Z")));
                writer.append(run("r2", 0.7, 0.8, 40, 28, Instant.parse("2026-01-01T00:00:06Z")));

                RagRetrievalEvalHistoryService reader = new RagRetrievalEvalHistoryService(
                                config,
                                new ObjectMapper().findAndRegisterModules(),
                                Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC")));

                List<RagRetrievalEvalRun> loaded = reader.history("tenant-a", "dataset-a", 10);
                assertEquals(2, loaded.size());
                assertEquals(0.5, loaded.get(0).recallAtK(), 1e-9);
                assertEquals(0.7, loaded.get(1).recallAtK(), 1e-9);
                assertTrue(loaded.stream().allMatch(run -> run.runId() != null && !run.runId().isBlank()));
        }

        private static RagRetrievalEvalResponse run(
                        String id,
                        double recall,
                        double mrr,
                        double p95,
                        double avg,
                        Instant evaluatedAt) {
                return new RagRetrievalEvalResponse(
                                "dataset-a",
                                "tenant-a",
                                5,
                                0.2,
                                "documentid",
                                10,
                                8,
                                recall,
                                mrr,
                                p95,
                                avg,
                                evaluatedAt,
                                List.of(new RagRetrievalEvalCaseResult(
                                                id,
                                                "query",
                                                List.of("doc-1"),
                                                List.of("doc-1"),
                                                recall,
                                                mrr,
                                                1)));
        }
}
