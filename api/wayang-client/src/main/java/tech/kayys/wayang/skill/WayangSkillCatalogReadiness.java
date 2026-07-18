package tech.kayys.wayang.skill;

import tech.kayys.wayang.agent.skill.AgentSkillDiscovery;
import tech.kayys.wayang.readiness.SkillCatalogAssessor;
import tech.kayys.wayang.readiness.WayangReadinessReport;

/**
 * Backward compatibility wrapper for skill catalog readiness assessment.
 * This class maintains the original API while delegating to the new assessor framework.
 */
public final class WayangSkillCatalogReadiness {

    public static final String READINESS_ID = SkillCatalogAssessor.READINESS_ID;

    private static final SkillCatalogAssessor ASSESSOR = new SkillCatalogAssessor();

    private WayangSkillCatalogReadiness() {
    }

    public static WayangReadinessReport assess(AgentSkillDiscovery discovery) {
        return ASSESSOR.assess(discovery);
    }
}
