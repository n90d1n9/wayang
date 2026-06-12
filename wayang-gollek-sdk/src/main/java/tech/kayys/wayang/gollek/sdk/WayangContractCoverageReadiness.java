package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WayangContractCoverageReadiness {

    public static final String READINESS_ID = "wayang.contract.coverage.readiness";

    private WayangContractCoverageReadiness() {
    }

    public static WayangReadinessReport assess(WayangContractCommandCoverageReport coverage) {
        WayangContractCommandCoverageReport resolved = coverage == null
                ? WayangContractCommandCoverage.defaultCoverage()
                : coverage;
        List<Map<String, Object>> issues = resolved.incompleteEntries().stream()
                .map(WayangContractCoverageReadiness::issue)
                .toList();
        boolean ready = issues.isEmpty();
        Map<String, Object> attributes = attributes(resolved);
        return WayangReadinessReport.from(
                READINESS_ID,
                ready,
                WayangReadinessReports.exitCode(ready),
                issues.size(),
                List.of(WayangReadinessReports.probe(
                        "contracts.command_coverage",
                        true,
                        ready,
                        issues.size(),
                        attributes)),
                issues,
                attributes);
    }

    private static Map<String, Object> attributes(WayangContractCommandCoverageReport report) {
        return WayangReadinessAttributeMaps.ordered(
                "totalContracts", report.totalContracts(),
                "totalCommands", report.totalCommands(),
                "commandLinkedContracts", report.commandLinkedContracts(),
                "commandlessContracts", report.commandlessContracts(),
                "incompleteContracts", report.incompleteContracts(),
                "commandContractLinks", report.commandContractLinks());
    }

    private static Map<String, Object> issue(WayangContractCommandCoverageEntry entry) {
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
                "contracts",
                message,
                fields);
    }
}
