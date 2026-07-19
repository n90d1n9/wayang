package tech.kayys.wayang.agent.store;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.agent.run.AgentRunEnvelopeMaps;
import tech.kayys.wayang.agent.run.AgentRunEnvelopes;

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
