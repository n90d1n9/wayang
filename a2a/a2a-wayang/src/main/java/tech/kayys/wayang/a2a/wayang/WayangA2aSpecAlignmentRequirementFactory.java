package tech.kayys.wayang.a2a.wayang;

import java.util.Map;

final class WayangA2aSpecAlignmentRequirementFactory {

    private WayangA2aSpecAlignmentRequirementFactory() {
    }

    static WayangA2aSpecAlignmentRequirement compare(
            String id,
            String category,
            String title,
            Map<String, Object> expected,
            Map<String, Object> actual,
            String message) {
        return from(id, category, title, expected.equals(actual), expected, actual, message);
    }

    static WayangA2aSpecAlignmentRequirement from(
            String id,
            String category,
            String title,
            boolean aligned,
            Map<String, Object> expected,
            Map<String, Object> actual,
            String message) {
        if (aligned) {
            return WayangA2aSpecAlignmentRequirement.aligned(id, category, title, expected, actual);
        }
        return WayangA2aSpecAlignmentRequirement.gap(id, category, title, expected, actual, message);
    }
}
