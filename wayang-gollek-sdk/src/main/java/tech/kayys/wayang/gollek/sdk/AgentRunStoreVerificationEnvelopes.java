package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Wire envelope factory for run-store verification reports.
 */
public final class AgentRunStoreVerificationEnvelopes {

    private AgentRunStoreVerificationEnvelopes() {
    }

    public static Map<String, Object> verification(AgentRunStoreVerification verification) {
        return verification(verification, AgentRunStoreVerificationPolicy.lenient());
    }

    public static Map<String, Object> verification(
            AgentRunStoreVerification verification,
            AgentRunStoreVerificationPolicy policy) {
        AgentRunStoreVerification model = verification == null
                ? AgentRunStore.memory().verification()
                : verification;
        AgentRunStoreVerificationPolicy resolvedPolicy = policy == null
                ? AgentRunStoreVerificationPolicy.lenient()
                : policy;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("contract", AgentRunEnvelopes.lifecycleContract(AgentRunLifecycleContract.runStoreVerification()));
        values.putAll(model.toMap(resolvedPolicy));
        return AgentRunEnvelopeMaps.copy(values);
    }
}
