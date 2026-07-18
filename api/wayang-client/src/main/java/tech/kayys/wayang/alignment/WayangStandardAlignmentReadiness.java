package tech.kayys.wayang.alignment;

import tech.kayys.wayang.readiness.StandardAlignmentAssessor;
import tech.kayys.wayang.readiness.WayangReadinessReport;

/**
 * Backward compatibility wrapper for standard alignment readiness assessment.
 * This class maintains the original API while delegating to the new assessor framework.
 */
public final class WayangStandardAlignmentReadiness {

    public static final String READINESS_ID = StandardAlignmentAssessor.READINESS_ID;

    private static final StandardAlignmentAssessor ASSESSOR = new StandardAlignmentAssessor();

    private WayangStandardAlignmentReadiness() {
    }

    public static WayangReadinessReport assess(WayangStandardAlignmentHealthReport health) {
        return ASSESSOR.assess(health);
    }
}
