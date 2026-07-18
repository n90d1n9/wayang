package tech.kayys.wayang.agent.store;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.agent.run.AgentRunEnvelopeMaps;
import tech.kayys.wayang.agent.run.AgentRunEnvelopes;

/**
 * Wire envelope factory for run-store backend, snapshot, and retention diagnostics.
 */
public final class AgentRunStoreDiagnosticsEnvelopes {

    private AgentRunStoreDiagnosticsEnvelopes() {
    }

    public static Map<String, Object> diagnostics(AgentRunStoreDiagnostics diagnostics) {
        AgentRunStoreDiagnostics model = diagnostics == null
                ? AgentRunStore.memory().diagnostics()
                : diagnostics;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runStore()));
        values.putAll(model.toMap());
        return AgentRunEnvelopeMaps.copy(values);
    }
}
