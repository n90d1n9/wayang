package tech.kayys.wayang.a2a.wayang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

record WayangA2aJsonRpcMethodDispatchCoverage(
        boolean reported,
        List<String> registeredMethods,
        List<String> dispatchMethods,
        List<String> missingDispatchMethods,
        List<String> orphanDispatchMethods,
        List<WayangA2aJsonRpcMethodDispatchGroupCoverage> methodGroups) {

    private static final String METHOD_GROUP_UNASSIGNED = "unassigned";

    WayangA2aJsonRpcMethodDispatchCoverage {
        registeredMethods = normalize(registeredMethods);
        dispatchMethods = normalize(dispatchMethods);
        missingDispatchMethods = normalize(missingDispatchMethods);
        orphanDispatchMethods = normalize(orphanDispatchMethods);
        methodGroups = normalizeMethodGroups(methodGroups);
    }

    static WayangA2aJsonRpcMethodDispatchCoverage from(WayangA2aJsonRpcMethodDispatchTable table) {
        if (table == null) {
            return new WayangA2aJsonRpcMethodDispatchCoverage(
                    false,
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of());
        }
        return from(
                WayangA2aJsonRpcMethods.methods(),
                table.methods(),
                WayangA2aJsonRpcMethods.methodGroups(),
                table.methodGroups());
    }

    static WayangA2aJsonRpcMethodDispatchCoverage from(
            List<String> registeredMethods,
            List<String> dispatchMethods) {
        return from(registeredMethods, dispatchMethods, methodGroupCoverage(registeredMethods, dispatchMethods));
    }

    private static WayangA2aJsonRpcMethodDispatchCoverage from(
            List<String> registeredMethods,
            List<String> dispatchMethods,
            Map<String, List<String>> registeredMethodGroups,
            Map<String, List<String>> dispatchMethodGroups) {
        return from(
                registeredMethods,
                dispatchMethods,
                methodGroupCoverage(
                        registeredMethods,
                        dispatchMethods,
                        registeredMethodGroups,
                        dispatchMethodGroups));
    }

    private static WayangA2aJsonRpcMethodDispatchCoverage from(
            List<String> registeredMethods,
            List<String> dispatchMethods,
            List<WayangA2aJsonRpcMethodDispatchGroupCoverage> methodGroups) {
        List<String> registered = normalize(registeredMethods);
        List<String> dispatched = normalize(dispatchMethods);
        return new WayangA2aJsonRpcMethodDispatchCoverage(
                true,
                registered,
                dispatched,
                registered.stream()
                        .filter(method -> !dispatched.contains(method))
                        .toList(),
                dispatched.stream()
                        .filter(method -> !registered.contains(method))
                        .toList(),
                methodGroups);
    }

    static WayangA2aJsonRpcMethodDispatchCoverage fromMap(Map<?, ?> values) {
        return WayangA2aJsonRpcMethodDispatchCoverageProjection.fromMap(values);
    }

    int registeredMethodCount() {
        return registeredMethods.size();
    }

    int dispatchMethodCount() {
        return dispatchMethods.size();
    }

    boolean complete() {
        return reported && missingDispatchMethods.isEmpty() && orphanDispatchMethods.isEmpty();
    }

    List<Map<String, Object>> methodGroupMaps() {
        return WayangA2aJsonRpcMethodDispatchCoverageProjection.methodGroups(this);
    }

    Map<String, Object> toMap() {
        return WayangA2aJsonRpcMethodDispatchCoverageProjection.coverage(this);
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

    private static List<WayangA2aJsonRpcMethodDispatchGroupCoverage> normalizeMethodGroups(
            List<WayangA2aJsonRpcMethodDispatchGroupCoverage> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null)
                .toList();
    }

    private static List<WayangA2aJsonRpcMethodDispatchGroupCoverage> methodGroupCoverage(
            List<String> registeredMethods,
            List<String> dispatchMethods) {
        List<String> registered = normalize(registeredMethods);
        List<String> dispatched = normalize(dispatchMethods);
        List<WayangA2aJsonRpcMethodDispatchGroupCoverage> values = new ArrayList<>();
        WayangA2aJsonRpcMethods.methodGroups()
                .forEach((group, methods) -> values.add(WayangA2aJsonRpcMethodDispatchGroupCoverage.from(
                        group,
                        methods,
                        registered,
                        dispatched)));
        List<String> assignedMethods = WayangA2aJsonRpcMethods.methodGroups().values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();
        List<String> unassignedMethods = unassignedMethods(registered, dispatched, assignedMethods);
        if (!unassignedMethods.isEmpty()) {
            values.add(WayangA2aJsonRpcMethodDispatchGroupCoverage.from(
                    METHOD_GROUP_UNASSIGNED,
                    unassignedMethods,
                    registered,
                    dispatched));
        }
        return List.copyOf(values);
    }

    private static List<WayangA2aJsonRpcMethodDispatchGroupCoverage> methodGroupCoverage(
            List<String> registeredMethods,
            List<String> dispatchMethods,
            Map<String, List<String>> registeredMethodGroups,
            Map<String, List<String>> dispatchMethodGroups) {
        List<String> registered = normalize(registeredMethods);
        List<String> dispatched = normalize(dispatchMethods);
        Map<String, List<String>> registeredGroups = normalizeMethodGroupMap(registeredMethodGroups);
        Map<String, List<String>> dispatchGroups = normalizeMethodGroupMap(dispatchMethodGroups);
        Map<String, List<String>> groups = new LinkedHashMap<>();
        registeredGroups.forEach(groups::put);
        dispatchGroups.forEach((group, methods) -> groups.putIfAbsent(group, methods));

        List<WayangA2aJsonRpcMethodDispatchGroupCoverage> values = new ArrayList<>();
        groups.forEach((group, methods) -> values.add(WayangA2aJsonRpcMethodDispatchGroupCoverage.fromGroupMethods(
                group,
                registeredGroups.getOrDefault(group, List.of()).stream()
                        .filter(registered::contains)
                        .toList(),
                dispatchGroups.getOrDefault(group, List.of()).stream()
                        .filter(dispatched::contains)
                        .toList())));
        List<String> assignedMethods = assignedMethods(registeredGroups, dispatchGroups);
        List<String> unassignedMethods = unassignedMethods(registered, dispatched, assignedMethods);
        if (!unassignedMethods.isEmpty()) {
            values.add(WayangA2aJsonRpcMethodDispatchGroupCoverage.from(
                    METHOD_GROUP_UNASSIGNED,
                    unassignedMethods,
                    registered,
                    dispatched));
        }
        return List.copyOf(values);
    }

    private static Map<String, List<String>> normalizeMethodGroupMap(Map<String, List<String>> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }
        Map<String, List<String>> normalized = new LinkedHashMap<>();
        values.forEach((group, methods) -> {
            String normalizedGroup = WayangA2aMaps.optional(group);
            List<String> normalizedMethods = normalize(methods);
            if (normalizedGroup != null && !normalizedMethods.isEmpty()) {
                normalized.put(normalizedGroup, normalizedMethods);
            }
        });
        return normalized;
    }

    private static List<String> assignedMethods(
            Map<String, List<String>> registeredGroups,
            Map<String, List<String>> dispatchGroups) {
        List<String> values = new ArrayList<>();
        registeredGroups.values().stream()
                .flatMap(List::stream)
                .forEach(method -> addDistinct(values, method));
        dispatchGroups.values().stream()
                .flatMap(List::stream)
                .forEach(method -> addDistinct(values, method));
        return List.copyOf(values);
    }

    private static List<String> unassignedMethods(
            List<String> registeredMethods,
            List<String> dispatchMethods,
            List<String> assignedMethods) {
        List<String> values = new ArrayList<>();
        registeredMethods.stream()
                .filter(method -> !assignedMethods.contains(method))
                .forEach(method -> addDistinct(values, method));
        dispatchMethods.stream()
                .filter(method -> !assignedMethods.contains(method))
                .forEach(method -> addDistinct(values, method));
        return List.copyOf(values);
    }

    private static void addDistinct(List<String> values, String method) {
        if (!values.contains(method)) {
            values.add(method);
        }
    }
}
