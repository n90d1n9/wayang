package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

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
