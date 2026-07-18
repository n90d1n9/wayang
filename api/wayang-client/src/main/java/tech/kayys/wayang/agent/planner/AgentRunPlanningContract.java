package tech.kayys.wayang.agent.planner;

import tech.kayys.wayang.client.SdkText;

public record AgentRunPlanningContract(
        String schema,
        int version,
        String envelope) {

    public static final String SCHEMA = "wayang.run.planning";
    public static final int VERSION = 1;
    public static final String RUN_PREFLIGHT = "run-preflight";
    public static final String RUN_PREVIEW = "run-preview";

    public AgentRunPlanningContract {
        schema = SdkText.trimToDefault(schema, SCHEMA);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static AgentRunPlanningContract of(String envelope) {
        return new AgentRunPlanningContract(SCHEMA, VERSION, envelope);
    }

    public static AgentRunPlanningContract runPreflight() {
        return of(RUN_PREFLIGHT);
    }

    public static AgentRunPlanningContract runPreview() {
        return of(RUN_PREVIEW);
    }
}
