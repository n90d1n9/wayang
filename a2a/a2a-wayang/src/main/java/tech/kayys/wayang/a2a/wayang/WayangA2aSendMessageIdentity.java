package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;

import java.util.UUID;

/**
 * Shared task and context id resolution for A2A SendMessage requests.
 */
final class WayangA2aSendMessageIdentity {

    private WayangA2aSendMessageIdentity() {
    }

    static String taskId(A2aSendMessageRequest request) {
        A2aSendMessageRequest resolved = requireRequest(request);
        String explicitTaskId = WayangA2aMaps.optional(resolved.message().taskId());
        if (explicitTaskId != null) {
            return explicitTaskId;
        }
        return WayangA2aMaps.firstString(resolved.metadata(), WayangA2a.TASK_ID_KEY, "taskId")
                .orElseGet(() -> "task-" + UUID.randomUUID());
    }

    static String contextId(A2aSendMessageRequest request, String taskId) {
        A2aSendMessageRequest resolved = requireRequest(request);
        String messageContextId = WayangA2aMaps.optional(resolved.message().contextId());
        if (messageContextId != null) {
            return messageContextId;
        }
        return WayangA2aMaps.firstString(resolved.metadata(), WayangA2a.CONTEXT_ID_KEY, "contextId")
                .orElse(WayangA2aMaps.required(taskId, "taskId"));
    }

    static String requiredMessageTaskId(A2aSendMessageRequest request, String reason) {
        A2aSendMessageRequest resolved = requireRequest(request);
        String taskId = WayangA2aMaps.optional(resolved.message().taskId());
        if (taskId == null) {
            throw new IllegalArgumentException(WayangA2aMaps.required(reason, "reason"));
        }
        return taskId;
    }

    private static A2aSendMessageRequest requireRequest(A2aSendMessageRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("sendMessageRequest must not be null");
        }
        return request;
    }
}
