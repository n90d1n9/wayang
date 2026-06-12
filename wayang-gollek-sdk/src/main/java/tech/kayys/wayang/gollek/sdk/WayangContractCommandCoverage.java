package tech.kayys.wayang.gollek.sdk;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class WayangContractCommandCoverage {

    private WayangContractCommandCoverage() {
    }

    public static WayangContractCommandCoverageReport defaultCoverage() {
        return of(WayangContractCatalog.defaultContracts(), WayangWorkbenchCatalog.localCommands());
    }

    public static WayangContractCommandCoverageReport of(
            List<WayangContractDescriptor> contracts,
            List<WorkbenchCommand> commands) {
        List<WayangContractDescriptor> contractSource = SdkLists.copy(contracts);
        List<WorkbenchCommand> commandSource = SdkLists.copy(commands);
        Map<WayangContractKey, List<String>> linkedCommandIds = linkedCommandIdsByContract(commandSource);
        return new WayangContractCommandCoverageReport(
                contractSource.size(),
                commandSource.size(),
                contractSource.stream()
                        .map(contract -> entry(contract, linkedCommandIds.get(contract.key())))
                        .toList());
    }

    private static WayangContractCommandCoverageEntry entry(
            WayangContractDescriptor contract,
            List<String> linkedCommandIds) {
        return new WayangContractCommandCoverageEntry(
                contract,
                contract == null ? List.of() : contract.commandIds(),
                linkedCommandIds);
    }

    private static Map<WayangContractKey, List<String>> linkedCommandIdsByContract(List<WorkbenchCommand> commands) {
        Map<WayangContractKey, LinkedHashSet<String>> values = new LinkedHashMap<>();
        for (WorkbenchCommand command : commands) {
            for (WorkbenchCommandContract contract : command.contracts()) {
                values.computeIfAbsent(contract.key(), ignored -> new LinkedHashSet<>())
                        .add(command.id());
            }
        }
        if (values.isEmpty()) {
            return Map.of();
        }
        Map<WayangContractKey, List<String>> copy = new LinkedHashMap<>();
        values.forEach((key, commandIds) -> copy.put(key, List.copyOf(commandIds)));
        return Collections.unmodifiableMap(copy);
    }
}
