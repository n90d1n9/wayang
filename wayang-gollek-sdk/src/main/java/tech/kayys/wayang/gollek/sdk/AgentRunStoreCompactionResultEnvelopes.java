package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wire envelope factory for run-store compaction results.
 */
public final class AgentRunStoreCompactionResultEnvelopes {

    private AgentRunStoreCompactionResultEnvelopes() {
    }

    public static Map<String, Object> result(AgentRunStoreCompactionResult result) {
        AgentRunStoreCompactionResult model = result == null
                ? AgentRunStore.memory().compact()
                : result;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(
                AgentRunLifecycleContract.runStoreCompaction()));
        values.putAll(model.toMap());
        return AgentRunEnvelopeMaps.copy(values);
    }
}
