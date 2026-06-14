package tech.kayys.wayang.gollek.sdk;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SurfacePolicyPreflightTest {

    @Test
    void reportsMissingCodingAgentWorkspaceAndRecommendations() {
        SurfacePolicyAssessment assessment = SurfacePolicyPreflight.assess(AgentRunRequest.builder()
                .prompt("plan")
                .build());

        assertThat(assessment.surfaceId()).isEqualTo("coding-agent");
        assertThat(assessment.ready()).isFalse();
        assertThat(assessment.satisfiedContextKeys()).contains("surfaceId");
        assertThat(assessment.missingContextKeys()).containsExactly("workspace");
        assertThat(assessment.recommendations())
                .contains("Attach workspace context with --workspace <path>.")
                .contains("Attach planned verification checks with --harness.")
                .contains("Consider surface skills: repo, tools, patching.");
        assertThat(assessment.routingHints()).contains("inspect-workspace", "plan-harness");
    }

    @Test
    void marksCodingAgentReadyWhenRequiredContextIsPresent() {
        SurfacePolicyAssessment assessment = SurfacePolicyPreflight.assess(AgentRunRequest.builder()
                .prompt("plan")
                .workspace(".")
                .harness(true)
                .skill("repo")
                .build());

        assertThat(assessment.ready()).isTrue();
        assertThat(assessment.satisfiedContextKeys()).containsExactly("surfaceId", "workspace");
        assertThat(assessment.missingContextKeys()).isEmpty();
        assertThat(assessment.recommendations()).isEmpty();
    }

    @Test
    void reportsMissingWorkflowForWorkflowSurface() {
        SurfacePolicyAssessment assessment = SurfacePolicyPreflight.assess(AgentRunRequest.builder()
                .prompt("plan")
                .surfaceId("workflow-platform")
                .build());

        assertThat(assessment.ready()).isFalse();
        assertThat(assessment.missingContextKeys()).containsExactly("workflowId");
        assertThat(assessment.recommendations())
                .contains("Set a workflow id with --workflow <id>.")
                .contains("Consider surface skills: workflow, hitl, observability.");
    }

    @Test
    void rejectsUnknownSurfaceIds() {
        AgentRunRequest request = AgentRunRequest.builder()
                .prompt("plan")
                .surfaceId("future-agent")
                .build();

        assertThatThrownBy(() -> SurfacePolicyPreflight.assess(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown Wayang product surface 'future-agent'");
    }
}
