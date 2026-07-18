package tech.kayys.wayang.storage;

import tech.kayys.wayang.readiness.StorageReadinessAssessor;
import tech.kayys.wayang.readiness.WayangReadinessReport;

/**
 * Backward compatibility wrapper for storage readiness assessment.
 * This class maintains the original API while delegating to the new assessor framework.
 */
public final class WayangStorageReadiness {

    public static final String READINESS_ID = StorageReadinessAssessor.READINESS_ID;

    private static final StorageReadinessAssessor ASSESSOR = new StorageReadinessAssessor();

    private WayangStorageReadiness() {
    }

    public static WayangReadinessReport assess(WayangStorageConfig storage) {
        return ASSESSOR.assess(storage);
    }
}
