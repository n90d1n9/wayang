package tech.kayys.wayang.readiness;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.agent.run.AgentRunReadiness;
import tech.kayys.wayang.agent.run.AgentRunRequest;
import tech.kayys.wayang.agent.skill.AgentRunSkillAssessment;
import tech.kayys.wayang.agent.skill.AgentRunSkillPreflight;
import tech.kayys.wayang.client.WayangProductCatalog;
import tech.kayys.wayang.policy.SurfacePolicyAssessment;
import tech.kayys.wayang.policy.SurfacePolicyPreflight;
import tech.kayys.wayang.skill.WayangSkillCatalog;

/**
 * Readiness assessor for agent run execution prerequisites.
 * Evaluates whether prerequisites for running an agent are satisfied.
 */
public class AgentRunAssessor extends ComponentReadinessAssessor {

    public static final String READINESS_ID = "wayang.agent-run.readiness";

    @Override
    protected String getId() {
        return READINESS_ID;
    }

    @Override
    protected String getSource() {
        return "agent-run";
    }

    @Override
    protected String buildProbeName() {
        return "agent.run_readiness";
    }

    @Override
    protected List<Map<String, Object>> validate(Object input) {
        if (!(input instanceof AgentRunRequest)) {
            return List.of();
        }

        AgentRunRequest request = (AgentRunRequest) input;
        AgentRunRequest normalized = AgentRunRequest.builder(request).build();

        SurfacePolicyAssessment surface = SurfacePolicyPreflight.assess(normalized);
        AgentRunSkillAssessment skills = AgentRunSkillPreflight.assess(
                WayangSkillCatalog.defaultRegistry(), normalized);

        List<Map<String, Object>> issues = new ArrayList<>();
        if (!surface.ready()) {
            issues.add(WayangReadinessReports.issue(
                    "agent_run_surface_not_ready",
                    getSource(),
                    "Agent run surface policy assessment failed.",
                    WayangReadinessAttributeMaps.ordered(
                            "surfaceId", normalized.surfaceId(),
                            "message", surface.message())));
        }
        if (!skills.ready()) {
            issues.add(WayangReadinessReports.issue(
                    "agent_run_skills_not_ready",
                    getSource(),
                    "Agent run skill assessment failed.",
                    WayangReadinessAttributeMaps.ordered(
                            "surfaceId", normalized.surfaceId(),
                            "message", skills.message())));
        }
        return issues;
    }

    @Override
    protected Map<String, Object> buildAttributes(Object input) {
        if (!(input instanceof AgentRunRequest)) {
            return WayangReadinessAttributeMaps.ordered();
        }

        AgentRunRequest request = (AgentRunRequest) input;
        AgentRunRequest normalized = AgentRunRequest.builder(request).build();
        String normalizedSurfaceId = WayangProductCatalog.normalizeSurfaceId(normalized.surfaceId());

        return WayangReadinessAttributeMaps.ordered(
                "surfaceId", normalizedSurfaceId,
                "normalized", true);
    }
}
