package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.gollek.sdk.WayangReadinessReport;
import tech.kayys.wayang.gollek.sdk.WayangReadinessReports;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

record AgenticCommerceConnectorDiagnosticsReadiness(AgenticCommerceConnectorDiagnostics diagnostics) {

    AgenticCommerceConnectorDiagnosticsReadiness {
        diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    static AgenticCommerceConnectorDiagnosticsReadiness from(AgenticCommerceConnectorDiagnostics diagnostics) {
        return new AgenticCommerceConnectorDiagnosticsReadiness(diagnostics);
    }

    WayangReadinessReport toReport() {
        return WayangReadinessReport.from(
                AgenticCommerceConnectorDiagnostics.READINESS_ID,
                diagnostics.ready(),
                WayangReadinessReports.exitCode(diagnostics.ready()),
                diagnostics.issueCount(),
                probes(),
                issues(),
                attributes());
    }

    private List<Map<String, Object>> probes() {
        return List.of(
                WayangReadinessReports.probe(
                        "runtimePreflight",
                        true,
                        diagnostics.preflight().ready(),
                        diagnostics.preflight().errorCount(),
                        diagnostics.preflight().standardReadiness().toMap()),
                contractProbe());
    }

    private List<Map<String, Object>> issues() {
        List<Map<String, Object>> issues = new ArrayList<>(
                diagnostics.preflight().standardReadiness().issues());
        if (diagnostics.contractReport() != null && !diagnostics.contractReport().passed()) {
            issues.add(contractIssue(diagnostics.contractReport()));
        }
        return List.copyOf(issues);
    }

    private Map<String, Object> attributes() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("connectorKind", diagnostics.snapshot().runtimeConfig().connectorFactoryConfig().connectorKind());
        values.put("connectorSupported", diagnostics.preflight().connectorSupported());
        values.put("contractAvailable", diagnostics.contractAvailable());
        values.put("warningCount", diagnostics.preflight().warningCount());
        values.put("preflight", diagnostics.preflight().standardReadiness().toMap());
        if (diagnostics.contractReport() != null) {
            values.put("contract", contractAttributes(diagnostics.contractReport()));
        }
        return AgenticCommerceWayangMaps.copy(values);
    }

    private Map<String, Object> contractProbe() {
        AgenticCommerceConnectorContractReport contract = diagnostics.contractReport();
        if (contract == null) {
            return WayangReadinessReports.probe(
                    "connectorContract",
                    false,
                    true,
                    0,
                    Map.of("contractAvailable", false));
        }
        return WayangReadinessReports.probe(
                "connectorContract",
                false,
                contract.passed(),
                contract.issueCount(),
                contractAttributes(contract));
    }

    private static Map<String, Object> contractIssue(AgenticCommerceConnectorContractReport contract) {
        return WayangReadinessReports.issue(
                "connector_contract_failed",
                "contract",
                "Connector contract did not pass.",
                Map.of(
                        "scenarioId",
                        contract.scenarioResult().scenario().id(),
                        "expectationId",
                        contract.expectationResult().expectation().id(),
                        "issueCount",
                        contract.issueCount()));
    }

    private static Map<String, Object> contractAttributes(AgenticCommerceConnectorContractReport contract) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("passed", contract.passed());
        values.put("connectorName", contract.connectorName());
        values.put("exchangeCount", contract.exchangeCount());
        values.put("issueCount", contract.issueCount());
        values.put("scenarioId", contract.scenarioResult().scenario().id());
        values.put("expectationId", contract.expectationResult().expectation().id());
        AgenticCommerceWayangMaps.putText(
                values,
                "contractId",
                AgenticCommerceWayangMaps.text(contract.attributes().get("contractId")));
        return AgenticCommerceWayangMaps.copy(values);
    }
}
