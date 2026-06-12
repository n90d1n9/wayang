package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WayangStandardAlignmentHealthEnvelopes {

    private WayangStandardAlignmentHealthEnvelopes() {
    }

    public static Map<String, Object> health(String productName, WayangStandardAlignmentHealthReport health) {
        WayangStandardAlignmentHealthReport model = normalize(health);
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("product", SdkText.trimToEmpty(productName));
        values.put("health", model.toMap());
        return SdkMaps.orderedCopy(values);
    }

    public static WayangStandardAlignmentHealthReport normalize(WayangStandardAlignmentHealthReport health) {
        return health == null ? WayangStandardAlignmentHealthReport.from(null) : health;
    }
}
