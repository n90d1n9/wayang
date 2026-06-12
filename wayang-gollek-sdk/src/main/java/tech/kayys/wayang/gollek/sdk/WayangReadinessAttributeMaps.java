package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.Map;

final class WayangReadinessAttributeMaps {

    private WayangReadinessAttributeMaps() {
    }

    static Map<String, Object> ordered(Object... entries) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index + 1 < entries.length; index += 2) {
            values.put(String.valueOf(entries[index]), entries[index + 1]);
        }
        return WayangReportMaps.copyMap(values);
    }
}
