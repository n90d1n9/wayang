package tech.kayys.wayang.agent.hermes;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HermesSessionSnapshotTest {

    @Test
    void classifiesPlannedSessionAsResumable() {
        HermesRuntimeEvent event = event(
                HermesRuntimeEvent.TYPE_REQUEST_PLANNED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "planned",
                "2026-06-03T00:00:00Z",
                Map.of());

        HermesSessionSnapshot snapshot = snapshot(List.of(event), HermesRuntimeEventQuery.forSession("session-a", 10));

        assertThat(snapshot.status()).isEqualTo("planned");
        assertThat(snapshot.resumable()).isTrue();
        assertThat(snapshot.terminal()).isFalse();
        assertThat(snapshot.requiresAttention()).isFalse();
        assertThat(snapshot.pendingActionCount()).isEqualTo(1);
        assertThat(snapshot.latestRequestId()).isEqualTo("req-1");
        assertThat(snapshot.requestIds()).containsExactly("req-1");
    }

    @Test
    void classifiesDispatchAttentionAsResumableNeedsAttention() {
        HermesRuntimeEvent planned = event(
                HermesRuntimeEvent.TYPE_REQUEST_PLANNED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "planned",
                "2026-06-03T00:00:00Z",
                Map.of());
        HermesRuntimeEvent dispatched = event(
                HermesRuntimeEvent.TYPE_DIRECTIVES_DISPATCHED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "degraded",
                "2026-06-03T00:00:01Z",
                Map.of(
                        "attentionCount", 2,
                        "remediationPlan", Map.of(
                                "required", true,
                                "actionCount", 2,
                                "strategy", "retry-runtime-adapter")));

        HermesSessionSnapshot snapshot = snapshot(
                List.of(planned, dispatched),
                HermesRuntimeEventQuery.forSession("session-a", 10));
        Map<String, Object> metadata = snapshot.toMetadata();

        assertThat(snapshot.status()).isEqualTo("needs-attention");
        assertThat(snapshot.resumable()).isTrue();
        assertThat(snapshot.terminal()).isFalse();
        assertThat(snapshot.requiresAttention()).isTrue();
        assertThat(snapshot.pendingActionCount()).isEqualTo(2);
        assertThat(snapshot.attentionCount()).isEqualTo(2);
        assertThat(snapshot.remediationActionCount()).isEqualTo(2);
        assertThat(snapshot.latestEventType()).isEqualTo(HermesRuntimeEvent.TYPE_DIRECTIVES_DISPATCHED);
        assertThat(metadata)
                .containsEntry("status", "needs-attention")
                .containsEntry("resumable", true)
                .containsEntry("pendingActionCount", 2)
                .containsKey("summary");
    }

    @Test
    void classifiesSuccessfulCompletionAsTerminal() {
        HermesRuntimeEvent failedAttempt = event(
                HermesRuntimeEvent.TYPE_RESPONSE_FAILED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "failed",
                "2026-06-03T00:00:00Z",
                Map.of());
        HermesRuntimeEvent completed = event(
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-2",
                "tenant-a",
                "session-a",
                "user-a",
                "successful",
                "2026-06-03T00:00:01Z",
                Map.of());

        HermesSessionSnapshot snapshot = snapshot(
                List.of(failedAttempt, completed),
                HermesRuntimeEventQuery.forSession("session-a", 10));

        assertThat(snapshot.status()).isEqualTo("completed");
        assertThat(snapshot.resumable()).isFalse();
        assertThat(snapshot.terminal()).isTrue();
        assertThat(snapshot.requiresAttention()).isFalse();
        assertThat(snapshot.failedEvents()).isEqualTo(1);
        assertThat(snapshot.successfulEvents()).isEqualTo(1);
        assertThat(snapshot.pendingActionCount()).isZero();
        assertThat(snapshot.latestRequestId()).isEqualTo("req-2");
        assertThat(snapshot.requestIds()).containsExactly("req-1", "req-2");
    }

    @Test
    void classifiesLearningCompletionAfterResponseAsTerminalCompleted() {
        HermesRuntimeEvent completed = event(
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "successful",
                "2026-06-03T00:00:00Z",
                Map.of());
        HermesRuntimeEvent learning = event(
                HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "created",
                "2026-06-03T00:00:01Z",
                Map.of("decision", "created", "skillId", "hermes-release-workflow"));

        HermesSessionSnapshot snapshot = snapshot(
                List.of(completed, learning),
                HermesRuntimeEventQuery.forSession("session-a", 10));

        assertThat(snapshot.status()).isEqualTo("completed");
        assertThat(snapshot.resumable()).isFalse();
        assertThat(snapshot.terminal()).isTrue();
        assertThat(snapshot.requiresAttention()).isFalse();
        assertThat(snapshot.latestEventType()).isEqualTo(HermesRuntimeEvent.TYPE_SKILL_LEARNING_COMPLETED);
        assertThat(snapshot.latestOutcome()).isEqualTo("created");
    }

    @Test
    void classifiesLearningFailureAsTerminalNeedsAttention() {
        HermesRuntimeEvent learningFailed = event(
                HermesRuntimeEvent.TYPE_SKILL_LEARNING_FAILED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "failed",
                "2026-06-03T00:00:01Z",
                Map.of("error", "artifact store unavailable"));

        HermesSessionSnapshot snapshot = snapshot(
                List.of(learningFailed),
                HermesRuntimeEventQuery.forSession("session-a", 10));

        assertThat(snapshot.status()).isEqualTo("needs-attention");
        assertThat(snapshot.resumable()).isTrue();
        assertThat(snapshot.terminal()).isTrue();
        assertThat(snapshot.requiresAttention()).isTrue();
        assertThat(snapshot.pendingActionCount()).isEqualTo(1);
    }

    @Test
    void journalServiceBuildsSnapshotFromSessionQuery() {
        HermesRuntimeEvent completed = event(
                HermesRuntimeEvent.TYPE_RESPONSE_COMPLETED,
                "req-1",
                "tenant-a",
                "session-a",
                "user-a",
                "successful",
                "2026-06-03T00:00:00Z",
                Map.of());
        HermesRuntimeEventReader reader = query -> HermesRuntimeEventPages.from(List.of(completed), query);

        HermesSessionSnapshot snapshot = new HermesRuntimeJournalService(reader)
                .sessionSnapshot("session-a", 10);

        assertThat(snapshot.sessionId()).isEqualTo("session-a");
        assertThat(snapshot.status()).isEqualTo("completed");
        assertThat(snapshot.matchedEvents()).isEqualTo(1);
    }

    private static HermesSessionSnapshot snapshot(
            List<HermesRuntimeEvent> events,
            HermesRuntimeEventQuery query) {
        HermesRuntimeEventPage page = HermesRuntimeEventPages.from(events, query);
        return HermesSessionSnapshot.from(HermesRuntimeJournalView.from(query, page));
    }

    private static HermesRuntimeEvent event(
            String type,
            String requestId,
            String tenantId,
            String sessionId,
            String userId,
            String outcome,
            String occurredAt,
            Map<String, Object> metadata) {
        return new HermesRuntimeEvent(
                "",
                type,
                requestId,
                tenantId,
                sessionId,
                userId,
                outcome,
                Instant.parse(occurredAt),
                metadata);
    }
}
