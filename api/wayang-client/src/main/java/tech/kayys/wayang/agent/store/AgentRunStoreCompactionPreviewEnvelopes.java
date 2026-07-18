package tech.kayys.wayang.agent.store;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.agent.run.AgentRunEnvelopeMaps;
import tech.kayys.wayang.agent.run.AgentRunEnvelopes;

/**
 * Wire envelope factory for run-store compaction previews.
 */
public final class AgentRunStoreCompactionPreviewEnvelopes {

    private AgentRunStoreCompactionPreviewEnvelopes() {
    }

    public static Map<String, Object> preview(AgentRunStoreCompactionPreview preview) {
        AgentRunStoreCompactionPreview model = preview == null
                ? AgentRunStore.memory().compactionPreview()
                : preview;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(
                AgentRunLifecycleContract.runStoreCompactionPreview()));
        values.putAll(model.toMap());
        return AgentRunEnvelopeMaps.copy(values);
    }
}
