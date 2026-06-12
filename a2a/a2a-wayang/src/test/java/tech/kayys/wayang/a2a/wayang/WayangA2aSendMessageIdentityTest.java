package tech.kayys.wayang.a2a.wayang;

import org.junit.jupiter.api.Test;
import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aPart;
import tech.kayys.wayang.a2a.core.A2aRole;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WayangA2aSendMessageIdentityTest {

    @Test
    void resolvesTaskIdFromMessageThenMetadataThenGeneratedFallback() {
        assertThat(WayangA2aSendMessageIdentity.taskId(request("task-message", null, Map.of("taskId", "task-meta"))))
                .isEqualTo("task-message");
        assertThat(WayangA2aSendMessageIdentity.taskId(request(null, null, Map.of("taskId", "task-meta"))))
                .isEqualTo("task-meta");
        assertThat(WayangA2aSendMessageIdentity.taskId(request(null, null, Map.of())))
                .startsWith("task-");
    }

    @Test
    void resolvesContextIdFromMessageThenMetadataThenTaskId() {
        assertThat(WayangA2aSendMessageIdentity.contextId(
                request("task-1", "context-message", Map.of("contextId", "context-meta")),
                "task-1"))
                .isEqualTo("context-message");
        assertThat(WayangA2aSendMessageIdentity.contextId(
                request("task-1", null, Map.of("contextId", "context-meta")),
                "task-1"))
                .isEqualTo("context-meta");
        assertThat(WayangA2aSendMessageIdentity.contextId(request("task-1", null, Map.of()), "task-1"))
                .isEqualTo("task-1");
    }

    @Test
    void requiresExplicitMessageTaskIdForSmokeScenarios() {
        assertThat(WayangA2aSendMessageIdentity.requiredMessageTaskId(
                request("task-message", null, Map.of()),
                "task id required"))
                .isEqualTo("task-message");
        assertThatThrownBy(() -> WayangA2aSendMessageIdentity.requiredMessageTaskId(
                request(null, null, Map.of("taskId", "task-meta")),
                "task id required"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("task id required");
    }

    private static A2aSendMessageRequest request(String taskId, String contextId, Map<String, Object> metadata) {
        return new A2aSendMessageRequest(
                null,
                new A2aMessage(
                        "message-identity",
                        contextId,
                        taskId,
                        A2aRole.ROLE_USER,
                        List.of(A2aPart.text("ping")),
                        Map.of(),
                        List.of(),
                        List.of()),
                null,
                metadata);
    }
}
