package tech.kayys.wayang.contract;

import tech.kayys.wayang.readiness.ContractCoverageAssessor;
import tech.kayys.wayang.readiness.WayangReadinessReport;

/**
 * Backward compatibility wrapper for contract coverage readiness assessment.
 * This class maintains the original API while delegating to the new assessor framework.
 */
public final class WayangContractCoverageReadiness {

    public static final String READINESS_ID = ContractCoverageAssessor.READINESS_ID;

    private static final ContractCoverageAssessor ASSESSOR = new ContractCoverageAssessor();

    private WayangContractCoverageReadiness() {
    }

    public static WayangReadinessReport assess(WayangContractCommandCoverageReport coverage) {
        return ASSESSOR.assess(coverage);
    }
}
