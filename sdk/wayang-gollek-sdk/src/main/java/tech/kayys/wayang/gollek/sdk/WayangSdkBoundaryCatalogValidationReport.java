package tech.kayys.wayang.gollek.sdk;

import java.util.List;

/**
 * Immutable validation report for the SDK boundary catalog, intended for SDK,
 * API, CLI, and future package-split tooling.
 */
public record WayangSdkBoundaryCatalogValidationReport(
        int totalBoundaries,
        List<String> boundaryIds,
        List<String> intendedPackages,
        List<String> classPrefixes,
        List<String> contractSchemas,
        List<WayangSdkBoundaryCatalogValidationIssue> issues) {

    public WayangSdkBoundaryCatalogValidationReport {
        totalBoundaries = Math.max(0, totalBoundaries);
        boundaryIds = SdkLists.copy(boundaryIds);
        intendedPackages = SdkLists.copy(intendedPackages);
        classPrefixes = SdkLists.copy(classPrefixes);
        contractSchemas = SdkLists.copy(contractSchemas);
        issues = SdkLists.copy(issues);
    }

    public boolean valid() {
        return issues.isEmpty();
    }

    public int issueCount() {
        return issues.size();
    }

    public int intendedPackageCount() {
        return intendedPackages.size();
    }

    public int classPrefixCount() {
        return classPrefixes.size();
    }

    public int contractSchemaCount() {
        return contractSchemas.size();
    }
}
