package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangContractCommandCoverage;
import tech.kayys.wayang.gollek.sdk.WayangContractCommandCoverageEntry;
import tech.kayys.wayang.gollek.sdk.WayangContractCommandCoverageReport;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class WayangCliGoldenFixtureCommandCoverage {
    private WayangCliGoldenFixtureCommandCoverage() {
    }

    static Report defaultReport() {
        return report(
                WayangCliGoldenFixtureManifest.entries(),
                WayangContractCommandCoverage.defaultCoverage());
    }

    static Report report(
            List<WayangCliGoldenFixtureManifest.Entry> entries,
            WayangContractCommandCoverageReport coverage) {
        LinkedHashSet<String> declaredCommandIds = declaredCommandIds(coverage);
        LinkedHashSet<String> coveredCommandIds = coveredCommandIds(entries);
        return new Report(
                List.copyOf(declaredCommandIds),
                List.copyOf(coveredCommandIds),
                missingCommandIds(declaredCommandIds, coveredCommandIds),
                undeclaredCoveredCommandIds(declaredCommandIds, coveredCommandIds));
    }

    static List<String> coveredCommandIds(WayangCliGoldenFixtureManifest.Entry entry) {
        if (!entry.schemaValidated()) {
            return List.of();
        }
        return entry.commandIds();
    }

    private static LinkedHashSet<String> declaredCommandIds(WayangContractCommandCoverageReport coverage) {
        LinkedHashSet<String> commandIds = new LinkedHashSet<>();
        for (WayangContractCommandCoverageEntry entry : coverage.entries()) {
            commandIds.addAll(entry.declaredCommandIds());
        }
        return commandIds;
    }

    private static LinkedHashSet<String> coveredCommandIds(List<WayangCliGoldenFixtureManifest.Entry> entries) {
        LinkedHashSet<String> commandIds = new LinkedHashSet<>();
        for (WayangCliGoldenFixtureManifest.Entry entry : entries) {
            commandIds.addAll(coveredCommandIds(entry));
        }
        return commandIds;
    }

    private static List<String> missingCommandIds(Set<String> declaredCommandIds, Set<String> coveredCommandIds) {
        return declaredCommandIds.stream()
                .filter(commandId -> !coveredCommandIds.contains(commandId))
                .toList();
    }

    private static List<String> undeclaredCoveredCommandIds(Set<String> declaredCommandIds, Set<String> coveredCommandIds) {
        return coveredCommandIds.stream()
                .filter(commandId -> !declaredCommandIds.contains(commandId))
                .toList();
    }

    record Report(
            List<String> declaredCommandIds,
            List<String> coveredCommandIds,
            List<String> missingCommandIds,
            List<String> undeclaredCoveredCommandIds) {
        Report {
            declaredCommandIds = List.copyOf(declaredCommandIds);
            coveredCommandIds = List.copyOf(coveredCommandIds);
            missingCommandIds = List.copyOf(missingCommandIds);
            undeclaredCoveredCommandIds = List.copyOf(undeclaredCoveredCommandIds);
        }
    }
}
