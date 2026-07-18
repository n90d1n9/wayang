package tech.kayys.wayang.contract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WayangContractIntegrity {

    private WayangContractIntegrity() {
    }

    public static WayangContractIntegrityReport validateDefault() {
        return validate(WayangContractCatalog.defaultContracts(), WayangWorkbenchCatalog.localCommands());
    }

    public static WayangContractIntegrityReport validate(
            List<WayangContractDescriptor> contracts,
            List<WorkbenchCommand> commands) {
        List<WayangContractDescriptor> contractSource = SdkLists.copy(contracts);
        List<WorkbenchCommand> commandSource = SdkLists.copy(commands);
        List<WayangContractIntegrityIssue> issues = new ArrayList<>();

        WayangContractIndex contractIndex = WayangContractIndex.of(contractSource);
        contractIndex.duplicateKeys().forEach(key -> issues.add(issue(
                "duplicate-contract",
                "Duplicate contract descriptor " + label(key) + ".",
                key,
                "")));
        Map<WayangContractKey, WayangContractDescriptor> contractsByKey = contractIndex.contractsByKey();
        Map<String, WorkbenchCommand> commandsById = commandsById(commandSource, issues);

        int contractCommandLinks = 0;
        for (WayangContractDescriptor contract : contractSource) {
            WayangContractKey key = contract.key();
            for (String commandId : contract.commandIds()) {
                contractCommandLinks++;
                WorkbenchCommand command = commandsById.get(commandId);
                if (command == null) {
                    issues.add(issue(
                            "missing-command",
                            "Contract " + label(key) + " references unknown command id '" + commandId + "'.",
                            key,
                            commandId));
                } else if (!commandLinksToContract(command, key)) {
                    issues.add(issue(
                            "missing-command-contract",
                            "Command '" + commandId + "' does not link back to contract " + label(key) + ".",
                            key,
                            commandId));
                }
            }
        }

        int commandContractLinks = 0;
        for (WorkbenchCommand command : commandSource) {
            for (WorkbenchCommandContract contract : command.contracts()) {
                commandContractLinks++;
                WayangContractKey key = contract.key();
                WayangContractDescriptor descriptor = contractsByKey.get(key);
                if (descriptor == null) {
                    issues.add(issue(
                            "missing-contract",
                            "Command '" + command.id() + "' references unknown contract " + label(key) + ".",
                            key,
                            command.id()));
                } else if (!descriptor.commandIds().contains(command.id())) {
                    issues.add(issue(
                            "missing-contract-command-id",
                            "Contract " + label(key) + " does not link back to command id '" + command.id() + "'.",
                            key,
                            command.id()));
                }
            }
        }

        return new WayangContractIntegrityReport(
                contractSource.size(),
                commandSource.size(),
                contractCommandLinks,
                commandContractLinks,
                issues);
    }

    private static Map<String, WorkbenchCommand> commandsById(
            List<WorkbenchCommand> commands,
            List<WayangContractIntegrityIssue> issues) {
        Map<String, WorkbenchCommand> result = new LinkedHashMap<>();
        for (WorkbenchCommand command : commands) {
            if (result.putIfAbsent(command.id(), command) != null) {
                issues.add(new WayangContractIntegrityIssue(
                        "duplicate-command",
                        "Duplicate command id '" + command.id() + "'.",
                        "",
                        0,
                        "",
                        command.id()));
            }
        }
        return result;
    }

    private static boolean commandLinksToContract(WorkbenchCommand command, WayangContractKey key) {
        return command.contracts().stream()
                .anyMatch(key::matches);
    }

    private static WayangContractIntegrityIssue issue(
            String kind,
            String message,
            WayangContractKey key,
            String commandId) {
        return new WayangContractIntegrityIssue(
                kind,
                message,
                key.schema(),
                key.version(),
                key.envelope(),
                commandId);
    }

    private static String label(WayangContractKey key) {
        return key.label();
    }
}
