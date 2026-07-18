package tech.kayys.wayang.readiness;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tech.kayys.wayang.contract.WayangContractCommandCoverage;
import tech.kayys.wayang.contract.WayangContractCommandCoverageEntry;
import tech.kayys.wayang.contract.WayangContractCommandCoverageReport;
import tech.kayys.wayang.client.WayangReadinessAttributeMaps;
import tech.kayys.wayang.client.WayangReadinessReports;

/**
 * Readiness assessor for contract command coverage.
 * Evaluates whether commands are properly linked to contracts.
 */
public class ContractCoverageAssessor extends ComponentReadinessAssessor {

    public static final String READINESS_ID = "wayang.contract.coverage.readiness";

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
        return "contracts.command_coverage";
    }

    @Override
    protected List<Map<String, Object>> validate(Object input) {
        WayangContractCommandCoverageReport coverage = (WayangContractCommandCoverageReport) input;
        WayangContractCommandCoverageReport resolved = coverage == null
                ? WayangContractCommandCoverage.defaultCoverage()
                : coverage;

        return resolved.incompleteEntries().stream()
                .map(this::issue)
                .toList();
    }

    @Override
    protected Map<String, Object> buildAttributes(Object input) {
        WayangContractCommandCoverageReport coverage = (WayangContractCommandCoverageReport) input;
        WayangContractCommandCoverageReport resolved = coverage == null
                ? WayangContractCommandCoverage.defaultCoverage()
                : coverage;

        return WayangReadinessAttributeMaps.ordered(
                "totalContracts", resolved.totalContracts(),
                "totalCommands", resolved.totalCommands(),
                "commandLinkedContracts", resolved.commandLinkedContracts(),
                "commandlessContracts", resolved.commandlessContracts(),
                "incompleteContracts", resolved.incompleteContracts(),
                "commandContractLinks", resolved.commandContractLinks());
    }

    private Map<String, Object> issue(WayangContractCommandCoverageEntry entry) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("jsonSchemaId", entry.jsonSchemaId());
        fields.put("schema", entry.schema());
        fields.put("version", entry.version());
        fields.put("envelope", entry.envelope());
        fields.put("unlinkedCommandIds", entry.unlinkedCommandIds());
        fields.put("undeclaredLinkedCommandIds", entry.undeclaredLinkedCommandIds());
        List<String> messages = new ArrayList<>();
        if (!entry.unlinkedCommandIds().isEmpty()) {
            messages.add("declared commands are not linked");
        }
        if (!entry.undeclaredLinkedCommandIds().isEmpty()) {
            messages.add("linked commands are not declared");
        }
        String message = messages.isEmpty()
                ? "Contract command coverage is incomplete."
                : "Contract command coverage is incomplete: " + String.join(", ", messages) + ".";
        return WayangReadinessReports.issue(
                "contract_command_coverage_incomplete",
                getSource(),
                message,
                fields);
    }
}
