package tech.kayys.wayang.readiness;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.contract.WayangContractIntegrity;
import tech.kayys.wayang.contract.WayangContractIntegrityIssue;
import tech.kayys.wayang.contract.WayangContractIntegrityReport;
import tech.kayys.wayang.client.WayangReadinessAttributeMaps;
import tech.kayys.wayang.client.WayangReadinessReports;

/**
 * Readiness assessor for contract integrity validation.
 * Evaluates whether contracts maintain referential integrity with commands.
 */
public class ContractIntegrityAssessor extends ComponentReadinessAssessor {

    public static final String READINESS_ID = "wayang.contract.integrity.readiness";

    @Override
    protected String getId() {
        return READINESS_ID;
    }

    @Override
    protected String getSource() {
        return "contracts";
    }

    @Override
    protected String buildProbeName() {
        return "contracts.integrity";
    }

    @Override
    protected List<Map<String, Object>> validate(Object input) {
        WayangContractIntegrityReport integrity = (WayangContractIntegrityReport) input;
        WayangContractIntegrityReport resolved = integrity == null
                ? WayangContractIntegrity.validateDefault()
                : integrity;

        return resolved.issues().stream()
                .map(this::issue)
                .toList();
    }

    @Override
    protected Map<String, Object> buildAttributes(Object input) {
        WayangContractIntegrityReport integrity = (WayangContractIntegrityReport) input;
        WayangContractIntegrityReport resolved = integrity == null
                ? WayangContractIntegrity.validateDefault()
                : integrity;

        return WayangReadinessAttributeMaps.ordered(
                "totalContracts", resolved.totalContracts(),
                "totalCommands", resolved.totalCommands(),
                "contractCommandLinks", resolved.contractCommandLinks(),
                "commandContractLinks", resolved.commandContractLinks());
    }

    private Map<String, Object> issue(WayangContractIntegrityIssue issue) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("kind", issue.kind());
        fields.put("schema", issue.schema());
        fields.put("version", issue.version());
        fields.put("envelope", issue.envelope());
        fields.put("commandId", issue.commandId());
        return WayangReadinessReports.issue(
                "contract_integrity_" + issue.kind(),
                getSource(),
                issue.message(),
                fields);
    }
}
