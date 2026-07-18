package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangSdkBoundary;

import java.util.List;

/**
 * Text renderer for SDK ownership boundaries shown by the Wayang CLI.
 */
final class WayangSdkBoundaryTextFormat {

    private static final String NL = System.lineSeparator();

    private WayangSdkBoundaryTextFormat() {
    }

    static String text(List<WayangSdkBoundary> boundaries) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang SDK boundaries").append(NL);
        output.append("Root package: tech.kayys.wayang.gollek.sdk").append(NL).append(NL);
        for (WayangSdkBoundary boundary : CliLists.copy(boundaries)) {
            appendSummary(output, boundary);
        }
        return output.toString();
    }

    static String detailText(WayangSdkBoundary boundary) {
        StringBuilder output = new StringBuilder();
        output.append("Wayang SDK boundary").append(NL);
        appendSummary(output, boundary);
        CliText.appendListLine(output, "class prefixes", boundary.classPrefixes());
        CliText.appendListLine(output, "contract schemas", boundary.contractSchemas());
        CliText.appendListLine(output, "depends on", boundary.dependsOn());
        return output.toString();
    }

    private static void appendSummary(StringBuilder output, WayangSdkBoundary boundary) {
        output.append(boundary.name())
                .append(" (")
                .append(boundary.id())
                .append(")")
                .append(NL);
        output.append("  package: ").append(boundary.intendedPackage()).append(NL);
        output.append("  owns: ").append(boundary.responsibility()).append(NL);
    }
}
