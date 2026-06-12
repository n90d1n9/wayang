package tech.kayys.wayang.gollek.sdk;

import java.util.List;
import java.util.Map;

final class WayangStandardAlignmentProviderIssues {

    private WayangStandardAlignmentProviderIssues() {
    }

    static List<WayangStandardAlignmentProviderIssue> copy(
            List<WayangStandardAlignmentProviderIssue> values) {
        return values == null
                ? List.of()
                : values.stream()
                        .filter(value -> value != null)
                        .toList();
    }

    static List<Map<String, Object>> toMaps(List<WayangStandardAlignmentProviderIssue> issues) {
        return copy(issues).stream()
                .map(WayangStandardAlignmentProviderIssue::toMap)
                .toList();
    }

    static List<String> recommendations(List<WayangStandardAlignmentProviderIssue> issues) {
        return copy(issues).stream()
                .map(WayangStandardAlignmentProviderIssue::recommendation)
                .toList();
    }
}
