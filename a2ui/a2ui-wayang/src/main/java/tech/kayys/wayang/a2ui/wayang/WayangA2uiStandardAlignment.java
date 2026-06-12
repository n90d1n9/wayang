package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentHealthReport;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPolicyConfig;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPortfolio;

/**
 * SDK bridge for A2UI standard-alignment reports.
 */
public final class WayangA2uiStandardAlignment {

    private WayangA2uiStandardAlignment() {
    }

    public static WayangStandardAlignmentPortfolio portfolio() {
        return portfolio(WayangA2uiSpecAlignmentReport.defaultReport());
    }

    public static WayangStandardAlignmentPortfolio portfolio(WayangA2uiSpecAlignmentReport report) {
        return WayangStandardAlignmentPortfolio.fromReportMaps(resolve(report).toMap());
    }

    public static WayangStandardAlignmentHealthReport pinnedHealth() {
        return pinnedHealth(WayangA2uiSpecAlignmentReport.defaultReport());
    }

    public static WayangStandardAlignmentHealthReport pinnedHealth(WayangA2uiSpecAlignmentReport report) {
        return WayangStandardAlignmentHealthReport.fromPinnedRegistry(
                portfolio(report),
                WayangA2uiSpecAlignmentReport.STANDARD_ID);
    }

    public static WayangStandardAlignmentHealthReport health(WayangStandardAlignmentPolicyConfig config) {
        return health(WayangA2uiSpecAlignmentReport.defaultReport(), config);
    }

    public static WayangStandardAlignmentHealthReport health(
            WayangA2uiSpecAlignmentReport report,
            WayangStandardAlignmentPolicyConfig config) {
        return WayangStandardAlignmentHealthReport.fromConfiguredPolicy(portfolio(report), config);
    }

    private static WayangA2uiSpecAlignmentReport resolve(WayangA2uiSpecAlignmentReport report) {
        return report == null ? WayangA2uiSpecAlignmentReport.defaultReport() : report;
    }
}
