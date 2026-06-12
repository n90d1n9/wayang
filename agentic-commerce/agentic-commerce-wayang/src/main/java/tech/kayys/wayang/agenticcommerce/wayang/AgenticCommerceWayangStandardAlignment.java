package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.agenticcommerce.core.AgenticCommerceSpecAlignmentReport;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentHealthReport;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPolicyConfig;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPortfolio;

/**
 * SDK bridge for Agentic Commerce standard-alignment reports.
 */
public final class AgenticCommerceWayangStandardAlignment {

    private AgenticCommerceWayangStandardAlignment() {
    }

    public static WayangStandardAlignmentPortfolio portfolio() {
        return portfolio(AgenticCommerceSpecAlignmentReport.checkout());
    }

    public static WayangStandardAlignmentPortfolio portfolio(AgenticCommerceSpecAlignmentReport report) {
        return WayangStandardAlignmentPortfolio.fromReportMaps(resolve(report).toMap());
    }

    public static WayangStandardAlignmentHealthReport pinnedHealth() {
        return pinnedHealth(AgenticCommerceSpecAlignmentReport.checkout());
    }

    public static WayangStandardAlignmentHealthReport pinnedHealth(AgenticCommerceSpecAlignmentReport report) {
        return WayangStandardAlignmentHealthReport.fromPinnedRegistry(
                portfolio(report),
                AgenticCommerceSpecAlignmentReport.STANDARD_ID);
    }

    public static WayangStandardAlignmentHealthReport health(WayangStandardAlignmentPolicyConfig config) {
        return health(AgenticCommerceSpecAlignmentReport.checkout(), config);
    }

    public static WayangStandardAlignmentHealthReport health(
            AgenticCommerceSpecAlignmentReport report,
            WayangStandardAlignmentPolicyConfig config) {
        return WayangStandardAlignmentHealthReport.fromConfiguredPolicy(portfolio(report), config);
    }

    private static AgenticCommerceSpecAlignmentReport resolve(AgenticCommerceSpecAlignmentReport report) {
        return report == null ? AgenticCommerceSpecAlignmentReport.checkout() : report;
    }
}
