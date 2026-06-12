package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

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
