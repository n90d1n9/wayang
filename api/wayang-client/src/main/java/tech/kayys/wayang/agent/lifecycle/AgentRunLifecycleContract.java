package tech.kayys.wayang.agent.lifecycle;

import tech.kayys.wayang.client.SdkText;

public record AgentRunLifecycleContract(
        String schema,
        int version,
        String envelope) {

    public static final String SCHEMA = "wayang.run.lifecycle";
    public static final int VERSION = 1;
    public static final String RUN_RESULT = "run-result";
    public static final String RUN_STATUS = "run-status";
    public static final String RUN_EVENTS = "run-events";
    public static final String RUN_EVENTS_STATS = "run-events-stats";
    public static final String RUN_EVENTS_FOLLOW = "run-events-follow";
    public static final String RUN_INSPECT = "run-inspect";
    public static final String RUN_LIST = "run-list";
    public static final String RUN_STATS = "run-stats";
    public static final String RUN_WAIT = "run-wait";
    public static final String RUN_CANCEL = "run-cancel";
    public static final String RUN_FORGET = "run-forget";
    public static final String RUN_STORE = "run-store";
    public static final String RUN_STORE_VERIFICATION = "run-store-verification";
    public static final String RUN_STORE_COMPACTION_PREVIEW = "run-store-compaction-preview";
    public static final String RUN_STORE_COMPACTION = "run-store-compaction";

    public AgentRunLifecycleContract {
        schema = SdkText.trimToDefault(schema, SCHEMA);
        version = Math.max(1, version);
        envelope = SdkText.trimToEmpty(envelope);
    }

    public static AgentRunLifecycleContract of(String envelope) {
        return new AgentRunLifecycleContract(SCHEMA, VERSION, envelope);
    }

    public static AgentRunLifecycleContract runResult() {
        return of(RUN_RESULT);
    }

    public static AgentRunLifecycleContract runStatus() {
        return of(RUN_STATUS);
    }

    public static AgentRunLifecycleContract runEvents() {
        return of(RUN_EVENTS);
    }

    public static AgentRunLifecycleContract runEventsStats() {
        return of(RUN_EVENTS_STATS);
    }

    public static AgentRunLifecycleContract runEventsFollow() {
        return of(RUN_EVENTS_FOLLOW);
    }

    public static AgentRunLifecycleContract runInspect() {
        return of(RUN_INSPECT);
    }

    public static AgentRunLifecycleContract runList() {
        return of(RUN_LIST);
    }

    public static AgentRunLifecycleContract runStats() {
        return of(RUN_STATS);
    }

    public static AgentRunLifecycleContract runWait() {
        return of(RUN_WAIT);
    }

    public static AgentRunLifecycleContract runCancel() {
        return of(RUN_CANCEL);
    }

    public static AgentRunLifecycleContract runForget() {
        return of(RUN_FORGET);
    }

    public static AgentRunLifecycleContract runStore() {
        return of(RUN_STORE);
    }

    public static AgentRunLifecycleContract runStoreVerification() {
        return of(RUN_STORE_VERIFICATION);
    }

    public static AgentRunLifecycleContract runStoreCompactionPreview() {
        return of(RUN_STORE_COMPACTION_PREVIEW);
    }

    public static AgentRunLifecycleContract runStoreCompaction() {
        return of(RUN_STORE_COMPACTION);
    }
}
