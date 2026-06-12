package tech.kayys.wayang.a2a.wayang;

import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPortfolio;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentProvider;

public final class WayangA2aStandardAlignmentProvider implements WayangStandardAlignmentProvider {

    @Override
    public String providerId() {
        return "wayang-a2a-standard-alignment";
    }

    @Override
    public WayangStandardAlignmentPortfolio portfolio() {
        return WayangA2aStandardAlignment.portfolio();
    }
}
