package tech.kayys.wayang.agent.run;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkLists;
import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;

public record AgentRunResult(
        String runId,
        String answer,
        boolean successful,
        String strategy,
        List<String> steps,
        Map<String, Object> metadata,
        AgentRunHandle handle) {

    public AgentRunResult(
            String runId,
            String answer,
            boolean successful,
            String strategy,
            List<String> steps,
            Map<String, Object> metadata) {
        this(runId, answer, successful, strategy, steps, metadata, null);
    }

    public AgentRunResult {
        runId = SdkText.trimToDefault(runId, "local-run");
        answer = SdkText.trimToEmpty(answer);
        strategy = SdkText.trimToDefault(strategy, "reactive-agent");
        steps = SdkLists.copy(steps);
        metadata = SdkMaps.copy(metadata);
        handle = handle == null
                ? new AgentRunHandle(runId, successful ? AgentRunState.COMPLETED : AgentRunState.FAILED, strategy)
                : handle;
    }

    public String outcome() {
        return handle.terminal() ? AgentRunOutcomes.TERMINAL : AgentRunOutcomes.PENDING;
    }
}
