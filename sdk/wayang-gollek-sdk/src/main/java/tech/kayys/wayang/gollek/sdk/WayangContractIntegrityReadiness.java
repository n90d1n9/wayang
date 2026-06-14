package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WayangContractIntegrityReadiness {

    public static final String READINESS_ID = "wayang.contract.integrity.readiness";

    private WayangContractIntegrityReadiness() {
    }

    public static WayangReadinessReport assess(WayangContractIntegrityReport integrity) {
        WayangContractIntegrityReport resolved = integrity == null
                ? WayangContractIntegrity.validateDefault()
                : integrity;
        boolean ready = resolved.valid();
        List<Map<String, Object>> issues = resolved.issues().stream()
                .map(WayangContractIntegrityReadiness::issue)
                .toList();
        return WayangReadinessReport.from(
                READINESS_ID,
                ready,
                WayangReadinessReports.exitCode(ready),
                issues.size(),
                List.of(WayangReadinessReports.probe(
                        "contracts.integrity",
                        true,
                        ready,
                        issues.size(),
                        attributes(resolved))),
                issues,
                attributes(resolved));
    }

    private static Map<String, Object> attributes(WayangContractIntegrityReport report) {
        return WayangReadinessAttributeMaps.ordered(
                "totalContracts", report.totalContracts(),
                "totalCommands", report.totalCommands(),
                "contractCommandLinks", report.contractCommandLinks(),
                "commandContractLinks", report.commandContractLinks());
    }

    private static Map<String, Object> issue(WayangContractIntegrityIssue issue) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("kind", issue.kind());
        fields.put("schema", issue.schema());
        fields.put("version", issue.version());
        fields.put("envelope", issue.envelope());
        fields.put("commandId", issue.commandId());
        return WayangReadinessReports.issue(
                "contract_integrity_" + issue.kind(),
                "contracts",
                issue.message(),
                fields);
    }
}
