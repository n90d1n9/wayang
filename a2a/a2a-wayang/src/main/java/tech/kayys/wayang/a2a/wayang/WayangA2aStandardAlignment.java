package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentHealthReport;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPolicyConfig;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPortfolio;

/**
 * SDK bridge for A2A standard-alignment reports.
 */
public final class WayangA2aStandardAlignment {

    private WayangA2aStandardAlignment() {
    }

    public static WayangStandardAlignmentPortfolio portfolio() {
        return portfolio(WayangA2aSpecAlignmentReport.defaults());
    }

    public static WayangStandardAlignmentPortfolio portfolio(WayangA2aSpecAlignmentReport report) {
        return WayangStandardAlignmentPortfolio.fromReportMaps(resolve(report).toMap());
    }

    public static WayangStandardAlignmentHealthReport pinnedHealth() {
        return pinnedHealth(WayangA2aSpecAlignmentReport.defaults());
    }

    public static WayangStandardAlignmentHealthReport pinnedHealth(WayangA2aSpecAlignmentReport report) {
        return WayangStandardAlignmentHealthReport.fromPinnedRegistry(
                portfolio(report),
                WayangA2aSpecAlignmentReport.STANDARD_ID);
    }

    public static WayangStandardAlignmentHealthReport health(WayangStandardAlignmentPolicyConfig config) {
        return health(WayangA2aSpecAlignmentReport.defaults(), config);
    }

    public static WayangStandardAlignmentHealthReport health(
            WayangA2aSpecAlignmentReport report,
            WayangStandardAlignmentPolicyConfig config) {
        return WayangStandardAlignmentHealthReport.fromConfiguredPolicy(portfolio(report), config);
    }

    private static WayangA2aSpecAlignmentReport resolve(WayangA2aSpecAlignmentReport report) {
        return report == null ? WayangA2aSpecAlignmentReport.defaults() : report;
    }
}
