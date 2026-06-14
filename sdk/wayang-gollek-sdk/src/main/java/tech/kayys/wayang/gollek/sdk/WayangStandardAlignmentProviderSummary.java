package tech.kayys.wayang.gollek.sdk;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON-ready summary for a standard-alignment provider that contributed reports.
 */
public record WayangStandardAlignmentProviderSummary(
        String providerId,
        String providerClass,
        int priority,
        List<String> standardIds,
        boolean aligned,
        int gapCount) {

    public WayangStandardAlignmentProviderSummary {
        providerId = SdkText.trimToDefault(providerId, "unknown");
        providerClass = SdkText.trimToDefault(providerClass, "");
        standardIds = SdkLists.copy(standardIds);
        gapCount = Math.max(0, gapCount);
    }

    public static WayangStandardAlignmentProviderSummary from(
            String providerId,
            String providerClass,
            int priority,
            WayangStandardAlignmentPortfolio portfolio) {
        WayangStandardAlignmentPortfolio resolved = portfolio == null
                ? WayangStandardAlignmentPortfolio.builder().build()
                : portfolio;
        return new WayangStandardAlignmentProviderSummary(
                providerId,
                providerClass,
                priority,
                resolved.standardIds(),
                resolved.aligned(),
                resolved.gapCount());
    }

    public int standardCount() {
        return standardIds.size();
    }

    public boolean hasGaps() {
        return gapCount > 0;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("providerId", providerId);
        values.put("providerClass", providerClass);
        values.put("priority", priority);
        values.put("standardCount", standardCount());
        values.put("standardIds", standardIds);
        values.put("aligned", aligned);
        values.put("gapCount", gapCount);
        return SdkMaps.orderedCopy(values);
    }
}
