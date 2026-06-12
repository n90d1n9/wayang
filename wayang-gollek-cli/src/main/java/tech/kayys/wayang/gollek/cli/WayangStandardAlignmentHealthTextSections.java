package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPolicyAssessment;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPortfolio;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentProviderDiagnostics;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentProviderIssue;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentProviderSummary;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentSummary;
import tech.kayys.wayang.gollek.sdk.WayangStandardRegistryDriftIssue;
import tech.kayys.wayang.gollek.sdk.WayangStandardRegistryDriftReport;

/**
 * Reusable section renderers for standard-alignment health terminal output.
 */
final class WayangStandardAlignmentHealthTextSections {

    private static final String NL = System.lineSeparator();

    private WayangStandardAlignmentHealthTextSections() {
    }

    static void appendStandards(StringBuilder output, WayangStandardAlignmentPortfolio portfolio) {
        if (portfolio.standards().isEmpty()) {
            return;
        }
        output.append("standards detail:").append(NL);
        for (WayangStandardAlignmentSummary summary : portfolio.standards()) {
            output.append("- ")
                    .append(summary.standardId())
                    .append(" ")
                    .append(summary.standard().version())
                    .append(" aligned=")
                    .append(CliText.yesNo(summary.aligned() && !summary.hasGaps()))
                    .append(" requirements=")
                    .append(summary.alignedCount())
                    .append("/")
                    .append(summary.requirementCount())
                    .append(" gaps=")
                    .append(summary.gapCount());
            if (!summary.gapCategories().isEmpty()) {
                output.append(" categories=").append(String.join(",", summary.gapCategories()));
            }
            output.append(NL);
        }
    }

    static void appendProviderSummaries(
            StringBuilder output,
            WayangStandardAlignmentProviderDiagnostics providers) {
        if (providers.providers().isEmpty()) {
            return;
        }
        output.append("provider detail:").append(NL);
        for (WayangStandardAlignmentProviderSummary summary : providers.providers()) {
            output.append("- ")
                    .append(summary.providerId())
                    .append(" priority=")
                    .append(summary.priority())
                    .append(" standards=")
                    .append(summary.standardCount())
                    .append(" aligned=")
                    .append(CliText.yesNo(summary.aligned() && !summary.hasGaps()))
                    .append(" gaps=")
                    .append(summary.gapCount());
            if (!summary.standardIds().isEmpty()) {
                output.append(" ids=").append(String.join(",", summary.standardIds()));
            }
            output.append(NL);
        }
    }

    static void appendVersionMismatches(
            StringBuilder output,
            WayangStandardAlignmentPolicyAssessment policy) {
        if (policy.versionMismatchStandardIds().isEmpty()) {
            return;
        }
        output.append("version mismatches:").append(NL);
        for (String standardId : policy.versionMismatchStandardIds()) {
            output.append("- ")
                    .append(standardId)
                    .append(" expected=")
                    .append(policy.requiredVersions().getOrDefault(standardId, ""))
                    .append(" actual=")
                    .append(policy.actualVersions().getOrDefault(standardId, ""))
                    .append(NL);
        }
    }

    static void appendDriftIssues(StringBuilder output, WayangStandardRegistryDriftReport drift) {
        CliText.appendBulletBlockIfAny(output, "registry unknown standards", drift.unknownStandardIds());
        if (drift.issues().isEmpty()) {
            return;
        }
        output.append("registry drift detail:").append(NL);
        for (WayangStandardRegistryDriftIssue issue : drift.issues()) {
            output.append("- ")
                    .append(issue.standardId())
                    .append(" ")
                    .append(issue.field())
                    .append(" expected=")
                    .append(issue.expected())
                    .append(" actual=")
                    .append(issue.actual())
                    .append(NL);
        }
    }

    static void appendProviderIssues(
            StringBuilder output,
            WayangStandardAlignmentProviderDiagnostics providers) {
        if (providers.issues().isEmpty()) {
            return;
        }
        output.append("provider issue detail:").append(NL);
        for (WayangStandardAlignmentProviderIssue issue : providers.issues()) {
            output.append("- ")
                    .append(issue.providerId())
                    .append(" ")
                    .append(issue.providerClass())
                    .append(": ")
                    .append(issue.message())
                    .append(NL);
        }
    }

}
