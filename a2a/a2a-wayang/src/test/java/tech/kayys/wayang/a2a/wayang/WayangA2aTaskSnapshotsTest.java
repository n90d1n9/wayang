package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aArtifact;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.a2a.core.A2aTaskState;
import tech.kayys.wayang.a2a.core.A2aTaskStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aTaskSnapshotsTest {

    @Test
    void replacesStatusWhilePreservingTaskPayload() {
        A2aTask task = task();
        A2aTaskStatus status = A2aTaskStatus.of(A2aTaskState.TASK_STATE_WORKING);

        A2aTask updated = WayangA2aTaskSnapshots.withStatus(task, status);

        assertThat(updated.status()).isEqualTo(status);
        assertThat(updated.id()).isEqualTo(task.id());
        assertThat(updated.contextId()).isEqualTo(task.contextId());
        assertThat(updated.history()).isEqualTo(task.history());
        assertThat(updated.artifacts()).isEqualTo(task.artifacts());
        assertThat(updated.metadata()).isEqualTo(task.metadata());
    }

    @Test
    void appendsMessagesAndArtifactsWithoutMutatingOriginalTask() {
        A2aTask task = task();
        A2aMessage message = message("message-2", "second");
        A2aArtifact artifact = artifact("artifact-1", "answer");

        A2aTask withMessage = WayangA2aTaskSnapshots.withAppendedMessage(task, message);
        A2aTask withArtifact = WayangA2aTaskSnapshots.withAppendedArtifact(withMessage, artifact);

        assertThat(task.history()).hasSize(1);
        assertThat(task.artifacts()).isEmpty();
        assertThat(withMessage.history()).containsExactlyElementsOf(List.of(task.history().get(0), message));
        assertThat(withArtifact.history()).isEqualTo(withMessage.history());
        assertThat(withArtifact.artifacts()).containsExactly(artifact);
    }

    @Test
    void rejectsMissingTaskOrMutationPayload() {
        A2aTask task = task();

        assertThatThrownBy(() -> WayangA2aTaskSnapshots.withStatus(null, task.status()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("task");
        assertThatThrownBy(() -> WayangA2aTaskSnapshots.withStatus(task, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("status");
        assertThatThrownBy(() -> WayangA2aTaskSnapshots.withAppendedMessage(task, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("message");
        assertThatThrownBy(() -> WayangA2aTaskSnapshots.withAppendedArtifact(task, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("artifact");
    }

    private static A2aTask task() {
        return new A2aTask(
                "task-1",
                "context-1",
                A2aTaskStatus.of(A2aTaskState.TASK_STATE_SUBMITTED),
                List.of(),
                List.of(message("message-1", "first")),
                Map.of("tenant", "tenant-a"));
    }

    private static A2aMessage message(String id, String text) {
        return new A2aMessage(
                id,
                null,
                null,
                A2aRole.ROLE_USER,
                List.of(A2aPart.text(text)),
                Map.of(),
                List.of(),
                List.of());
    }

    private static A2aArtifact artifact(String id, String text) {
        return new A2aArtifact(
                id,
                "answer",
                null,
                List.of(A2aPart.text(text)),
                Map.of(),
                List.of());
    }
}
