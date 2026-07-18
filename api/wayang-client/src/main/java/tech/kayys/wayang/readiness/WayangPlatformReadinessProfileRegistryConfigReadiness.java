package tech.kayys.wayang.readiness;


/**
 * Backward compatibility wrapper for registry config readiness assessment.
 * This class maintains the original API while delegating to the new assessor framework.
 */
public final class WayangPlatformReadinessProfileRegistryConfigReadiness {

    public static final String READINESS_ID = PlatformRegistryConfigAssessor.READINESS_ID;

    private static final PlatformRegistryConfigAssessor ASSESSOR = new PlatformRegistryConfigAssessor();

    private WayangPlatformReadinessProfileRegistryConfigReadiness() {
    }

    public static WayangReadinessReport assess(
            WayangPlatformReadinessProfileRegistryConfigDiagnostics diagnostics) {
        return ASSESSOR.assess(diagnostics);
    }
}
