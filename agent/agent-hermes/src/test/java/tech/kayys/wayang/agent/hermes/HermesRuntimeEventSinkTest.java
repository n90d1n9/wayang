package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tech.kayys.wayang.agent.spi.AgentRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class HermesRuntimeEventSinkTest {

    @Test
    void compositeFansOutAndIsolatesFailingSinks() {
        List<HermesRuntimeEvent> first = new ArrayList<>();
        List<HermesRuntimeEvent> second = new ArrayList<>();
        HermesRuntimeEventSink sink = HermesRuntimeEventSink.composite(
                first::add,
                event -> {
                    throw new IllegalStateException("sink failed");
                },
                second::add);
        HermesRuntimeEvent event = HermesRuntimeEvent.requestPlanned(
                AgentRequest.builder().requestId("req-composite").prompt("plan").build(),
                null);

        sink.emit(event);

        assertThat(first).containsExactly(event);
        assertThat(second).containsExactly(event);
        assertThat(HermesRuntimeEventReader.forSink(sink).latest().events()).isEmpty();
    }

    @Test
    void fileSystemSinkAppendsRuntimeEventsAsJsonLines(@TempDir Path tempDir) throws Exception {
        Path journal = tempDir.resolve("journals/hermes-runtime.jsonl");
        FileSystemHermesRuntimeEventSink sink = new FileSystemHermesRuntimeEventSink(journal);

        sink.emit(HermesRuntimeEvent.requestPlanned(
                AgentRequest.builder().requestId("req-file").tenantId("tenant-a").prompt("plan").build(),
                null));
        sink.emit(HermesRuntimeEvent.responseFailed(
                AgentRequest.builder().requestId("req-file").tenantId("tenant-a").prompt("plan").build(),
                new IllegalStateException("boom"),
                42));

        List<String> lines = Files.readAllLines(journal);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0))
                .contains("\"requestId\":\"req-file\"")
                .contains("\"tenantId\":\"tenant-a\"")
                .contains("\"type\":\"request.planned\"");
        assertThat(lines.get(1))
                .contains("\"durationMs\":42")
                .contains("\"error\":\"boom\"")
                .contains("\"type\":\"response.failed\"");
        HermesRuntimeEventPage page = sink.query(HermesRuntimeEventQuery.forRequest("req-file", 10));
        assertThat(page.returnedEvents()).isEqualTo(2);
        assertThat(page.events()).extracting(HermesRuntimeEvent::type)
                .containsExactly(
                        HermesRuntimeEvent.TYPE_REQUEST_PLANNED,
                        HermesRuntimeEvent.TYPE_RESPONSE_FAILED);
        assertThat(sink.query(HermesRuntimeEventQuery.failures(10)).events())
                .extracting(HermesRuntimeEvent::type)
                .containsExactly(HermesRuntimeEvent.TYPE_RESPONSE_FAILED);
    }

    @Test
    void fileSystemSinkPrunesJournalToConfiguredCapacity(@TempDir Path tempDir) throws Exception {
        Path journal = tempDir.resolve("journals/hermes-runtime.jsonl");
        FileSystemHermesRuntimeEventSink sink = new FileSystemHermesRuntimeEventSink(journal, 2);

        sink.emit(event("req-1"));
        sink.emit(event("req-2"));
        sink.emit(event("req-3"));

        assertThat(Files.readAllLines(journal)).hasSize(2);
        HermesRuntimeEventPage page = sink.latest();
        assertThat(page.returnedEvents()).isEqualTo(2);
        assertThat(page.events()).extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-2", "req-3");
    }

    @Test
    void compositeReaderUsesFirstReadableHealthySink(@TempDir Path tempDir) {
        FileSystemHermesRuntimeEventSink fileSink =
                new FileSystemHermesRuntimeEventSink(tempDir.resolve("events.jsonl"));
        HermesRuntimeEvent event = event("req-readable");
        fileSink.emit(event);
        HermesRuntimeEventSink composite = HermesRuntimeEventSink.composite(
                emitted -> {
                    throw new IllegalStateException("write-only sink failed");
                },
                fileSink);

        HermesRuntimeEventPage page = HermesRuntimeEventReader.forSink(composite)
                .query(HermesRuntimeEventQuery.forType(HermesRuntimeEvent.TYPE_REQUEST_PLANNED, 5));

        assertThat(page.returnedEvents()).isEqualTo(1);
        assertThat(page.events()).containsExactly(event);
    }

    @Test
    void objectStorageSinkPersistsQueriesAndPrunesEvents() {
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        ObjectStorageHermesRuntimeEventSink sink =
                new ObjectStorageHermesRuntimeEventSink(storage, "/tenant-a/hermes-events", 2);

        sink.emit(event("req-object-1", "2026-06-03T00:00:00Z"));
        sink.emit(event("req-object-2", "2026-06-03T00:00:01Z"));
        sink.emit(event("req-object-3", "2026-06-03T00:00:02Z"));

        assertThat(storage.objects).hasSize(2);
        assertThat(storage.objects.keySet()).allMatch(key -> key.startsWith("tenant-a/hermes-events/"));
        assertThat(sink.latest().events()).extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-object-2", "req-object-3");
        assertThat(sink.query(HermesRuntimeEventQuery.forRequest("req-object-2", 10)).events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-object-2");
    }

    @Test
    void databaseSinkPersistsQueriesAndPrunesEvents() {
        InMemoryRuntimeEventDataSource dataSource = new InMemoryRuntimeEventDataSource();
        DatabaseHermesRuntimeEventSink sink =
                new DatabaseHermesRuntimeEventSink(dataSource, "tenant_a_runtime_events", true, 2);

        sink.emit(event("req-db-1", "2026-06-03T00:00:00Z"));
        sink.emit(event("req-db-2", "2026-06-03T00:00:01Z"));
        sink.emit(event("req-db-3", "2026-06-03T00:00:02Z"));

        assertThat(sink.tableName()).isEqualTo("tenant_a_runtime_events");
        assertThat(sink.maxEvents()).isEqualTo(2);
        assertThat(sink.eventCount()).isEqualTo(2);
        assertThat(sink.latest().events()).extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-db-2", "req-db-3");
        assertThat(sink.query(HermesRuntimeEventQuery.forRequest("req-db-2", 10)).events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-db-2");
        assertThat(sink.query(HermesRuntimeEventQuery.forTenant("tenant-a", 10)).matchedEvents())
                .isEqualTo(2);
    }

    @Test
    void resolverBuildsDatabaseRuntimeJournalFromDataSource() {
        InMemoryRuntimeEventDataSource dataSource = new InMemoryRuntimeEventDataSource();
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .runtimeEventJournalEnabled(true)
                .runtimeEventJournalStore("jdbc")
                .runtimeEventJournalJdbcTableName("producer_runtime_events")
                .runtimeEventJournalMaxEvents(10)
                .build();
        HermesRuntimeEventSink sink = HermesRuntimeEventSinkResolver.resolve(
                config,
                Optional.empty(),
                Optional.of(dataSource));

        sink.emit(event("req-producer-db", "2026-06-03T00:00:00Z"));

        assertThat(sink).isInstanceOf(DatabaseHermesRuntimeEventSink.class);
        assertThat(HermesRuntimeEventReader.forSink(sink)
                        .query(HermesRuntimeEventQuery.forRequest("req-producer-db", 10))
                        .events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-producer-db");
    }

    @Test
    void hybridJournalUsesDatabasePrimaryWithFileFallback(@TempDir Path tempDir) {
        InMemoryRuntimeEventDataSource dataSource = new InMemoryRuntimeEventDataSource();
        Path fallback = tempDir.resolve("hybrid/events.jsonl");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .runtimeEventJournalEnabled(true)
                .runtimeEventJournalStore("hybrid")
                .runtimeEventJournalPath(fallback.toString())
                .runtimeEventJournalMaxEvents(10)
                .build();
        HermesRuntimeEventSink sink = HermesRuntimeEventSinkResolver.resolve(
                config,
                Optional.empty(),
                Optional.of(dataSource));

        sink.emit(event("req-hybrid-db", "2026-06-03T00:00:00Z"));

        assertThat(dataSource.rows).hasSize(1);
        assertThat(Files.exists(fallback)).isTrue();
        assertThat(HermesRuntimeEventReader.forSink(sink).latest().events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-hybrid-db");
    }

    @Test
    void hybridJournalUsesObjectStoragePrimaryWithFileFallback(@TempDir Path tempDir) {
        InMemoryHermesObjectStorageService storage = new InMemoryHermesObjectStorageService();
        Path fallback = tempDir.resolve("hybrid-object/events.jsonl");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .runtimeEventJournalEnabled(true)
                .runtimeEventJournalStore("hybrid")
                .runtimeEventJournalPath(fallback.toString())
                .runtimeEventJournalObjectPrefix("tenant-a/hermes/hybrid-runtime-events")
                .runtimeEventJournalMaxEvents(10)
                .build();
        HermesRuntimeEventSink sink = HermesRuntimeEventSinkResolver.resolve(config, Optional.of(storage));

        sink.emit(event("req-hybrid-object", "2026-06-03T00:00:00Z"));

        assertThat(storage.objects)
                .hasSize(1)
                .allSatisfy((key, value) -> assertThat(key)
                        .startsWith("tenant-a/hermes/hybrid-runtime-events/")
                        .endsWith(".jsonl"));
        assertThat(Files.exists(fallback)).isTrue();
        assertThat(HermesRuntimeEventReader.forSink(sink).latest().events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-hybrid-object");
    }

    @Test
    void hybridJournalFallsBackToFileWhenPrimaryResourcesAreMissing(@TempDir Path tempDir) throws Exception {
        Path fallback = tempDir.resolve("hybrid-file/events.jsonl");
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .runtimeEventJournalEnabled(true)
                .runtimeEventJournalStore("hybrid")
                .runtimeEventJournalPath(fallback.toString())
                .runtimeEventJournalMaxEvents(10)
                .build();
        HermesRuntimeEventSink sink = HermesRuntimeEventSinkResolver.resolve(
                config,
                Optional.empty(),
                Optional.empty());

        sink.emit(event("req-hybrid-file", "2026-06-03T00:00:00Z"));

        List<String> lines = Files.readAllLines(fallback);
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0)).contains("\"requestId\":\"req-hybrid-file\"");
        assertThat(HermesRuntimeEventReader.forSink(sink).latest().events())
                .extracting(HermesRuntimeEvent::requestId)
                .containsExactly("req-hybrid-file");
    }

    @Test
    void resolverMetadataDescribesConfiguredRuntimeJournal() {
        HermesAgentModeConfig config = HermesAgentModeConfig.builder()
                .runtimeEventJournalEnabled(true)
                .runtimeEventJournalStore("hybrid")
                .runtimeEventJournalPath("logs/hermes-runtime.jsonl")
                .runtimeEventJournalObjectPrefix("objects/hermes-runtime")
                .runtimeEventJournalJdbcTableName("metadata_runtime_events")
                .runtimeEventJournalJdbcInitializeSchema(false)
                .runtimeEventJournalMaxEvents(64)
                .build();

        Map<String, Object> metadata = HermesRuntimeEventSinkResolver.metadata(config);

        assertThat(metadata)
                .containsEntry("enabled", true)
                .containsEntry("journalStore", "hybrid")
                .containsEntry("journalPath", "logs/hermes-runtime.jsonl")
                .containsEntry("journalObjectPrefix", "objects/hermes-runtime")
                .containsEntry("journalJdbcTableName", "metadata_runtime_events")
                .containsEntry("journalJdbcInitializeSchema", false)
                .containsEntry("maxEvents", 64)
                .containsEntry("fileFallback", true)
                .containsEntry("objectStorageCapable", true)
                .containsEntry("databaseCapable", true);
    }

    private static HermesRuntimeEvent event(String requestId) {
        return HermesRuntimeEvent.requestPlanned(
                AgentRequest.builder().requestId(requestId).prompt("plan").build(),
                null);
    }

    private static HermesRuntimeEvent event(String requestId, String occurredAt) {
        return new HermesRuntimeEvent(
                "",
                HermesRuntimeEvent.TYPE_REQUEST_PLANNED,
                requestId,
                "tenant-a",
                "",
                "",
                "planned",
                Instant.parse(occurredAt),
                Map.of("test", true));
    }

    private record RuntimeEventRow(
            long sequence,
            String recordId,
            String eventId,
            String type,
            String requestId,
            String tenantId,
            String sessionId,
            String userId,
            String outcome,
            String occurredAt,
            String eventJson) {
    }

    private static final class InMemoryRuntimeEventDataSource extends AbstractHermesJdbcDataSource {
        private final List<RuntimeEventRow> rows = new ArrayList<>();
        private long sequence;

        @Override
        protected int executeUpdate(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (normalizedSql.startsWith("INSERT")) {
                rows.add(new RuntimeEventRow(
                        ++sequence,
                        (String) parameters.get(1),
                        (String) parameters.get(2),
                        (String) parameters.get(3),
                        (String) parameters.get(4),
                        (String) parameters.get(5),
                        (String) parameters.get(6),
                        (String) parameters.get(7),
                        (String) parameters.get(8),
                        (String) parameters.get(9),
                        (String) parameters.get(10)));
                return 1;
            }
            if (normalizedSql.startsWith("DELETE")) {
                String recordId = (String) parameters.get(1);
                int before = rows.size();
                rows.removeIf(row -> row.recordId().equals(recordId));
                return before - rows.size();
            }
            return 0;
        }

        @Override
        protected List<List<Object>> select(String sql, Map<Integer, Object> parameters) {
            String normalizedSql = sql.toUpperCase(Locale.ROOT);
            if (normalizedSql.contains("COUNT(*)")) {
                return List.of(List.of(rows.size()));
            }
            if (normalizedSql.startsWith("SELECT EVENT_JSON")) {
                return rows.stream()
                        .sorted(runtimeEventAscendingOrdering())
                        .map(row -> List.<Object>of(row.eventJson()))
                        .toList();
            }
            if (normalizedSql.startsWith("SELECT RECORD_ID")) {
                return rows.stream()
                        .sorted(runtimeEventDescendingOrdering())
                        .map(row -> List.<Object>of(row.recordId()))
                        .toList();
            }
            return List.of();
        }

        private Comparator<RuntimeEventRow> runtimeEventAscendingOrdering() {
            return Comparator
                    .comparing(RuntimeEventRow::occurredAt)
                    .thenComparing(RuntimeEventRow::sequence);
        }

        private Comparator<RuntimeEventRow> runtimeEventDescendingOrdering() {
            return runtimeEventAscendingOrdering().reversed();
        }
    }
}
