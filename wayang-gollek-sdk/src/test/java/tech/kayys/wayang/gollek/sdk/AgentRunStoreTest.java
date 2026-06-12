package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRunStoreTest {

    @Test
    void storesStatusSnapshotsFromRunResults() {
        AgentRunStore store = AgentRunStore.memory();
        AgentRunResult result = new AgentRunResult(
                "run-1",
                "done",
                true,
                "strategy-a",
                List.of("step-a", "step-b"),
                Map.of("tenant", "tenant-a"));

        AgentRunStatus saved = store.save(result);
        AgentRunStatus found = store.status(" run-1 ");

        assertThat(saved.known()).isTrue();
        assertThat(found).isEqualTo(saved);
        assertThat(found.handle()).isEqualTo(result.handle());
        assertThat(found.message()).isEqualTo("Run state is completed.");
        assertThat(found.metadata())
                .containsEntry("tenant", "tenant-a")
                .containsEntry("successful", true)
                .containsEntry("stepCount", 2);
        assertThat(store.history().runs()).containsExactly(saved);
        assertThat(store.history().totalRuns()).isEqualTo(1);
        assertThat(store.timeline("run-1").events())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.sequence()).isEqualTo(1);
                    assertThat(event.type()).isEqualTo("run.completed");
                    assertThat(event.state()).isEqualTo(AgentRunState.COMPLETED);
                    assertThat(event.metadata()).containsEntry("tenant", "tenant-a");
                });
    }

    @Test
    void filtersHistoryByStateAndLimit() {
        AgentRunStore store = AgentRunStore.memory();
        AgentRunStatus firstCompleted = store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-1", "strategy-a"),
                true,
                "done",
                Map.of()));
        store.save(new AgentRunStatus(
                new AgentRunHandle("run-2", AgentRunState.RUNNING, "strategy-a"),
                true,
                "running",
                Map.of()));
        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-3", "strategy-a"),
                true,
                "done",
                Map.of()));

        AgentRunHistory history = store.history(new AgentRunHistoryQuery(AgentRunState.COMPLETED, 1));

        assertThat(history.query().state()).isEqualTo(AgentRunState.COMPLETED);
        assertThat(history.query().limit()).isEqualTo(1);
        assertThat(history.totalRuns()).isEqualTo(2);
        assertThat(history.returnedRuns()).isEqualTo(1);
        assertThat(history.outcome()).isEqualTo(AgentRunOutcomes.TERMINAL);
        assertThat(history.pageSize()).isEqualTo(1);
        assertThat(history.offset()).isZero();
        assertThat(history.windowStart()).isEqualTo(1);
        assertThat(history.windowEnd()).isEqualTo(1);
        assertThat(history.previousOffset()).isZero();
        assertThat(history.hasPrevious()).isFalse();
        assertThat(history.nextOffset()).isEqualTo(1);
        assertThat(history.hasMore()).isTrue();
        assertThat(history.truncated()).isTrue();
        assertThat(history.stateCounts()).containsEntry("completed", 1);
        assertThat(history.strategyCounts()).containsEntry("strategy-a", 1);
        assertThat(history.stateSummaries())
                .containsExactly(new AgentRunHistoryFacetSummary("completed", 1));
        assertThat(history.strategySummaries())
                .containsExactly(new AgentRunHistoryFacetSummary("strategy-a", 1));
        assertThat(history.summary()).isEqualTo(new AgentRunHistorySummary(
                2,
                1,
                Map.of("completed", 1),
                Map.of(),
                Map.of("strategy-a", 1)));
        assertThat(history.runs()).containsExactly(firstCompleted);
    }

    @Test
    void paginatesHistoryByOffsetAndLimit() {
        AgentRunStore store = AgentRunStore.memory();
        AgentRunStatus first = store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-page-1", "strategy-a"),
                true,
                "done",
                Map.of()));
        AgentRunStatus second = store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-page-2", "strategy-a"),
                true,
                "done",
                Map.of()));
        AgentRunStatus third = store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-page-3", "strategy-a"),
                true,
                "done",
                Map.of()));

        AgentRunHistory middle = store.history(new AgentRunHistoryQuery(
                AgentRunState.COMPLETED,
                1,
                "",
                "",
                "",
                1));
        AgentRunHistory tail = store.history(new AgentRunHistoryQuery(
                AgentRunState.COMPLETED,
                2,
                "",
                "",
                "",
                2));
        AgentRunHistory pastEnd = store.history(new AgentRunHistoryQuery(
                AgentRunState.COMPLETED,
                2,
                "",
                "",
                "",
                10));

        assertThat(first.handle().runId()).isEqualTo("run-page-1");
        assertThat(middle.totalRuns()).isEqualTo(3);
        assertThat(middle.page()).isEqualTo(new AgentRunHistoryPage(3, 1, 1, 1));
        assertThat(middle.returnedRuns()).isEqualTo(1);
        assertThat(middle.pageSize()).isEqualTo(1);
        assertThat(middle.offset()).isEqualTo(1);
        assertThat(middle.windowStart()).isEqualTo(2);
        assertThat(middle.windowEnd()).isEqualTo(2);
        assertThat(middle.previousOffset()).isZero();
        assertThat(middle.hasPrevious()).isTrue();
        assertThat(middle.nextOffset()).isEqualTo(2);
        assertThat(middle.hasMore()).isTrue();
        assertThat(middle.truncated()).isTrue();
        assertThat(middle.runs()).containsExactly(second);
        assertThat(tail.totalRuns()).isEqualTo(3);
        assertThat(tail.returnedRuns()).isEqualTo(1);
        assertThat(tail.pageSize()).isEqualTo(2);
        assertThat(tail.windowStart()).isEqualTo(3);
        assertThat(tail.windowEnd()).isEqualTo(3);
        assertThat(tail.previousOffset()).isZero();
        assertThat(tail.hasPrevious()).isTrue();
        assertThat(tail.nextOffset()).isEqualTo(3);
        assertThat(tail.hasMore()).isFalse();
        assertThat(tail.truncated()).isTrue();
        assertThat(tail.runs()).containsExactly(third);
        assertThat(pastEnd.totalRuns()).isEqualTo(3);
        assertThat(pastEnd.returnedRuns()).isZero();
        assertThat(pastEnd.outcome()).isEqualTo(AgentRunOutcomes.EMPTY);
        assertThat(pastEnd.windowStart()).isZero();
        assertThat(pastEnd.windowEnd()).isZero();
        assertThat(pastEnd.previousOffset()).isEqualTo(2);
        assertThat(pastEnd.hasPrevious()).isTrue();
        assertThat(pastEnd.nextOffset()).isEqualTo(3);
        assertThat(pastEnd.hasMore()).isFalse();
        assertThat(pastEnd.truncated()).isTrue();
        assertThat(pastEnd.message()).isEqualTo("No run statuses are recorded for this page.");
        assertThat(pastEnd.runs()).isEmpty();
    }

    @Test
    void filtersHistoryByTenantSessionAndSurfaceMetadata() {
        AgentRunStore store = AgentRunStore.memory();
        AgentRunStatus match = store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-filter-1", "strategy-a"),
                true,
                "done",
                Map.of(
                        "tenant", "tenant-a",
                        "session", "session-a",
                        "surface", "assistant-agent")));
        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-filter-2", "strategy-a"),
                true,
                "done",
                Map.of(
                        "tenant", "tenant-b",
                        "session", "session-b",
                        "surface", "coding-agent")));

        AgentRunHistory history = store.history(new AgentRunHistoryQuery(
                AgentRunState.COMPLETED,
                10,
                "tenant-a",
                "session-a",
                "assistant-agent"));

        assertThat(history.query().filtered()).isTrue();
        assertThat(history.totalRuns()).isEqualTo(1);
        assertThat(history.returnedRuns()).isEqualTo(1);
        assertThat(history.surfaceCounts()).containsEntry("assistant-agent", 1);
        assertThat(history.runs()).containsExactly(match);
    }

    @Test
    void filtersHistoryByProfileMetadata() {
        AgentRunStore store = AgentRunStore.memory();
        AgentRunStatus profileIdMatch = store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-profile-1", "strategy-a"),
                true,
                "done",
                Map.of("profileId", "low-code-agent")));
        AgentRunStatus wayangProfileMatch = store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-profile-2", "strategy-a"),
                true,
                "done",
                Map.of("wayang.profile", "low-code-agent")));
        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-profile-3", "strategy-a"),
                true,
                "done",
                Map.of("profileId", "openclaw-agent")));

        AgentRunHistory history = store.history(new AgentRunHistoryQuery(
                AgentRunState.COMPLETED,
                10,
                "",
                "",
                "",
                "low-code-agent",
                0));

        assertThat(history.query().profileId()).isEqualTo("low-code-agent");
        assertThat(history.query().filtered()).isTrue();
        assertThat(history.totalRuns()).isEqualTo(2);
        assertThat(history.returnedRuns()).isEqualTo(2);
        assertThat(history.profileCounts()).containsEntry("low-code-agent", 2);
        assertThat(history.summary().profileCounts()).containsEntry("low-code-agent", 2);
        assertThat(history.profileSummaries())
                .containsExactly(new AgentRunHistoryFacetSummary("low-code-agent", 2));
        assertThat(history.summary().profileSummaries())
                .containsExactly(new AgentRunHistoryFacetSummary("low-code-agent", 2));
        assertThat(history.runs()).containsExactly(profileIdMatch, wayangProfileMatch);
    }

    @Test
    void persistsRunStatusesInFileStore(@TempDir Path directory) {
        Path storePath = directory.resolve("wayang-runs.properties");
        AgentRunStore writer = AgentRunStore.file(storePath.toString());
        AgentRunStatus saved = writer.save(new AgentRunStatus(
                AgentRunHandle.completed("run-file-1", "strategy-a"),
                true,
                "done",
                Map.of("tenant", "tenant-a")));

        AgentRunStore reader = AgentRunStore.file(storePath.toString());
        AgentRunStatus found = reader.status("run-file-1");

        assertThat(found.handle()).isEqualTo(saved.handle());
        assertThat(found.known()).isTrue();
        assertThat(found.message()).isEqualTo("done");
        assertThat(found.metadata()).containsEntry("tenant", "tenant-a");
        assertThat(reader.history().runs()).containsExactly(found);
        assertThat(reader.timeline("run-file-1").events())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.runId()).isEqualTo("run-file-1");
                    assertThat(event.state()).isEqualTo(AgentRunState.COMPLETED);
                    assertThat(event.message()).isEqualTo("done");
                    assertThat(event.metadata()).containsEntry("tenant", "tenant-a");
                });
    }

    @Test
    void fileStoreWritesSnapshotsAtomicallyAndCleansTemporaryFiles(@TempDir Path directory) throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        AgentRunStore writer = AgentRunStore.file(storePath.toString());
        writer.save(new AgentRunStatus(
                new AgentRunHandle("run-file-atomic-1", AgentRunState.RUNNING, "strategy-a"),
                true,
                "running",
                Map.of("tenant", "tenant-a")));
        AgentRunStatus completed = writer.save(new AgentRunStatus(
                AgentRunHandle.completed("run-file-atomic-1", "strategy-a"),
                true,
                "done",
                Map.of("tenant", "tenant-a")));
        writer.appendEvent(new AgentRunEvent(
                "run-file-atomic-1",
                99,
                "run.audit",
                AgentRunState.COMPLETED,
                "audit",
                Map.of("phase", "after")));

        AgentRunStore reader = AgentRunStore.file(storePath.toString());

        assertThat(Files.exists(storePath)).isTrue();
        assertThat(Files.readString(storePath)).contains("version=1").contains("event.count=");
        assertThat(temporaryStoreFiles(directory)).isEmpty();
        assertThat(reader.status("run-file-atomic-1").handle()).isEqualTo(completed.handle());
        assertThat(reader.timeline("run-file-atomic-1").events())
                .extracting(AgentRunEvent::type)
                .containsExactly("run.running", "run.completed", "run.audit");
    }

    @Test
    void memoryStoreGeneratesStatusEventsAfterHighestExistingSequence() {
        AgentRunStore store = AgentRunStore.memory();
        store.save(new AgentRunStatus(
                new AgentRunHandle("run-memory-sequence-1", AgentRunState.RUNNING, "strategy-a"),
                true,
                "running",
                Map.of()));
        store.appendEvent(new AgentRunEvent(
                "run-memory-sequence-1",
                99,
                "run.audit",
                AgentRunState.RUNNING,
                "audit",
                Map.of()));

        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-memory-sequence-1", "strategy-a"),
                true,
                "done",
                Map.of()));

        assertThat(store.timeline("run-memory-sequence-1").events())
                .extracting(AgentRunEvent::sequence)
                .containsExactly(1L, 99L, 100L);
    }

    @Test
    void fileStoreGeneratesStatusEventsAfterHighestExistingSequence(@TempDir Path directory) {
        Path storePath = directory.resolve("wayang-runs.properties");
        AgentRunStore writer = AgentRunStore.file(storePath.toString());
        writer.save(new AgentRunStatus(
                new AgentRunHandle("run-file-sequence-1", AgentRunState.RUNNING, "strategy-a"),
                true,
                "running",
                Map.of()));
        writer.appendEvent(new AgentRunEvent(
                "run-file-sequence-1",
                99,
                "run.audit",
                AgentRunState.RUNNING,
                "audit",
                Map.of()));

        writer.save(new AgentRunStatus(
                AgentRunHandle.completed("run-file-sequence-1", "strategy-a"),
                true,
                "done",
                Map.of()));

        AgentRunStore reader = AgentRunStore.file(storePath.toString());
        assertThat(reader.timeline("run-file-sequence-1").events())
                .extracting(AgentRunEvent::sequence)
                .containsExactly(1L, 99L, 100L);
    }

    @Test
    void fileStoreSerializesSeparateInstancesThroughLockFile(@TempDir Path directory) throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        AgentRunStore first = AgentRunStore.file(storePath.toString());
        AgentRunStore second = AgentRunStore.file(storePath.toString());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstWrite = executor.submit(() -> {
                await(start);
                first.save(new AgentRunStatus(
                        AgentRunHandle.completed("run-file-lock-1", "strategy-a"),
                        true,
                        "first",
                        Map.of()));
            });
            Future<?> secondWrite = executor.submit(() -> {
                await(start);
                second.save(new AgentRunStatus(
                        AgentRunHandle.completed("run-file-lock-2", "strategy-a"),
                        true,
                        "second",
                        Map.of()));
            });

            start.countDown();
            firstWrite.get();
            secondWrite.get();
        } finally {
            executor.shutdownNow();
        }

        AgentRunStore reader = AgentRunStore.file(storePath.toString());
        assertThat(reader.history().runs())
                .extracting(status -> status.handle().runId())
                .containsExactlyInAnyOrder("run-file-lock-1", "run-file-lock-2");
        assertThat(Files.exists(directory.resolve("wayang-runs.properties.lock"))).isTrue();
    }

    @Test
    void fileStoreAppliesRunRetentionPolicy(@TempDir Path directory) {
        Path storePath = directory.resolve("wayang-runs.properties");
        AgentRunStore store = AgentRunStore.file(
                storePath.toString(),
                AgentRunStoreRetentionPolicy.of(2, 0));
        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-file-retention-1", "strategy-a"),
                true,
                "first",
                Map.of()));
        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-file-retention-2", "strategy-a"),
                true,
                "second",
                Map.of()));
        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-file-retention-3", "strategy-a"),
                true,
                "third",
                Map.of()));

        AgentRunStore reader = AgentRunStore.file(storePath.toString());

        assertThat(reader.history().runs())
                .extracting(status -> status.handle().runId())
                .containsExactly("run-file-retention-2", "run-file-retention-3");
        assertThat(reader.status("run-file-retention-1").known()).isFalse();
        assertThat(reader.timeline("run-file-retention-1").empty()).isTrue();
    }

    @Test
    void fileStoreAppliesEventRetentionPolicy(@TempDir Path directory) {
        Path storePath = directory.resolve("wayang-runs.properties");
        AgentRunStore store = AgentRunStore.file(
                storePath.toString(),
                AgentRunStoreRetentionPolicy.of(0, 2));
        store.save(new AgentRunStatus(
                new AgentRunHandle("run-file-event-retention-1", AgentRunState.RUNNING, "strategy-a"),
                true,
                "running",
                Map.of()));
        store.save(new AgentRunStatus(
                new AgentRunHandle("run-file-event-retention-1", AgentRunState.WAITING_FOR_APPROVAL, "strategy-a"),
                true,
                "waiting",
                Map.of()));
        store.appendEvent(new AgentRunEvent(
                "run-file-event-retention-1",
                99,
                "run.audit",
                AgentRunState.RUNNING,
                "audit",
                Map.of()));
        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-file-event-retention-1", "strategy-a"),
                true,
                "done",
                Map.of()));

        AgentRunStore reader = AgentRunStore.file(storePath.toString());

        assertThat(reader.timeline("run-file-event-retention-1").events())
                .extracting(AgentRunEvent::sequence)
                .containsExactly(99L, 100L);
        assertThat(reader.timeline("run-file-event-retention-1").events())
                .extracting(AgentRunEvent::type)
                .containsExactly("run.audit", "run.completed");
    }

    @Test
    void fileStoreCountsEventOnlyRunsForRetention(@TempDir Path directory) {
        Path storePath = directory.resolve("wayang-runs.properties");
        AgentRunStore store = AgentRunStore.file(
                storePath.toString(),
                AgentRunStoreRetentionPolicy.of(1, 0));
        store.appendEvent(new AgentRunEvent(
                "run-file-event-only-retention-1",
                1,
                "run.audit",
                AgentRunState.RUNNING,
                "first",
                Map.of()));
        store.appendEvent(new AgentRunEvent(
                "run-file-event-only-retention-2",
                1,
                "run.audit",
                AgentRunState.RUNNING,
                "second",
                Map.of()));

        AgentRunStore reader = AgentRunStore.file(storePath.toString());

        assertThat(reader.timeline("run-file-event-only-retention-1").empty()).isTrue();
        assertThat(reader.timeline("run-file-event-only-retention-2").events())
                .singleElement()
                .satisfies(event -> assertThat(event.message()).isEqualTo("second"));
    }

    @Test
    void memoryStoreReportsDiagnostics() {
        AgentRunStore store = AgentRunStore.memory();
        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-memory-diagnostics-1", "strategy-a"),
                true,
                "done",
                Map.of()));
        store.appendEvent(new AgentRunEvent(
                "run-memory-diagnostics-event-only-1",
                1,
                "run.audit",
                AgentRunState.RUNNING,
                "event-only",
                Map.of()));

        AgentRunStoreDiagnostics diagnostics = store.diagnostics();

        assertThat(diagnostics.backend()).isEqualTo("memory");
        assertThat(diagnostics.persistent()).isFalse();
        assertThat(diagnostics.runCount()).isEqualTo(2);
        assertThat(diagnostics.statusCount()).isEqualTo(1);
        assertThat(diagnostics.eventCount()).isEqualTo(2);
        assertThat(diagnostics.retentionPolicy()).isEqualTo(AgentRunStoreRetentionPolicy.unlimited());
        assertThat(diagnostics.retentionAssessment().pruned()).isFalse();
        assertThat(diagnostics.retentionAssessment().retainedRunIds())
                .containsExactly("run-memory-diagnostics-1", "run-memory-diagnostics-event-only-1");
        assertThat(diagnostics.toMap())
                .containsEntry("backend", "memory")
                .containsEntry("persistent", false)
                .containsEntry("runCount", 2)
                .containsEntry("statusCount", 1)
                .containsEntry("eventCount", 2);
    }

    @Test
    void memoryStoreVerificationPasses() {
        AgentRunStore store = AgentRunStore.memory();

        AgentRunStoreVerification verification = store.verification();

        assertThat(verification.passed()).isTrue();
        assertThat(verification.exitCode()).isZero();
        assertThat(verification.issueCount()).isZero();
        assertThat(verification.diagnostics().backend()).isEqualTo("memory");
        assertThat(verification.toMap())
                .containsEntry("passed", true)
                .containsEntry("exitCode", 0)
                .containsEntry("issueCount", 0);
    }

    @Test
    void memoryStoreCompactionPreviewIsClean() {
        AgentRunStore store = AgentRunStore.memory();

        AgentRunStoreCompactionPreview preview = store.compactionPreview();

        assertThat(preview.dryRun()).isTrue();
        assertThat(preview.previewable()).isTrue();
        assertThat(preview.compactionNeeded()).isFalse();
        assertThat(preview.exitCode()).isZero();
        assertThat(preview.warningCount()).isZero();
        assertThat(preview.backupRetention().prunedBackupCount()).isZero();
        assertThat(preview.toMap())
                .containsEntry("dryRun", true)
                .containsEntry("previewable", true)
                .containsEntry("compactionNeeded", false);
    }

    @Test
    void verificationPolicyCanFailWarningOnlyReports() {
        AgentRunStoreVerification verification = new AgentRunStoreVerification(
                AgentRunStore.memory().diagnostics(),
                List.of(AgentRunStoreVerificationIssue.warning(
                        "retention.would-prune",
                        "snapshot exceeds retention")));

        assertThat(verification.passed()).isTrue();
        assertThat(verification.exitCode()).isZero();
        assertThat(verification.passed(AgentRunStoreVerificationPolicy.strict())).isFalse();
        assertThat(verification.exitCode(AgentRunStoreVerificationPolicy.strict())).isEqualTo(1);
        assertThat(verification.toMap(AgentRunStoreVerificationPolicy.strict()))
                .containsEntry("passed", false)
                .containsEntry("exitCode", 1)
                .containsEntry("warningCount", 1);
        Map<?, ?> policy = (Map<?, ?>) verification.toMap(AgentRunStoreVerificationPolicy.strict()).get("policy");
        assertThat(policy.get("mode")).isEqualTo("strict");
        assertThat(policy.get("failOnWarnings")).isEqualTo(true);
    }

    @Test
    void verificationPolicyParsesOperatorAliases() {
        assertThat(AgentRunStoreVerificationPolicy.fromMode("").mode()).isEqualTo("lenient");
        assertThat(AgentRunStoreVerificationPolicy.fromMode("default").mode()).isEqualTo("lenient");
        assertThat(AgentRunStoreVerificationPolicy.fromMode("warnings-as-errors").mode()).isEqualTo("strict");
        assertThat(AgentRunStoreVerificationPolicy.fromMap(Map.of("failOnWarnings", "true")).mode())
                .isEqualTo("strict");

        assertThatThrownBy(() -> AgentRunStoreVerificationPolicy.fromMode("surprising"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported run-store verification policy");
    }

    @Test
    void fileStoreReportsDiagnostics(@TempDir Path directory) {
        Path storePath = directory.resolve("wayang-runs.properties");
        AgentRunStore store = AgentRunStore.file(
                storePath.toString(),
                AgentRunStoreRetentionPolicy.of(10, 10));
        store.save(new AgentRunStatus(
                new AgentRunHandle("run-file-diagnostics-1", AgentRunState.RUNNING, "strategy-a"),
                true,
                "running",
                Map.of()));
        store.appendEvent(new AgentRunEvent(
                "run-file-diagnostics-1",
                99,
                "run.audit",
                AgentRunState.RUNNING,
                "audit",
                Map.of()));

        AgentRunStoreDiagnostics diagnostics = store.diagnostics();

        assertThat(diagnostics.backend()).isEqualTo("file");
        assertThat(diagnostics.persistent()).isTrue();
        assertThat(diagnostics.path()).isEqualTo(storePath.toAbsolutePath().normalize().toString());
        assertThat(diagnostics.lockPath()).isEqualTo(directory
                .resolve("wayang-runs.properties.lock")
                .toAbsolutePath()
                .normalize()
                .toString());
        assertThat(diagnostics.snapshotPresent()).isTrue();
        assertThat(diagnostics.lockPresent()).isTrue();
        assertThat(diagnostics.snapshotVersion()).isEqualTo("1");
        assertThat(diagnostics.unsupportedSnapshotVersion()).isFalse();
        assertThat(diagnostics.runCount()).isEqualTo(1);
        assertThat(diagnostics.statusCount()).isEqualTo(1);
        assertThat(diagnostics.eventCount()).isEqualTo(2);
        assertThat(diagnostics.retentionPolicy()).isEqualTo(AgentRunStoreRetentionPolicy.of(10, 10));
        assertThat(diagnostics.retentionAssessment().retainedEvents()).isEqualTo(2);
        assertThat(diagnostics.backupRetentionPolicy()).isEqualTo(AgentRunStoreBackupRetentionPolicy.defaults());
        assertThat(diagnostics.backupInventory().backupCount()).isZero();
        assertThat(diagnostics.toMap().toString())
                .contains("backend=file")
                .contains("snapshotVersion=1")
                .contains("retentionAssessment=")
                .contains("backupInventory=");
    }

    @Test
    void fileStoreCompactionPreviewReportsRetentionWithoutMutation(@TempDir Path directory) throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        writeRetentionWarningSnapshot(storePath, "run-compaction-preview-1");
        AgentRunStore store = AgentRunStore.file(
                storePath.toString(),
                AgentRunStoreRetentionPolicy.of(0, 1));

        AgentRunStoreCompactionPreview preview = store.compactionPreview();

        assertThat(preview.previewable()).isTrue();
        assertThat(preview.compactionNeeded()).isTrue();
        assertThat(preview.exitCode()).isZero();
        assertThat(preview.warningCount()).isEqualTo(1);
        assertThat(preview.retentionAssessment().prunedEvents()).isEqualTo(1);
        assertThat(preview.backupRetention().prunedBackupCount()).isZero();
        assertThat(preview.message()).contains("would prune 0 runs, 0 statuses, and 1 events");
        assertThat(Files.readString(storePath)).contains("event.count=2");
        assertThat(quarantinedStoreFiles(directory)).isEmpty();
    }

    @Test
    void fileStoreCompactionPreviewReportsBackupRetentionWithoutMutation(@TempDir Path directory)
            throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        writeRetentionWarningSnapshot(storePath, "run-compaction-preview-backup-1");
        Path firstBackup = writeCompactionBackup(storePath, "first");
        Path secondBackup = writeCompactionBackup(storePath, "second");
        AgentRunStore store = AgentRunStore.file(
                storePath.toString(),
                AgentRunStoreRetentionPolicy.of(0, 1),
                AgentRunStoreBackupRetentionPolicy.of(1));

        AgentRunStoreCompactionPreview preview = store.compactionPreview();

        assertThat(preview.compactionNeeded()).isTrue();
        assertThat(preview.warningCount()).isEqualTo(2);
        assertThat(preview.backupRetention().policy())
                .isEqualTo(AgentRunStoreBackupRetentionPolicy.of(1));
        assertThat(preview.backupRetention().retainedBackupCount()).isEqualTo(1);
        assertThat(preview.backupRetention().prunedBackupCount()).isEqualTo(1);
        assertThat(preview.backupRetention().prunedBackupPaths()).hasSize(1);
        assertThat(backupStoreFiles(directory)).containsExactlyInAnyOrder(firstBackup, secondBackup);
    }

    @Test
    void fileStoreCompactionAppliesRetentionUnderLock(@TempDir Path directory) throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        writeRetentionWarningSnapshot(storePath, "run-compaction-apply-1");
        AgentRunStore store = AgentRunStore.file(
                storePath.toString(),
                AgentRunStoreRetentionPolicy.of(0, 1));

        AgentRunStoreCompactionResult result = store.compact();

        assertThat(result.successful()).isTrue();
        assertThat(result.applied()).isTrue();
        assertThat(result.compacted()).isTrue();
        assertThat(result.backupCreated()).isTrue();
        assertThat(result.backupPath()).contains(".compaction-").endsWith(".properties");
        assertThat(result.backupRetention().policy())
                .isEqualTo(AgentRunStoreBackupRetentionPolicy.defaults());
        assertThat(result.backupRetention().retainedBackupCount()).isEqualTo(1);
        assertThat(result.backupRetention().prunedBackupCount()).isZero();
        assertThat(result.exitCode()).isZero();
        assertThat(result.warningCount()).isEqualTo(1);
        assertThat(result.beforeDiagnostics().eventCount()).isEqualTo(2);
        assertThat(result.afterDiagnostics().eventCount()).isEqualTo(1);
        assertThat(result.afterDiagnostics().backupInventory().backupCount()).isEqualTo(1);
        assertThat(result.afterDiagnostics().backupInventory().latestBackupPath()).isEqualTo(result.backupPath());
        assertThat(result.retentionAssessment().prunedEvents()).isEqualTo(1);
        assertThat(result.message()).contains("pruned 0 runs, 0 statuses, and 1 events");
        assertThat(Files.readString(storePath))
                .contains("event.count=1")
                .doesNotContain("event.1.runId=");
        Path backupPath = Path.of(result.backupPath());
        assertThat(Files.exists(backupPath)).isTrue();
        assertThat(Files.readString(backupPath)).contains("event.count=2");
        assertThat(backupStoreFiles(directory)).containsExactly(backupPath);
        assertThat(quarantinedStoreFiles(directory)).isEmpty();
    }

    @Test
    void fileStoreCompactionPrunesOldBackupsWithConfiguredRetention(@TempDir Path directory) throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        AgentRunStore store = AgentRunStore.file(
                storePath.toString(),
                AgentRunStoreRetentionPolicy.of(0, 1),
                AgentRunStoreBackupRetentionPolicy.of(1));
        writeRetentionWarningSnapshot(storePath, "run-compaction-backup-1");

        AgentRunStoreCompactionResult first = store.compact();
        Path firstBackup = Path.of(first.backupPath());
        assertThat(firstBackup).exists();
        writeRetentionWarningSnapshot(storePath, "run-compaction-backup-2");
        AgentRunStoreCompactionResult second = store.compact();
        Path secondBackup = Path.of(second.backupPath());

        assertThat(secondBackup).exists();
        assertThat(firstBackup).doesNotExist();
        assertThat(second.backupRetention().policy())
                .isEqualTo(AgentRunStoreBackupRetentionPolicy.of(1));
        assertThat(second.backupRetention().retainedBackupCount()).isEqualTo(1);
        assertThat(second.backupRetention().prunedBackupCount()).isEqualTo(1);
        assertThat(second.backupRetention().prunedBackupPaths()).contains(firstBackup.toString());
        assertThat(backupStoreFiles(directory)).containsExactly(secondBackup);
    }

    @Test
    void fileStoreCompactionReportsIncompleteBackupPruning(@TempDir Path directory) throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        Path stuckBackup = writeStuckCompactionBackup(storePath, "stuck");
        writeRetentionWarningSnapshot(storePath, "run-compaction-stuck-backup-1");
        AgentRunStore store = AgentRunStore.file(
                storePath.toString(),
                AgentRunStoreRetentionPolicy.of(0, 1),
                AgentRunStoreBackupRetentionPolicy.of(1));

        AgentRunStoreCompactionResult result = store.compact();

        assertThat(result.successful()).isTrue();
        assertThat(result.applied()).isTrue();
        assertThat(result.backupRetention().retainedBackupCount()).isEqualTo(1);
        assertThat(result.backupRetention().prunedBackupCount()).isZero();
        assertThat(result.backupRetention().failedBackupPruneCount()).isEqualTo(1);
        assertThat(result.backupRetention().failedBackupPrunePaths()).contains(stuckBackup.toString());
        assertThat(result.issues())
                .anySatisfy(issue -> {
                    assertThat(issue.severity()).isEqualTo("warning");
                    assertThat(issue.code()).isEqualTo("backup-retention.prune-incomplete");
                });
        assertThat(stuckBackup).exists();
    }

    @Test
    void fileStoreVerificationWarnsWhenBackupInventoryExceedsRetention(@TempDir Path directory) throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        Path firstBackup = writeCompactionBackup(storePath, "first");
        Path secondBackup = writeCompactionBackup(storePath, "second");
        AgentRunStore store = AgentRunStore.file(
                storePath.toString(),
                AgentRunStoreRetentionPolicy.defaults(),
                AgentRunStoreBackupRetentionPolicy.of(1));

        AgentRunStoreVerification verification = store.verification();

        assertThat(verification.passed()).isTrue();
        assertThat(verification.warningCount()).isEqualTo(1);
        assertThat(verification.passed(AgentRunStoreVerificationPolicy.strict())).isFalse();
        assertThat(verification.issues())
                .singleElement()
                .satisfies(issue -> {
                    assertThat(issue.severity()).isEqualTo("warning");
                    assertThat(issue.code()).isEqualTo("backup-retention.would-prune");
                    assertThat(issue.message()).contains("1 old backups");
                });
        assertThat(verification.diagnostics().backupInventory().backupCount()).isEqualTo(2);
        assertThat(backupStoreFiles(directory)).containsExactlyInAnyOrder(firstBackup, secondBackup);
    }

    @Test
    void fileStoreVerificationReportsCorruptSnapshotWithoutQuarantine(@TempDir Path directory) throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        Files.writeString(storePath, "count=1\nrun.0.runId=\\u00ZZ\n");
        AgentRunStore store = AgentRunStore.file(storePath.toString());

        AgentRunStoreVerification verification = store.verification();

        assertThat(verification.passed()).isFalse();
        assertThat(verification.exitCode()).isEqualTo(1);
        assertThat(verification.errorCount()).isEqualTo(1);
        assertThat(verification.issues())
                .singleElement()
                .satisfies(issue -> {
                    assertThat(issue.severity()).isEqualTo("error");
                    assertThat(issue.code()).isEqualTo("snapshot.unreadable");
                });
        assertThat(verification.diagnostics().snapshotPresent()).isTrue();
        assertThat(Files.exists(storePath)).isTrue();
        assertThat(quarantinedStoreFiles(directory)).isEmpty();
    }

    @Test
    void fileStoreVerificationReportsDecodedCountMismatch(@TempDir Path directory) throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        Files.writeString(storePath, String.join(System.lineSeparator(),
                "version=1",
                "count=2",
                "run.0.runId=run-count-mismatch-1",
                "run.0.state=COMPLETED",
                "run.0.strategy=strategy-a",
                "event.count=0"));
        AgentRunStore store = AgentRunStore.file(storePath.toString());

        AgentRunStoreVerification verification = store.verification();

        assertThat(verification.passed()).isFalse();
        assertThat(verification.issues())
                .singleElement()
                .satisfies(issue -> assertThat(issue.code()).isEqualTo("snapshot.status-count-mismatch"));
        assertThat(verification.diagnostics().statusCount()).isEqualTo(1);
        assertThat(Files.exists(storePath)).isTrue();
        assertThat(quarantinedStoreFiles(directory)).isEmpty();
    }

    @Test
    void fileStoreQuarantinesMalformedSnapshotsAndContinues(@TempDir Path directory) throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        Files.writeString(storePath, "count=1\nrun.0.runId=\\u00ZZ\n");
        AgentRunStore store = AgentRunStore.file(storePath.toString());

        AgentRunHistory history = store.history();

        assertThat(history.empty()).isTrue();
        assertThat(Files.exists(storePath)).isFalse();
        List<Path> quarantined = quarantinedStoreFiles(directory);
        assertThat(quarantined).hasSize(1);
        assertThat(Files.readString(quarantined.get(0))).contains("\\u00ZZ");

        AgentRunStatus saved = store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-file-recovered-1", "strategy-a"),
                true,
                "done after quarantine",
                Map.of("tenant", "tenant-a")));

        assertThat(Files.exists(storePath)).isTrue();
        assertThat(temporaryStoreFiles(directory)).isEmpty();
        assertThat(store.status("run-file-recovered-1").handle()).isEqualTo(saved.handle());
        assertThat(quarantinedStoreFiles(directory)).hasSize(1);
    }

    @Test
    void fileStoreQuarantinesUnsupportedFutureVersionsAndContinues(@TempDir Path directory) throws Exception {
        Path storePath = directory.resolve("wayang-runs.properties");
        Files.writeString(storePath, """
                version=999
                count=1
                run.0.runId=run-file-future-1
                run.0.state=COMPLETED
                run.0.strategy=strategy-a
                run.0.known=true
                run.0.message=future
                run.0.metadata.count=0
                event.count=0
                """);
        AgentRunStore store = AgentRunStore.file(storePath.toString());

        AgentRunHistory history = store.history();

        assertThat(history.empty()).isTrue();
        assertThat(Files.exists(storePath)).isFalse();
        List<Path> quarantined = quarantinedStoreFiles(directory, "unsupported-version");
        assertThat(quarantined).hasSize(1);
        assertThat(Files.readString(quarantined.get(0))).contains("version=999");

        AgentRunStatus saved = store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-file-version-recovered-1", "strategy-a"),
                true,
                "done after version quarantine",
                Map.of("tenant", "tenant-a")));

        assertThat(Files.exists(storePath)).isTrue();
        assertThat(store.status("run-file-version-recovered-1").handle()).isEqualTo(saved.handle());
        assertThat(quarantinedStoreFiles(directory)).hasSize(1);
    }

    @Test
    void forgetsStatusSnapshotsFromMemoryStore() {
        AgentRunStore store = AgentRunStore.memory();
        AgentRunStatus saved = store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-forget-1", "strategy-a"),
                true,
                "done",
                Map.of()));

        AgentRunForgetResult result = store.forget(" run-forget-1 ");

        assertThat(result.forgotten()).isTrue();
        assertThat(result.outcome()).isEqualTo(AgentRunOutcomes.FORGOTTEN);
        assertThat(result.runId()).isEqualTo("run-forget-1");
        assertThat(result.metadata())
                .containsEntry("state", "COMPLETED")
                .containsEntry("strategy", "strategy-a");
        assertThat(store.status(saved.handle().runId()).known()).isFalse();
        assertThat(store.history().empty()).isTrue();
        assertThat(store.timeline(saved.handle().runId()).empty()).isTrue();
    }

    @Test
    void persistsForgottenStatusSnapshotsInFileStore(@TempDir Path directory) {
        Path storePath = directory.resolve("wayang-runs.properties");
        AgentRunStore writer = AgentRunStore.file(storePath.toString());
        writer.save(new AgentRunStatus(
                AgentRunHandle.completed("run-file-forget-1", "strategy-a"),
                true,
                "done",
                Map.of("tenant", "tenant-a")));

        AgentRunForgetResult result = writer.forget("run-file-forget-1");
        AgentRunStore reader = AgentRunStore.file(storePath.toString());

        assertThat(result.forgotten()).isTrue();
        assertThat(result.outcome()).isEqualTo(AgentRunOutcomes.FORGOTTEN);
        assertThat(reader.status("run-file-forget-1").known()).isFalse();
        assertThat(reader.history().empty()).isTrue();
        assertThat(reader.timeline("run-file-forget-1").empty()).isTrue();
    }

    @Test
    void cancelsNonTerminalStatusSnapshots() {
        AgentRunStore store = AgentRunStore.memory();
        store.save(new AgentRunStatus(
                new AgentRunHandle("run-cancel-1", AgentRunState.RUNNING, "strategy-a"),
                true,
                "running",
                Map.of("tenant", "tenant-a")));

        AgentRunCancelResult result = store.cancel("run-cancel-1", "user requested stop");
        AgentRunStatus status = store.status("run-cancel-1");

        assertThat(result.cancelled()).isTrue();
        assertThat(result.outcome()).isEqualTo(AgentRunOutcomes.CANCELLED);
        assertThat(result.handle().state()).isEqualTo(AgentRunState.CANCELLED);
        assertThat(result.metadata())
                .containsEntry("tenant", "tenant-a")
                .containsEntry("previousState", "RUNNING")
                .containsEntry("reason", "user requested stop");
        assertThat(status.handle().state()).isEqualTo(AgentRunState.CANCELLED);
        assertThat(status.handle().terminal()).isTrue();
        assertThat(store.history(new AgentRunHistoryQuery(AgentRunState.CANCELLED, 10)).totalRuns()).isEqualTo(1);
        assertThat(store.timeline("run-cancel-1").events())
                .extracting(AgentRunEvent::state)
                .containsExactly(AgentRunState.RUNNING, AgentRunState.CANCELLED);
    }

    @Test
    void filtersRunEventsByStateTypeAndLatestLimit() {
        AgentRunStore store = AgentRunStore.memory();
        store.save(new AgentRunStatus(
                new AgentRunHandle("run-events-1", AgentRunState.RUNNING, "strategy-a"),
                true,
                "running",
                Map.of()));
        store.save(new AgentRunStatus(
                new AgentRunHandle("run-events-1", AgentRunState.WAITING_FOR_APPROVAL, "strategy-a"),
                true,
                "waiting",
                Map.of()));
        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-events-1", "strategy-a"),
                true,
                "done",
                Map.of()));

        AgentRunEvents latestTwo = store.timeline("run-events-1", AgentRunEventsQuery.of("", "", 2));
        AgentRunEvents completed = store.timeline("run-events-1", AgentRunEventsQuery.of("completed", "", 10));
        AgentRunEvents runningType = store.timeline("run-events-1", AgentRunEventsQuery.of("", "RUN.RUNNING", 10));
        AgentRunEvents afterFirst = store.timeline("run-events-1", AgentRunEventsQuery.of("", "", 1L, 10));

        assertThat(latestTwo.totalEvents()).isEqualTo(3);
        assertThat(latestTwo.returnedEvents()).isEqualTo(2);
        assertThat(latestTwo.cursor()).isEqualTo(new AgentRunEventsCursor(0, 2, 3, 3, 2, 3, 2));
        assertThat(latestTwo.cursor().remainingEvents()).isEqualTo(1);
        assertThat(latestTwo.cursor().advanced()).isTrue();
        assertThat(latestTwo.truncated()).isTrue();
        assertThat(latestTwo.firstSequence()).isEqualTo(2);
        assertThat(latestTwo.lastSequence()).isEqualTo(3);
        assertThat(latestTwo.nextAfterSequence()).isEqualTo(3);
        assertThat(latestTwo.stateCounts())
                .containsEntry("waiting-for-approval", 1)
                .containsEntry("completed", 1);
        assertThat(latestTwo.typeCounts())
                .containsEntry("run.waiting-for-approval", 1)
                .containsEntry("run.completed", 1);
        assertThat(latestTwo.stateSummaries())
                .containsExactly(
                        new AgentRunEventFacetSummary("completed", 1),
                        new AgentRunEventFacetSummary("waiting-for-approval", 1));
        assertThat(latestTwo.typeSummaries())
                .containsExactly(
                        new AgentRunEventFacetSummary("run.completed", 1),
                        new AgentRunEventFacetSummary("run.waiting-for-approval", 1));
        assertThat(latestTwo.summary()).isEqualTo(new AgentRunEventsSummary(
                3,
                2,
                Map.of("waiting-for-approval", 1, "completed", 1),
                Map.of("run.waiting-for-approval", 1, "run.completed", 1)));
        assertThat(latestTwo.summary().stateSummaries())
                .containsExactly(
                        new AgentRunEventFacetSummary("completed", 1),
                        new AgentRunEventFacetSummary("waiting-for-approval", 1));
        assertThat(latestTwo.summary().typeSummaries())
                .containsExactly(
                        new AgentRunEventFacetSummary("run.completed", 1),
                        new AgentRunEventFacetSummary("run.waiting-for-approval", 1));
        assertThat(latestTwo.events())
                .extracting(AgentRunEvent::state)
                .containsExactly(AgentRunState.WAITING_FOR_APPROVAL, AgentRunState.COMPLETED);
        assertThat(completed.query().filtered()).isTrue();
        assertThat(completed.events())
                .singleElement()
                .satisfies(event -> assertThat(event.state()).isEqualTo(AgentRunState.COMPLETED));
        assertThat(runningType.events())
                .singleElement()
                .satisfies(event -> assertThat(event.type()).isEqualTo("run.running"));
        assertThat(afterFirst.query().afterSequence()).isEqualTo(1);
        assertThat(afterFirst.cursor()).isEqualTo(new AgentRunEventsCursor(1, 2, 3, 3, 10, 2, 2));
        assertThat(afterFirst.truncated()).isFalse();
        assertThat(afterFirst.nextAfterSequence()).isEqualTo(3);
        assertThat(afterFirst.events())
                .extracting(AgentRunEvent::sequence)
                .containsExactly(2L, 3L);
    }

    @Test
    void ordersRunEventsBySequenceBeforeApplyingLatestLimit() {
        AgentRunStore store = AgentRunStore.memory();
        store.appendEvent(new AgentRunEvent(
                "run-events-order-1",
                30,
                "run.audit.late",
                AgentRunState.RUNNING,
                "late",
                Map.of()));
        store.appendEvent(new AgentRunEvent(
                "run-events-order-1",
                10,
                "run.audit.early",
                AgentRunState.RUNNING,
                "early",
                Map.of()));
        store.appendEvent(new AgentRunEvent(
                "run-events-order-1",
                20,
                "run.audit.middle",
                AgentRunState.RUNNING,
                "middle",
                Map.of()));

        AgentRunEvents latestTwo = store.timeline("run-events-order-1", AgentRunEventsQuery.of("", "", 2));
        AgentRunEvents afterTen = store.timeline("run-events-order-1", AgentRunEventsQuery.of("", "", 10L, 10));

        assertThat(latestTwo.totalEvents()).isEqualTo(3);
        assertThat(latestTwo.events())
                .extracting(AgentRunEvent::sequence)
                .containsExactly(20L, 30L);
        assertThat(latestTwo.cursor()).isEqualTo(new AgentRunEventsCursor(0, 20, 30, 30, 2, 3, 2));
        assertThat(afterTen.events())
                .extracting(AgentRunEvent::sequence)
                .containsExactly(20L, 30L);
    }

    @Test
    void refusesToCancelTerminalStatusSnapshots() {
        AgentRunStore store = AgentRunStore.memory();
        store.save(new AgentRunStatus(
                AgentRunHandle.completed("run-cancel-2", "strategy-a"),
                true,
                "done",
                Map.of()));

        AgentRunCancelResult result = store.cancel("run-cancel-2", "too late");

        assertThat(result.cancelled()).isFalse();
        assertThat(result.outcome()).isEqualTo(AgentRunOutcomes.NOT_CANCELLABLE);
        assertThat(result.handle().state()).isEqualTo(AgentRunState.COMPLETED);
        assertThat(result.message()).contains("completed").contains("cannot be cancelled");
        assertThat(store.status("run-cancel-2").handle().state()).isEqualTo(AgentRunState.COMPLETED);
    }

    @Test
    void returnsUnknownStatusForMissingRuns() {
        AgentRunStatus status = AgentRunStore.memory().status(" missing-run ");

        assertThat(status.known()).isFalse();
        assertThat(status.outcome()).isEqualTo(AgentRunOutcomes.UNKNOWN);
        assertThat(status.handle().runId()).isEqualTo("missing-run");
        assertThat(status.handle().state()).isEqualTo(AgentRunState.UNKNOWN);
        assertThat(status.message()).isEqualTo("No run status is recorded for this run id.");
        assertThat(AgentRunStore.memory().history().empty()).isTrue();
        assertThat(AgentRunStore.memory().timeline("missing-run").outcome()).isEqualTo(AgentRunOutcomes.EMPTY);
    }

    private static List<Path> temporaryStoreFiles(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(file -> file.getFileName().toString().endsWith(".tmp"))
                    .toList();
        }
    }

    private static List<Path> quarantinedStoreFiles(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(file -> file.getFileName().toString().contains(".corrupt-")
                            || file.getFileName().toString().contains(".unsupported-version-"))
                    .toList();
        }
    }

    private static List<Path> quarantinedStoreFiles(Path directory, String reason) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(file -> file.getFileName().toString().contains("." + reason + "-"))
                    .toList();
        }
    }

    private static List<Path> backupStoreFiles(Path directory) throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            return files
                    .filter(file -> file.getFileName().toString().contains(".compaction-"))
                    .toList();
        }
    }

    private static void writeRetentionWarningSnapshot(Path storePath, String runId) throws IOException {
        Files.writeString(storePath, String.join(System.lineSeparator(),
                "version=1",
                "count=1",
                "run.0.runId=" + runId,
                "run.0.state=RUNNING",
                "run.0.strategy=strategy-a",
                "run.0.known=true",
                "run.0.message=running",
                "run.0.metadata.count=0",
                "event.count=2",
                "event.0.runId=" + runId,
                "event.0.sequence=1",
                "event.0.type=run.audit",
                "event.0.state=RUNNING",
                "event.0.message=first",
                "event.0.metadata.count=0",
                "event.1.runId=" + runId,
                "event.1.sequence=2",
                "event.1.type=run.audit",
                "event.1.state=RUNNING",
                "event.1.message=second",
                "event.1.metadata.count=0"));
    }

    private static Path writeCompactionBackup(Path storePath, String suffix) throws IOException {
        Path backupPath = storePath.resolveSibling(
                storePath.getFileName().toString() + ".compaction-" + suffix + ".properties");
        Files.writeString(backupPath, String.join(System.lineSeparator(),
                "version=1",
                "count=0",
                "event.count=0"));
        return backupPath;
    }

    private static Path writeStuckCompactionBackup(Path storePath, String suffix) throws IOException {
        Path backupPath = storePath.resolveSibling(
                storePath.getFileName().toString() + ".compaction-" + suffix + ".properties");
        Files.createDirectories(backupPath);
        Files.writeString(backupPath.resolve("held.txt"), "held");
        Files.setLastModifiedTime(backupPath, FileTime.fromMillis(0));
        return backupPath;
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for test latch.", e);
        }
    }

    @Test
    void parsesHistoryQueryFromCliStyleValues() {
        AgentRunHistoryQuery query = AgentRunHistoryQuery.of(
                "waiting-for-approval",
                1_000,
                " tenant-a ",
                " session-a ",
                " assistant-agent ",
                " low-code-agent ",
                9);

        assertThat(query.state()).isEqualTo(AgentRunState.WAITING_FOR_APPROVAL);
        assertThat(query.limit()).isEqualTo(AgentRunHistoryQuery.MAX_LIMIT);
        assertThat(query.offset()).isEqualTo(9);
        assertThat(query.tenantId()).isEqualTo("tenant-a");
        assertThat(query.sessionId()).isEqualTo("session-a");
        assertThat(query.surfaceId()).isEqualTo("assistant-agent");
        assertThat(query.profileId()).isEqualTo("low-code-agent");
        assertThat(query.filtered()).isTrue();

        assertThatThrownBy(() -> AgentRunHistoryQuery.of("missing", 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown run state 'missing'")
                .hasMessageContaining("completed");
    }
}
