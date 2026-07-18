package tech.kayys.wayang.contract;

import tech.kayys.wayang.readiness.ContractIntegrityAssessor;
import tech.kayys.wayang.readiness.WayangReadinessReport;

/**
 * Backward compatibility wrapper for contract integrity readiness assessment.
 * This class maintains the original API while delegating to the new assessor framework.
 */
public final class WayangContractIntegrityReadiness {

    public static final String READINESS_ID = ContractIntegrityAssessor.READINESS_ID;

    private static final ContractIntegrityAssessor ASSESSOR = new ContractIntegrityAssessor();

    private WayangContractIntegrityReadiness() {
    }

    public static WayangReadinessReport assess(WayangContractIntegrityReport integrity) {
        return ASSESSOR.assess(integrity);
    }
}
