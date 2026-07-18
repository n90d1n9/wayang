package tech.kayys.wayang.alignment;

import java.util.List;
import java.util.Map;

import tech.kayys.wayang.client.SdkLists;

final class WayangStandardAlignmentProviderSummaries {

    private WayangStandardAlignmentProviderSummaries() {
    }

    static List<WayangStandardAlignmentProviderSummary> copy(
            List<WayangStandardAlignmentProviderSummary> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(value -> value != null)
                        .toList();
    }

    static List<String> resolveProviderIds(
            List<String> providerIds,
            List<WayangStandardAlignmentProviderSummary> providerSummaries) {
        List<String> resolved = SdkLists.copy(providerIds);
        if (!resolved.isEmpty()) {
            return resolved;
        }
        return copy(providerSummaries).stream()
                .map(WayangStandardAlignmentProviderSummary::providerId)
                .toList();
    }

    static List<Map<String, Object>> toMaps(List<WayangStandardAlignmentProviderSummary> providerSummaries) {
        return copy(providerSummaries).stream()
                .map(WayangStandardAlignmentProviderSummary::toMap)
                .toList();
    }
}
