package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Objects;

record WayangA2aSpecAlignmentRequirementSummary(
        List<WayangA2aSpecAlignmentRequirement> requirements) {

    WayangA2aSpecAlignmentRequirementSummary {
        requirements = requirements == null
                ? List.of()
                : requirements.stream()
                        .filter(Objects::nonNull)
                        .toList();
    }

    boolean aligned() {
        return gapCount() == 0;
    }

    int requirementCount() {
        return requirements.size();
    }

    int alignedCount() {
        return (int) requirements.stream()
                .filter(WayangA2aSpecAlignmentRequirement::aligned)
                .count();
    }

    int gapCount() {
        return requirementCount() - alignedCount();
    }

    List<WayangA2aSpecAlignmentRequirement> gaps() {
        return requirements.stream()
                .filter(requirement -> !requirement.aligned())
                .toList();
    }

    List<String> requirementIds() {
        return requirements.stream()
                .map(WayangA2aSpecAlignmentRequirement::id)
                .toList();
    }

    List<String> gapIds() {
        return gaps().stream()
                .map(WayangA2aSpecAlignmentRequirement::id)
                .toList();
    }
}
