package tech.kayys.wayang.agenticcommerce.wayang;

import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPortfolio;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentProvider;

public final class AgenticCommerceWayangStandardAlignmentProvider implements WayangStandardAlignmentProvider {

    @Override
    public String providerId() {
        return "wayang-agentic-commerce-standard-alignment";
    }

    @Override
    public WayangStandardAlignmentPortfolio portfolio() {
        return AgenticCommerceWayangStandardAlignment.portfolio();
    }
}
