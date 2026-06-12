package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceJson;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgenticCommerceWayangPersistenceTransferAuditSinkTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void inMemorySinkRecordsLatestBoundedTrails() {
        InMemoryAgenticCommerceWayangPersistenceTransferAuditSink sink =
                new InMemoryAgenticCommerceWayangPersistenceTransferAuditSink(2);
        AgenticCommerceWayangPersistenceTransferAuditTrail first =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT, "ready", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail second =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail third =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY, "applied", true);

        sink.record((AgenticCommerceWayangPersistenceTransferAuditTrail) null);
        sink.record(first);
        sink.record(second);
        sink.record(third);

        assertThat(sink.trails()).containsExactly(second, third);
        assertThat(sink.latest()).contains(third);
        AgenticCommerceWayangPersistenceTransferAuditPage latestPage =
                sink.query(AgenticCommerceWayangPersistenceTransferAuditQuery.latest(1));
        assertThat(latestPage.trails()).containsExactly(third);
        assertThat(latestPage.totalTrailCount()).isEqualTo(2);
        assertThat(latestPage.returnedTrailCount()).isEqualTo(1);
        assertThat(latestPage.truncated()).isTrue();
        assertThat(map(latestPage.toMap().get("query"))).containsEntry("limit", 1);
        assertThat(sink.query(AgenticCommerceWayangPersistenceTransferAuditQuery.byType(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        10))
                .trails()).containsExactly(second);
        assertThat(sink.query(AgenticCommerceWayangPersistenceTransferAuditQuery.byOutcome(
                        AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_COMPLETE,
                        10))
                .trails()).containsExactly(second, third);
        assertThat(sink.query(AgenticCommerceWayangPersistenceTransferAuditQuery.byNextAction(
                        AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_STOP,
                        10))
                .trails()).containsExactly(second, third);
        sink.clear();
        assertThat(sink.trails()).isEmpty();
        assertThat(sink.latest()).isEmpty();
    }

    @Test
    void fileSystemStoreAppendsAuditTrailsAndQueriesBoundedMirror() throws Exception {
        Path journal = temporaryDirectory.resolve("audit/trails.jsonl");
        FileSystemAgenticCommerceWayangPersistenceTransferAuditStore store =
                new FileSystemAgenticCommerceWayangPersistenceTransferAuditStore(journal, 2);
        AgenticCommerceWayangPersistenceTransferAuditTrail first =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT, "ready", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail second =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail third =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY, "applied", true);

        store.record((AgenticCommerceWayangPersistenceTransferAuditTrail) null);
        store.record(first);
        store.record(second);
        store.record(third);

        assertThat(store.journalPath()).isEqualTo(journal);
        assertThat(store.maxTrails()).isEqualTo(2);
        assertThat(store.trails()).containsExactly(second, third);
        assertThat(store.latest()).contains(third);
        assertThat(store.query(AgenticCommerceWayangPersistenceTransferAuditQuery.byType(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        10))
                .trails()).containsExactly(second);
        assertThat(AgenticCommerceWayangPersistenceTransferAuditReader.forSink(store)
                .query(AgenticCommerceWayangPersistenceTransferAuditQuery.byOutcome(
                        AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_COMPLETE,
                        10))
                .trails()).containsExactly(second, third);

        assertThat(Files.exists(journal)).isTrue();
        List<String> lines = Files.readAllLines(journal, StandardCharsets.UTF_8);
        assertThat(lines).hasSize(2);
        Map<String, Object> firstLine = AgenticCommerceJson.readObject(lines.get(0));
        assertThat(firstLine)
                .containsEntry(
                        "journalFormat",
                        FileSystemAgenticCommerceWayangPersistenceTransferAuditStore.JOURNAL_FORMAT)
                .containsEntry(
                        "trailType",
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY)
                .containsEntry("trailStatus", "complete");
        assertThat(map(firstLine.get("trail")))
                .containsEntry(
                        "trailType",
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY)
                .containsEntry("trailStatus", "complete");
        assertThat(String.join("\n", lines))
                .contains(
                        "\"trailType\":\""
                                + AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY
                                + "\"",
                        "\"trailStatus\":\"complete\"",
                        "\"trailType\":\""
                                + AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY
                                + "\"",
                        "\"trailStatus\":\"applied\"",
                        "\"nextAction\":\"" + AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_STOP + "\"");

        long journalSize = Files.size(journal);
        store.record((AgenticCommerceWayangPersistenceTransferAuditTrail) null);
        assertThat(Files.size(journal)).isEqualTo(journalSize);
    }

    @Test
    void fileSystemStoreReloadsExistingJournalAndPrunesRetention() throws Exception {
        Path journal = temporaryDirectory.resolve("audit/reload.jsonl");
        FileSystemAgenticCommerceWayangPersistenceTransferAuditStore writer =
                new FileSystemAgenticCommerceWayangPersistenceTransferAuditStore(journal, 3);
        AgenticCommerceWayangPersistenceTransferAuditTrail first =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT, "ready", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail second =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true);
        AgenticCommerceWayangPersistenceTransferAuditTrail third =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY, "applied", true);

        writer.record(first);
        writer.record(second);
        writer.record(third);
        assertThat(Files.readAllLines(journal, StandardCharsets.UTF_8)).hasSize(3);

        FileSystemAgenticCommerceWayangPersistenceTransferAuditStore reloaded =
                new FileSystemAgenticCommerceWayangPersistenceTransferAuditStore(journal, 2);

        assertThat(reloaded.trails()).containsExactly(second, third);
        assertThat(reloaded.latest()).contains(third);
        assertThat(reloaded.query(AgenticCommerceWayangPersistenceTransferAuditQuery.byType(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        10))
                .trails()).containsExactly(second);
        assertThat(Files.readAllLines(journal, StandardCharsets.UTF_8)).hasSize(2);
        assertThat(String.join("\n", Files.readAllLines(journal, StandardCharsets.UTF_8)))
                .doesNotContain("\"trailStatus\":\"ready\"")
                .contains("\"trailStatus\":\"complete\"", "\"trailStatus\":\"applied\"");
    }

    @Test
    void compositeSinkFansOutBestEffort() {
        InMemoryAgenticCommerceWayangPersistenceTransferAuditSink primary =
                new InMemoryAgenticCommerceWayangPersistenceTransferAuditSink();
        InMemoryAgenticCommerceWayangPersistenceTransferAuditSink fallback =
                new InMemoryAgenticCommerceWayangPersistenceTransferAuditSink();
        AgenticCommerceWayangPersistenceTransferAuditSink composite =
                AgenticCommerceWayangPersistenceTransferAuditSink.composite(
                        Arrays.<AgenticCommerceWayangPersistenceTransferAuditSink>asList(
                                null,
                                trail -> {
                                    throw new IllegalStateException("audit target failed");
                                },
                                primary,
                                fallback));
        AgenticCommerceWayangPersistenceTransferAuditTrail trail =
                trail(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY, "complete", true);

        composite.record(trail);
        composite.record((AgenticCommerceWayangPersistenceTransferAuditTrail) null);

        assertThat(primary.trails()).containsExactly(trail);
        assertThat(fallback.trails()).containsExactly(trail);
        AgenticCommerceWayangPersistenceTransferAuditPage page =
                AgenticCommerceWayangPersistenceTransferAuditReader.forSink(composite)
                        .query(AgenticCommerceWayangPersistenceTransferAuditQuery.byType(
                                AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                                10));
        assertThat(page.trails()).containsExactly(trail);
        assertThat(page.trailTypes())
                .containsExactly(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY);
    }

    @Test
    void serviceOverloadsRecordTransferAuditTrails() {
        FileAgenticCommerceWayangPersistenceStore source =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("source"));
        AgenticCommerceWayangPersistenceContractHarness.roundTrip().run(source);
        AgenticCommerceWayangPersistenceService service = AgenticCommerceWayangPersistenceService.of(source);
        InMemoryAgenticCommerceWayangPersistenceTransferAuditSink sink =
                new InMemoryAgenticCommerceWayangPersistenceTransferAuditSink();

        FileAgenticCommerceWayangPersistenceStore preflightTarget =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("preflight-target"));
        AgenticCommerceWayangPersistenceTransferPreflightReport preflight =
                service.preflightTransferTo(preflightTarget, sink);

        assertThat(preflight.readyToApply()).isTrue();
        assertThat(sink.trails()).hasSize(1);
        assertThat(sink.latest().orElseThrow().trailType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT);
        assertThat(sink.latest().orElseThrow().summary().decision().nextAction())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_APPLY);

        FileAgenticCommerceWayangPersistenceStore transferTarget =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("transfer-target"));
        AgenticCommerceWayangPersistenceTransferReport transfer =
                service.transferTo(transferTarget, sink);

        assertThat(transfer.passed()).isTrue();
        assertThat(sink.trails()).hasSize(2);
        assertThat(sink.latest().orElseThrow().trailType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY);
        assertThat(sink.latest().orElseThrow().summary().outcomeStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_COMPLETE);

        FileAgenticCommerceWayangPersistenceStore blockedTarget =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("blocked-target"));
        blockedTarget.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("blocked-token"))
                .build());
        AgenticCommerceWayangPersistenceTransferApplyReport blockedApply =
                service.applyTransferTo(
                        blockedTarget,
                        AgenticCommerceWayangPersistenceTransferOptions.noOverwrite(),
                        sink);

        assertThat(blockedApply.transferAttempted()).isFalse();
        assertThat(sink.trails()).hasSize(3);
        assertThat(sink.latest().orElseThrow().trailType())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(sink.latest().orElseThrow().eventTypes())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(sink.latest().orElseThrow().summary().decision().nextAction())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_FORCE);

        FileAgenticCommerceWayangPersistenceStore forcedTarget =
                FileAgenticCommerceWayangPersistenceStore.at(temporaryDirectory.resolve("forced-target"));
        forcedTarget.saveRuntimeConfig(AgenticCommerceWayangRuntimeConfig.builder()
                .connectorConfig(AgenticCommerceConnectorConfig.bearer("forced-token"))
                .build());
        AgenticCommerceWayangPersistenceTransferApplyReport forcedApply =
                service.applyTransferTo(
                        forcedTarget,
                        AgenticCommerceWayangPersistenceTransferOptions.noOverwrite(),
                        true,
                        sink);

        assertThat(forcedApply.transferAttempted()).isTrue();
        assertThat(sink.trails()).hasSize(4);
        assertThat(sink.latest().orElseThrow().eventTypes())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_PREFLIGHT,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_COPY,
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY);
        assertThat(sink.latest().orElseThrow().summary().decision().decisionStatus())
                .isEqualTo(AgenticCommerceWayangPersistenceTransferAuditDecision.STATUS_FORCED_COMPLETE);

        AgenticCommerceWayangPersistenceTransferAuditReader reader =
                AgenticCommerceWayangPersistenceTransferAuditReader.forSink(sink);
        assertThat(reader.query(AgenticCommerceWayangPersistenceTransferAuditQuery.byType(
                        AgenticCommerceWayangPersistenceTransferAuditEvent.TYPE_TRANSFER_APPLY,
                        10))
                .trails()).hasSize(2);
        assertThat(reader.query(AgenticCommerceWayangPersistenceTransferAuditQuery.byNextAction(
                        AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_FORCE,
                        10))
                .trails()).hasSize(1);
        assertThat(reader.query(AgenticCommerceWayangPersistenceTransferAuditQuery.byOutcome(
                        AgenticCommerceWayangPersistenceTransferAuditSummary.OUTCOME_FORCED,
                        10))
                .trails()).hasSize(1);
        AgenticCommerceWayangPersistenceTransferAuditPage attentionPage =
                reader.query(AgenticCommerceWayangPersistenceTransferAuditQuery.requiringAttention(10));
        assertThat(attentionPage.trails()).hasSize(3);
        assertThat(attentionPage.nextActions())
                .containsExactly(
                        AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_APPLY,
                        AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_FORCE,
                        AgenticCommerceWayangPersistenceTransferAuditDecision.ACTION_STOP);
    }

    private static AgenticCommerceWayangPersistenceTransferAuditTrail trail(
            String eventType,
            String eventStatus,
            boolean successful) {
        AgenticCommerceWayangPersistenceTransferAuditEvent event =
                new AgenticCommerceWayangPersistenceTransferAuditEvent(
                        eventType,
                        eventStatus,
                        successful,
                        false,
                        false,
                        successful,
                        false,
                        0,
                        0,
                        0,
                        1,
                        1,
                        successful ? 1 : 0,
                        0,
                        0,
                        List.of(),
                        List.of(),
                        Map.of("eventStatus", eventStatus));
        return new AgenticCommerceWayangPersistenceTransferAuditTrail(
                eventType,
                List.of(event),
                Map.of("eventStatus", eventStatus));
    }

    private static Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return AgenticCommerceWayangMaps.copy((Map<?, ?>) value);
    }
}
