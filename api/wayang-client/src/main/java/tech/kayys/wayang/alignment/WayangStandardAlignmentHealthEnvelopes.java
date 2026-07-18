package tech.kayys.wayang.alignment;

import java.util.LinkedHashMap;
import java.util.Map;

import tech.kayys.wayang.client.SdkMaps;
import tech.kayys.wayang.client.SdkText;

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
