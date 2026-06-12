package tech.kayys.wayang.gollek.cli;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class WayangCliContextEntries {

    private WayangCliContextEntries() {
    }

    static Map<String, Object> parse(List<String> entries) {
        Map<String, Object> context = new LinkedHashMap<>();
        if (entries == null || entries.isEmpty()) {
            return context;
        }
        for (String entry : entries) {
            String normalized = entry == null ? "" : entry.trim();
            int separator = normalized.indexOf('=');
            if (separator <= 0) {
                throw new IllegalArgumentException("Context entries must use key=value: " + normalized);
            }
            String key = normalized.substring(0, separator).trim();
            String value = normalized.substring(separator + 1).trim();
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Context entries must include a non-empty key.");
            }
            context.put(key, value);
        }
        return context;
    }
}
