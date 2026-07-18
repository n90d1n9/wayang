package tech.kayys.wayang.readiness;


/**
 * Backward compatibility wrapper for registry readiness assessment.
 * This class maintains the original API while delegating to the new assessor framework.
 */
public final class WayangPlatformReadinessProfileRegistryReadiness {

    public static final String READINESS_ID = PlatformRegistryAssessor.READINESS_ID;

    private static final PlatformRegistryAssessor ASSESSOR = new PlatformRegistryAssessor();

    private WayangPlatformReadinessProfileRegistryReadiness() {
    }

    public static WayangReadinessReport assess(
            WayangPlatformReadinessProfileRegistryResolution resolution) {
        return ASSESSOR.assess(resolution);
    }
}
