package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangWorkbenchCatalog;
import tech.kayys.wayang.gollek.sdk.WorkbenchCommand;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class WayangCliCommandFamilies {
    private static final Set<String> RUN_SUBCOMMANDS =
            Set.of("status", "inspect", "events", "list", "stats", "store", "wait", "cancel", "forget");
    private static final Map<String, Set<String>> SUBCOMMANDS_BY_ROOT = Map.of(
            "profiles", Set.of("inspect"),
            "providers", Set.of("list", "inspect", "search"),
            "readiness-profiles", Set.of("inspect", "policies", "sources"),
            "skills", Set.of("list", "inspect", "search"),
            "spec", Set.of("validate", "template"));

    private WayangCliCommandFamilies() {
    }

    static Set<String> localWorkbenchCommandFamilies() {
        return WayangWorkbenchCatalog.localCommands().stream()
                .map(WorkbenchCommand::command)
                .map(WayangCliCommandFamilies::commandFamily)
                .collect(Collectors.toCollection(HashSet::new));
    }

    static boolean sameCommandFamily(String catalogCommand, String fixtureCommand) {
        return commandFamily(catalogCommand).equals(commandFamily(fixtureCommand));
    }

    static boolean sameCommandFamily(String catalogCommand, List<String> fixtureArgs) {
        return commandFamily(catalogCommand).equals(commandFamily(fixtureArgs));
    }

    static String commandFamily(String commandLine) {
        List<String> tokens = commandLine == null
                ? List.of()
                : List.of(commandLine.trim().split("\\s+"));
        return commandFamilyFromTokens(tokens);
    }

    static String commandFamily(List<String> args) {
        List<String> tokens = args == null
                ? List.of()
                : args.stream()
                        .map(value -> value == null ? "" : value.trim())
                        .filter(value -> !value.isBlank())
                        .toList();
        return commandFamilyFromTokens(tokens);
    }

    private static String commandFamilyFromTokens(List<String> tokens) {
        if (tokens.isEmpty()) {
            return "";
        }
        String head = tokens.get(0);
        if (head.equals("run")) {
            return tokens.size() > 1 && RUN_SUBCOMMANDS.contains(tokens.get(1))
                    ? head + " " + tokens.get(1)
                    : "run <task>";
        }
        if (tokens.size() > 1 && SUBCOMMANDS_BY_ROOT.getOrDefault(head, Set.of()).contains(tokens.get(1))) {
            return head + " " + tokens.get(1);
        }
        return head;
    }
}
