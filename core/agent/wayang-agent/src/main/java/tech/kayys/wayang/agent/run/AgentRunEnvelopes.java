package tech.kayys.wayang.agent.run;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.agent.lifecycle.AgentRunLifecycleContract;
import tech.kayys.wayang.agent.planner.AgentRunPlanningContract;
import tech.kayys.wayang.agent.skill.AgentRunSkillAssessmentContext;
import tech.kayys.wayang.client.SurfacePolicyAssessmentContext;
import tech.kayys.wayang.client.WayangSkillCatalog;

public final class AgentRunEnvelopes {

    private AgentRunEnvelopes() {
    }

    public static Map<String, Object> preflight(AgentRunReadiness readiness) {
        AgentRunReadiness model = readiness == null ? defaultReadiness() : readiness;
        Map<String, Object> values = new LinkedHashMap<>();
        putPlanning(values, AgentRunPlanningContract.runPreflight());
        values.putAll(AgentRunReadinessContext.from(model));
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> preview(AgentRunPreview preview) {
        AgentRunPreview model = preview == null ? defaultPreview() : preview;
        Map<String, Object> values = new LinkedHashMap<>();
        putPlanning(values, AgentRunPlanningContract.runPreview());
        values.put("requestId", model.requestId());
        values.put("tenantId", model.tenantId());
        values.put("modelId", model.modelId());
        values.put("workflowId", model.workflowId());
        values.put("surfaceId", model.surfaceId());
        values.put("sessionId", model.sessionId());
        values.put("userId", model.userId());
        values.put("systemPromptPresent", model.systemPromptPresent());
        values.put("promptCharacters", model.promptCharacters());
        values.put("systemPromptCharacters", model.systemPromptCharacters());
        values.put("memoryEnabled", model.memoryEnabled());
        values.put("maxSteps", model.maxSteps());
        values.put("workspaceAttached", model.workspaceAttached());
        values.put("harnessAttached", model.harnessAttached());
        values.put("skills", model.skills());
        values.put("contextKeys", model.contextKeys());
        values.put("context", model.context());
        values.put("parameters", model.parameters());
        values.put("surfacePolicyAssessment", SurfacePolicyAssessmentContext.from(model.surfacePolicyAssessment()));
        values.put("skillAssessment", AgentRunSkillAssessmentContext.from(model.skillAssessment()));
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> result(AgentRunResult result) {
        AgentRunResult model = result == null ? defaultResult() : result;
        Map<String, Object> values = new LinkedHashMap<>();
        putLifecycle(values, AgentRunLifecycleContract.runResult());
        values.put("runId", model.runId());
        values.put("answer", model.answer());
        values.put("successful", model.successful());
        values.put("outcome", model.outcome());
        values.put("strategy", model.strategy());
        values.put("handle", handle(model.handle()));
        values.put("steps", model.steps());
        values.put("metadata", model.metadata());
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> status(AgentRunStatus status) {
        AgentRunStatus model = status == null
                ? new AgentRunStatus(AgentRunHandle.unknown(""), false, "Run status is unknown.", Map.of())
                : status;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("handle", handle(model.handle()));
        values.put("outcome", model.outcome());
        values.put("known", model.known());
        values.put("message", model.message());
        values.put("metadata", model.metadata());
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> handle(AgentRunHandle handle) {
        AgentRunHandle model = handle == null ? AgentRunHandle.unknown("") : handle;
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("runId", model.runId());
        values.put("state", model.state().name());
        values.put("strategy", model.strategy());
        values.put("terminal", model.terminal());
        return AgentRunEnvelopeMaps.copy(values);
    }

    public static Map<String, Object> planningContract(AgentRunPlanningContract contract) {
        AgentRunPlanningContract model = contract == null ? AgentRunPlanningContract.of("") : contract;
        return contract(model.schema(), model.version(), model.envelope());
    }

    public static Map<String, Object> lifecycleContract(AgentRunLifecycleContract contract) {
        AgentRunLifecycleContract model = contract == null ? AgentRunLifecycleContract.of("") : contract;
        return contract(model.schema(), model.version(), model.envelope());
    }

    private static void putPlanning(Map<String, Object> values, AgentRunPlanningContract contract) {
        values.put("contract", planningContract(contract));
    }

    private static void putLifecycle(Map<String, Object> values, AgentRunLifecycleContract contract) {
        values.put("contract", lifecycleContract(contract));
    }

    private static Map<String, Object> contract(String schema, int version, String envelope) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("schema", schema);
        values.put("version", version);
        values.put("envelope", envelope);
        return AgentRunEnvelopeMaps.copy(values);
    }

    private static AgentRunReadiness defaultReadiness() {
        return AgentRunReadiness.assess(WayangSkillCatalog.defaultRegistry(), AgentRunRequest.builder().build());
    }

    private static AgentRunPreview defaultPreview() {
        return AgentRunPreview.from(AgentRunRequest.builder().build(), null);
    }

    private static AgentRunResult defaultResult() {
        return new AgentRunResult("", "", false, "", List.of(), Map.of());
    }
}
