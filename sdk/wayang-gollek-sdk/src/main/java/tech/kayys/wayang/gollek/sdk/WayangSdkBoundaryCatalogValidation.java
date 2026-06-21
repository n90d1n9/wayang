package tech.kayys.wayang.gollek.sdk;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Validates the SDK boundary catalog before wrappers or future package-split
 * tooling depend on its ownership metadata.
 */
public final class WayangSdkBoundaryCatalogValidation {

    private WayangSdkBoundaryCatalogValidation() {
    }

    public static WayangSdkBoundaryCatalogValidationReport validateDefault() {
        return validate(WayangSdkBoundaryCatalog.defaultBoundaries());
    }

    public static WayangSdkBoundaryCatalogValidationReport validate(List<WayangSdkBoundary> boundaries) {
        List<WayangSdkBoundary> model = boundaries == null ? List.of() : boundaries;
        List<WayangSdkBoundaryCatalogValidationIssue> issues = new ArrayList<>();
        List<WayangSdkBoundary> nonNullBoundaries = new ArrayList<>();
        Set<String> boundaryIds = new LinkedHashSet<>();
        Set<String> intendedPackages = new LinkedHashSet<>();
        Set<String> classPrefixes = new LinkedHashSet<>();
        Set<String> contractSchemas = new LinkedHashSet<>();
        Map<String, WayangSdkBoundary> boundaryById = new LinkedHashMap<>();
        Map<String, String> packageOwners = new LinkedHashMap<>();
        Map<String, String> classPrefixOwners = new LinkedHashMap<>();

        if (model.isEmpty()) {
            issues.add(issue(
                    "empty-boundary-catalog",
                    "At least one SDK boundary is required.",
                    "",
                    "boundaries",
                    ""));
        }

        for (int index = 0; index < model.size(); index++) {
            WayangSdkBoundary boundary = model.get(index);
            if (boundary == null) {
                issues.add(issue(
                        "null-boundary",
                        "SDK boundary catalog contains a null entry at index " + index + ".",
                        "",
                        "boundaries[" + index + "]",
                        ""));
                continue;
            }
            nonNullBoundaries.add(boundary);
            if (!boundaryIds.add(boundary.id())) {
                issues.add(issue(
                        "duplicate-boundary-id",
                        "Duplicate SDK boundary id '" + boundary.id() + "'.",
                        boundary.id(),
                        "id",
                        boundary.id()));
            }
            boundaryById.putIfAbsent(boundary.id(), boundary);
        }

        if (!boundaryIds.contains(WayangSdkBoundaryCatalog.DEFAULT_BOUNDARY_ID)) {
            issues.add(issue(
                    "missing-default-boundary",
                    "SDK boundary catalog must include the default boundary id '"
                            + WayangSdkBoundaryCatalog.DEFAULT_BOUNDARY_ID + "'.",
                    "",
                    "id",
                    WayangSdkBoundaryCatalog.DEFAULT_BOUNDARY_ID));
        }

        for (WayangSdkBoundary boundary : nonNullBoundaries) {
            validatePackage(boundary, intendedPackages, packageOwners, issues);
            validateClassPrefixes(boundary, classPrefixes, classPrefixOwners, issues);
            contractSchemas.addAll(boundary.contractSchemas());
            validateDependencies(boundary, boundaryIds, issues);
        }
        validateDependencyCycles(boundaryById, issues);

        return new WayangSdkBoundaryCatalogValidationReport(
                nonNullBoundaries.size(),
                List.copyOf(boundaryIds),
                List.copyOf(intendedPackages),
                List.copyOf(classPrefixes),
                List.copyOf(contractSchemas),
                issues);
    }

    private static void validatePackage(
            WayangSdkBoundary boundary,
            Set<String> intendedPackages,
            Map<String, String> packageOwners,
            List<WayangSdkBoundaryCatalogValidationIssue> issues) {
        intendedPackages.add(boundary.intendedPackage());
        String expectedPrefix = WayangSdkBoundaryCatalog.SDK_ROOT_PACKAGE + ".";
        if (!boundary.intendedPackage().startsWith(expectedPrefix)) {
            issues.add(issue(
                    "invalid-intended-package",
                    "Boundary '" + boundary.id() + "' must target a package under '" + expectedPrefix + "'.",
                    boundary.id(),
                    "intendedPackage",
                    boundary.intendedPackage()));
        }

        String existingOwner = packageOwners.putIfAbsent(boundary.intendedPackage(), boundary.id());
        if (existingOwner != null && !existingOwner.equals(boundary.id())) {
            issues.add(issue(
                    "duplicate-intended-package",
                    "Boundaries '" + existingOwner + "' and '" + boundary.id()
                            + "' share intended package '" + boundary.intendedPackage() + "'.",
                    boundary.id(),
                    "intendedPackage",
                    boundary.intendedPackage()));
        }
    }

    private static void validateClassPrefixes(
            WayangSdkBoundary boundary,
            Set<String> classPrefixes,
            Map<String, String> classPrefixOwners,
            List<WayangSdkBoundaryCatalogValidationIssue> issues) {
        if (boundary.classPrefixes().isEmpty()) {
            issues.add(issue(
                    "empty-class-prefixes",
                    "Boundary '" + boundary.id() + "' must own at least one class prefix.",
                    boundary.id(),
                    "classPrefixes",
                    ""));
            return;
        }

        for (String prefix : boundary.classPrefixes()) {
            classPrefixes.add(prefix);
            String existingOwner = classPrefixOwners.putIfAbsent(prefix, boundary.id());
            if (existingOwner != null && !existingOwner.equals(boundary.id())) {
                issues.add(issue(
                        "duplicate-class-prefix",
                        "Class prefix '" + prefix + "' is owned by both '" + existingOwner
                                + "' and '" + boundary.id() + "'.",
                        boundary.id(),
                        "classPrefixes",
                        prefix));
            }
        }
    }

    private static void validateDependencies(
            WayangSdkBoundary boundary,
            Set<String> boundaryIds,
            List<WayangSdkBoundaryCatalogValidationIssue> issues) {
        for (String dependency : boundary.dependsOn()) {
            if (boundary.id().equals(dependency)) {
                issues.add(issue(
                        "self-dependency",
                        "Boundary '" + boundary.id() + "' must not depend on itself.",
                        boundary.id(),
                        "dependsOn",
                        dependency));
            }
            if (!boundaryIds.contains(dependency)) {
                issues.add(issue(
                        "unknown-dependency",
                        "Boundary '" + boundary.id() + "' depends on unknown boundary '" + dependency + "'.",
                        boundary.id(),
                        "dependsOn",
                        dependency));
            }
        }
    }

    private static void validateDependencyCycles(
            Map<String, WayangSdkBoundary> boundaryById,
            List<WayangSdkBoundaryCatalogValidationIssue> issues) {
        Set<String> visiting = new LinkedHashSet<>();
        Set<String> visited = new LinkedHashSet<>();
        Set<String> reported = new LinkedHashSet<>();
        Deque<String> path = new ArrayDeque<>();
        for (String boundaryId : boundaryById.keySet()) {
            visit(boundaryId, boundaryById, visiting, visited, reported, path, issues);
        }
    }

    private static void visit(
            String boundaryId,
            Map<String, WayangSdkBoundary> boundaryById,
            Set<String> visiting,
            Set<String> visited,
            Set<String> reported,
            Deque<String> path,
            List<WayangSdkBoundaryCatalogValidationIssue> issues) {
        if (visiting.contains(boundaryId)) {
            List<String> cycle = cyclePath(boundaryId, path);
            String value = String.join(" -> ", cycle);
            if (reported.add(value)) {
                issues.add(issue(
                        "dependency-cycle",
                        "SDK boundary dependency cycle detected: " + value + ".",
                        boundaryId,
                        "dependsOn",
                        value));
            }
            return;
        }
        if (visited.contains(boundaryId)) {
            return;
        }

        WayangSdkBoundary boundary = boundaryById.get(boundaryId);
        if (boundary == null) {
            return;
        }
        visiting.add(boundaryId);
        path.addLast(boundaryId);
        for (String dependency : boundary.dependsOn()) {
            if (!boundaryId.equals(dependency) && boundaryById.containsKey(dependency)) {
                visit(dependency, boundaryById, visiting, visited, reported, path, issues);
            }
        }
        path.removeLast();
        visiting.remove(boundaryId);
        visited.add(boundaryId);
    }

    private static List<String> cyclePath(String boundaryId, Deque<String> path) {
        List<String> cycle = new ArrayList<>();
        boolean insideCycle = false;
        for (String pathBoundaryId : path) {
            if (pathBoundaryId.equals(boundaryId)) {
                insideCycle = true;
            }
            if (insideCycle) {
                cycle.add(pathBoundaryId);
            }
        }
        cycle.add(boundaryId);
        return cycle;
    }

    private static WayangSdkBoundaryCatalogValidationIssue issue(
            String kind,
            String message,
            String boundaryId,
            String field,
            String value) {
        return new WayangSdkBoundaryCatalogValidationIssue(kind, message, boundaryId, field, value);
    }
}
