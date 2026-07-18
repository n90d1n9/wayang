package tech.kayys.wayang.capability;

import tech.kayys.wayang.readiness.ProviderCapabilityAssessor;
import tech.kayys.wayang.readiness.WayangReadinessReport;

/**
 * Backward compatibility wrapper for provider capability readiness assessment.
 * This class maintains the original API while delegating to the new assessor framework.
 */
public final class WayangProviderCapabilityReadiness {

    public static final String READINESS_ID = ProviderCapabilityAssessor.READINESS_ID;

    private static final ProviderCapabilityAssessor ASSESSOR = new ProviderCapabilityAssessor();

    private WayangProviderCapabilityReadiness() {
    }

    public static WayangReadinessReport assess(WayangProviderCapabilityDiscovery discovery) {
        return ASSESSOR.assess(discovery);
    }
}
