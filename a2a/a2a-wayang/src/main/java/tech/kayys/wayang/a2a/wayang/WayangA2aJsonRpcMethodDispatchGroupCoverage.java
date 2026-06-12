package tech.kayys.wayang.a2a.wayang;

import java.util.List;
import java.util.Map;

record WayangA2aJsonRpcMethodDispatchGroupCoverage(
        String group,
        List<String> registeredMethods,
        List<String> dispatchMethods,
        List<String> missingDispatchMethods,
        List<String> orphanDispatchMethods) {

    WayangA2aJsonRpcMethodDispatchGroupCoverage {
        group = groupName(group);
        registeredMethods = normalize(registeredMethods);
        dispatchMethods = normalize(dispatchMethods);
        missingDispatchMethods = normalize(missingDispatchMethods);
        orphanDispatchMethods = normalize(orphanDispatchMethods);
    }

    static WayangA2aJsonRpcMethodDispatchGroupCoverage from(
            String group,
            List<String> groupMethods,
            List<String> registeredMethods,
            List<String> dispatchMethods) {
        List<String> normalizedGroupMethods = normalize(groupMethods);
        List<String> registered = normalize(registeredMethods);
        List<String> dispatched = normalize(dispatchMethods);
        List<String> registeredInGroup = normalizedGroupMethods.stream()
                .filter(registered::contains)
                .toList();
        List<String> dispatchedInGroup = normalizedGroupMethods.stream()
                .filter(dispatched::contains)
                .toList();
        return new WayangA2aJsonRpcMethodDispatchGroupCoverage(
                group,
                registeredInGroup,
                dispatchedInGroup,
                registeredInGroup.stream()
                        .filter(method -> !dispatchedInGroup.contains(method))
                        .toList(),
                dispatchedInGroup.stream()
                        .filter(method -> !registeredInGroup.contains(method))
                        .toList());
    }

    static WayangA2aJsonRpcMethodDispatchGroupCoverage fromGroupMethods(
            String group,
            List<String> registeredMethods,
            List<String> dispatchMethods) {
        List<String> registered = normalize(registeredMethods);
        List<String> dispatched = normalize(dispatchMethods);
        return new WayangA2aJsonRpcMethodDispatchGroupCoverage(
                group,
                registered,
                dispatched,
                registered.stream()
                        .filter(method -> !dispatched.contains(method))
                        .toList(),
                dispatched.stream()
                        .filter(method -> !registered.contains(method))
                        .toList());
    }

    static WayangA2aJsonRpcMethodDispatchGroupCoverage fromMap(Map<?, ?> values) {
        return WayangA2aJsonRpcMethodDispatchGroupCoverageProjection.fromMap(values);
    }

    boolean complete() {
        return missingDispatchMethods.isEmpty() && orphanDispatchMethods.isEmpty();
    }

    int registeredMethodCount() {
        return registeredMethods.size();
    }

    int dispatchMethodCount() {
        return dispatchMethods.size();
    }

    Map<String, Object> toMap() {
        return WayangA2aJsonRpcMethodDispatchGroupCoverageProjection.group(this);
    }

    private static List<String> normalize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private static String groupName(String value) {
        String normalized = WayangA2aMaps.optional(value);
        return normalized == null ? "unknown" : normalized;
    }
}
