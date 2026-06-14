package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AgentRunInspectionEnvelopes {

    private AgentRunInspectionEnvelopes() {
    }

    public static Map<String, Object> statusEnvelope(AgentRunStatus status) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runStatus()));
        values.putAll(AgentRunEnvelopes.status(status));
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> inspection(AgentRunInspection inspection) {
        AgentRunInspection model = inspection == null ? new AgentRunInspection("", null, null, "") : inspection;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runInspect()));
        values.put("runId", model.runId());
        values.put("outcome", model.outcome());
        values.put("known", model.known());
        values.put("empty", model.empty());
        values.put("message", model.message());
        values.put("status", AgentRunEnvelopes.status(model.status()));
        values.put("events", AgentRunEventEnvelopes.events(model.events()));
        return AgentRunEnvelopeMaps.copy(values);
    }
}
