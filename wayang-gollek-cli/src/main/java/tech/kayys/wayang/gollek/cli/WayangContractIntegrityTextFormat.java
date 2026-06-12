package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangContractIntegrityIssue;
import tech.kayys.wayang.gollek.sdk.WayangContractIntegrityReport;
import tech.kayys.wayang.gollek.sdk.WayangContractEnvelopes;

/**
 * Plain-text renderer for contract integrity diagnostics.
 *
 * <p>The renderer owns terminal-facing labels while SDK envelopes provide the
 * normalized integrity report model used by JSON and text surfaces.</p>
 */
final class WayangContractIntegrityTextFormat {

    private WayangContractIntegrityTextFormat() {
    }

    static String text(String productName, WayangContractIntegrityReport report) {
        WayangContractIntegrityReport model = WayangContractEnvelopes.normalizeIntegrityReport(report);
        StringBuilder output = new StringBuilder(productName).append(" contract integrity").append(System.lineSeparator());
        output.append("valid: ").append(model.valid()).append(System.lineSeparator());
        output.append("issues: ").append(model.issueCount()).append(System.lineSeparator());
        output.append("contracts: ").append(model.totalContracts()).append(System.lineSeparator());
        output.append("commands: ").append(model.totalCommands()).append(System.lineSeparator());
        output.append("contractCommandLinks: ").append(model.contractCommandLinks()).append(System.lineSeparator());
        output.append("commandContractLinks: ").append(model.commandContractLinks()).append(System.lineSeparator());
        if (!model.issues().isEmpty()) {
            output.append(System.lineSeparator()).append("Issues").append(System.lineSeparator());
            for (WayangContractIntegrityIssue issue : model.issues()) {
                output.append("  - ")
                        .append(issue.kind())
                        .append(": ")
                        .append(issue.message())
                        .append(System.lineSeparator());
            }
        }
        return output.toString();
    }
}
