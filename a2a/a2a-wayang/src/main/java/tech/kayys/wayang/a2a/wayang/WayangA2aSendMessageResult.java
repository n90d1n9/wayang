package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.a2a.core.A2aMessage;
import tech.kayys.wayang.a2a.core.A2aSendMessageRequest;
import tech.kayys.wayang.a2a.core.A2aTask;
import tech.kayys.wayang.agent.spi.AgentRequest;
import tech.kayys.wayang.agent.spi.AgentResponse;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Captures the task and mapped Wayang execution values for one send-message call.
 */
public record WayangA2aSendMessageResult(
        A2aSendMessageRequest a2aRequest,
        AgentRequest agentRequest,
        AgentResponse agentResponse,
        A2aTask task) {

    public WayangA2aSendMessageResult {
        if (a2aRequest == null) {
            throw new IllegalArgumentException("a2aRequest must not be null");
        }
        if (agentRequest == null) {
            throw new IllegalArgumentException("agentRequest must not be null");
        }
        if (agentResponse == null) {
            throw new IllegalArgumentException("agentResponse must not be null");
        }
        if (task == null) {
            throw new IllegalArgumentException("task must not be null");
        }
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("task", responseTask().toMap());
        values.put("agentRequestId", agentRequest.requestId());
        values.put("agentRunId", agentResponse.runId());
        values.put("successful", agentResponse.successful());
        return WayangA2aMaps.copyMap(values);
    }

    public A2aTask responseTask() {
        Integer historyLength = a2aRequest.configuration() == null
                ? null
                : a2aRequest.configuration().historyLength();
        if (historyLength == null) {
            return task;
        }
        List<A2aMessage> history = task.history();
        int from = Math.max(0, history.size() - historyLength);
        return new A2aTask(
                task.id(),
                task.contextId(),
                task.status(),
                task.artifacts(),
                history.subList(from, history.size()),
                task.metadata());
    }
}
