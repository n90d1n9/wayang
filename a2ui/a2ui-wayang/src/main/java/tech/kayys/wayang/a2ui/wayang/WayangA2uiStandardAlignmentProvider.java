package tech.kayys.wayang.a2ui.wayang;

import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentPortfolio;
import tech.kayys.wayang.gollek.sdk.WayangStandardAlignmentProvider;

public final class WayangA2uiStandardAlignmentProvider implements WayangStandardAlignmentProvider {

    @Override
    public String providerId() {
        return "wayang-a2ui-standard-alignment";
    }

    @Override
    public WayangStandardAlignmentPortfolio portfolio() {
        return WayangA2uiStandardAlignment.portfolio();
    }
}
