package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangContractCommandCoverageEntry;
import tech.kayys.wayang.gollek.sdk.WayangContractCommandCoverageReport;
import tech.kayys.wayang.gollek.sdk.WayangContractEnvelopes;

import java.util.List;

/**
 * Plain-text renderer for contract command coverage diagnostics.
 *
 * <p>The renderer keeps terminal formatting local to the CLI while relying on
 * SDK contract envelopes for normalized report defaults.</p>
 */
final class WayangContractCoverageTextFormat {

    private WayangContractCoverageTextFormat() {
    }

    static String text(String productName, WayangContractCommandCoverageReport report) {
        WayangContractCommandCoverageReport model = WayangContractEnvelopes.normalizeCoverageReport(report);
        StringBuilder output = new StringBuilder(productName).append(" contract command coverage")
                .append(System.lineSeparator());
        output.append("contracts: ").append(model.totalContracts()).append(System.lineSeparator());
        output.append("commands: ").append(model.totalCommands()).append(System.lineSeparator());
        output.append("commandLinkedContracts: ").append(model.commandLinkedContracts()).append(System.lineSeparator());
        output.append("commandlessContracts: ").append(model.commandlessContracts()).append(System.lineSeparator());
        output.append("incompleteContracts: ").append(model.incompleteContracts()).append(System.lineSeparator());
        output.append("commandContractLinks: ").append(model.commandContractLinks()).append(System.lineSeparator());
        appendEntries(output, "Commandless contracts", model.commandlessEntries());
        appendEntries(output, "Incomplete contracts", model.incompleteEntries());
        return output.toString();
    }

    private static void appendEntries(
            StringBuilder output,
            String title,
            List<WayangContractCommandCoverageEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }
        output.append(System.lineSeparator()).append(title).append(System.lineSeparator());
        for (WayangContractCommandCoverageEntry entry : entries) {
            output.append("  - ")
                    .append(entry.schema())
                    .append("/")
                    .append(entry.envelope())
                    .append(" v")
                    .append(entry.version())
                    .append(System.lineSeparator());
        }
    }
}
