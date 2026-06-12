package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentRunEnvelopesTest {

    @Test
    void preflightEnvelopeOwnsPublishedReadinessShape() {
        AgentRunReadiness readiness = new AgentRunReadiness(
                "coding-agent",
                true,
                new SurfacePolicyAssessment(
                        "coding-agent",
                        false,
                        List.of("tenantId"),
                        List.of("workspace"),
                        List.of("Attach workspace context."),
                        List.of("route:agent")),
                new AgentRunSkillAssessment(
                        "coding-agent",
                        false,
                        List.of("search"),
                        List.of(),
                        List.of("search"),
                        List.of(),
                        List.of(),
                        List.of("Install search skill.")));

        Map<String, Object> values = AgentRunEnvelopes.preflight(readiness);

        assertThat(objectMap(values.get("contract")))
                .containsEntry("schema", AgentRunPlanningContract.SCHEMA)
                .containsEntry("version", AgentRunPlanningContract.VERSION)
                .containsEntry("envelope", AgentRunPlanningContract.RUN_PREFLIGHT);
        assertThat(values)
                .containsEntry("surfaceId", "coding-agent")
                .containsEntry("ready", false);
        assertThat(objectMap(values.get("surfacePolicyAssessment")))
                .containsEntry("ready", false)
                .containsEntry("missingContextKeys", List.of("workspace"));
        assertThat(objectMap(values.get("skillAssessment")))
                .containsEntry("ready", false)
                .containsEntry("unknownSkills", List.of("search"));
    }

    @Test
    void previewEnvelopeOwnsPublishedPlanningShape() {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("workspace", Map.of("rootPath", "/repo"));
        context.put("mode", "assist");

        AgentRunPreview preview = new AgentRunPreview(
                "req-1",
                "tenant-a",
                "model-a",
                "agent-direct",
                "coding-agent",
                "session-a",
                "user-a",
                true,
                42,
                12,
                true,
                7,
                List.of("skill-a"),
                context,
                Map.of("temperature", 0.2),
                new SurfacePolicyAssessment("coding-agent", true, List.of("workspace"), List.of(), List.of(), List.of()),
                new AgentRunSkillAssessment(
                        "coding-agent",
                        true,
                        List.of("skill-a"),
                        List.of("skill-a"),
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()));

        Map<String, Object> values = AgentRunEnvelopes.preview(preview);

        assertThat(objectMap(values.get("contract")))
                .containsEntry("schema", AgentRunPlanningContract.SCHEMA)
                .containsEntry("version", AgentRunPlanningContract.VERSION)
                .containsEntry("envelope", AgentRunPlanningContract.RUN_PREVIEW);
        assertThat(values)
                .containsEntry("requestId", "req-1")
                .containsEntry("tenantId", "tenant-a")
                .containsEntry("modelId", "model-a")
                .containsEntry("workflowId", "agent-direct")
                .containsEntry("surfaceId", "coding-agent")
                .containsEntry("sessionId", "session-a")
                .containsEntry("userId", "user-a")
                .containsEntry("systemPromptPresent", true)
                .containsEntry("promptCharacters", 42)
                .containsEntry("systemPromptCharacters", 12)
                .containsEntry("memoryEnabled", true)
                .containsEntry("maxSteps", 7)
                .containsEntry("workspaceAttached", true)
                .containsEntry("harnessAttached", false)
                .containsEntry("skills", List.of("skill-a"))
                .containsEntry("contextKeys", List.of("workspace", "mode"));
        assertThat(objectMap(values.get("surfacePolicyAssessment"))).containsEntry("ready", true);
        assertThat(objectMap(values.get("skillAssessment"))).containsEntry("resolvedSkillIds", List.of("skill-a"));
    }

    @Test
    void resultEnvelopeOwnsPublishedLifecycleShape() {
        AgentRunResult result = new AgentRunResult(
                "run-1",
                "done",
                true,
                "react",
                List.of("think", "answer"),
                Map.of("profileId", "default"),
                new AgentRunHandle("run-1", AgentRunState.RUNNING, "react"));

        Map<String, Object> values = AgentRunEnvelopes.result(result);

        assertThat(objectMap(values.get("contract")))
                .containsEntry("schema", AgentRunLifecycleContract.SCHEMA)
                .containsEntry("version", AgentRunLifecycleContract.VERSION)
                .containsEntry("envelope", AgentRunLifecycleContract.RUN_RESULT);
        assertThat(values)
                .containsEntry("runId", "run-1")
                .containsEntry("answer", "done")
                .containsEntry("successful", true)
                .containsEntry("outcome", AgentRunOutcomes.PENDING)
                .containsEntry("strategy", "react")
                .containsEntry("steps", List.of("think", "answer"))
                .containsEntry("metadata", Map.of("profileId", "default"));
        assertThat(objectMap(values.get("handle")))
                .containsEntry("runId", "run-1")
                .containsEntry("state", "RUNNING")
                .containsEntry("strategy", "react")
                .containsEntry("terminal", false);
    }

    @Test
    void statusAndHandleFragmentsAreReusableAndImmutable() {
        AgentRunStatus status = new AgentRunStatus(
                AgentRunHandle.completed("run-2", "react"),
                true,
                "complete",
                Map.of("surfaceId", "coding-agent"));

        Map<String, Object> values = AgentRunEnvelopes.status(status);

        assertThat(values)
                .containsEntry("outcome", AgentRunOutcomes.TERMINAL)
                .containsEntry("known", true)
                .containsEntry("message", "complete")
                .containsEntry("metadata", Map.of("surfaceId", "coding-agent"));
        assertThat(objectMap(values.get("handle")))
                .containsEntry("state", "COMPLETED")
                .containsEntry("terminal", true);
        assertThatThrownBy(() -> values.put("extra", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        return (Map<String, Object>) value;
    }
}
