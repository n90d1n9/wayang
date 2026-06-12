package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AgentRunControlEnvelopes {

    private AgentRunControlEnvelopes() {
    }

    public static Map<String, Object> forget(AgentRunForgetResult result) {
        AgentRunForgetResult model = result == null ? AgentRunForgetResult.notFound("", "") : result;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runForget()));
        values.put("runId", model.runId());
        values.put("forgotten", model.forgotten());
        values.put("outcome", model.outcome());
        values.put("message", model.message());
        values.put("metadata", model.metadata());
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> cancel(AgentRunCancelResult result) {
        AgentRunCancelResult model = result == null ? AgentRunCancelResult.notFound("", "") : result;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runCancel()));
        values.put("runId", model.runId());
        values.put("cancelled", model.cancelled());
        values.put("outcome", model.outcome());
        values.put("handle", AgentRunEnvelopes.handle(model.handle()));
        values.put("message", model.message());
        values.put("metadata", model.metadata());
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> waitResult(AgentRunWaitResult result) {
        AgentRunWaitResult model = result == null
                ? new AgentRunWaitResult("", null, false, false, 0, 0, "", Map.of())
                : result;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runWait()));
        values.put("runId", model.runId());
        values.put("outcome", model.outcome());
        values.put("terminal", model.terminal());
        values.put("timedOut", model.timedOut());
        values.put("attempts", model.attempts());
        values.put("elapsedMillis", model.elapsedMillis());
        values.put("status", AgentRunEnvelopes.status(model.status()));
        values.put("message", model.message());
        values.put("metadata", model.metadata());
        return AgentRunEnvelopeMaps.copy(values);
    }
}
