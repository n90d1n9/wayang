package tech.kayys.wayang.client;

import java.util.List;

import tech.kayys.wayang.workbench.WorkbenchCommand;

/**
 * SDK-owned workbench projection for command palettes and agent product shells.
 *
 * <p>The model is intentionally UI-neutral: CLI, TUI, HTTP, and future desktop
 * surfaces can render the same status, surface catalog, command set, and next
 * actions without coupling to one wrapper implementation.</p>
 */
public record WayangWorkbenchModel(
        WayangPlatformStatus status,
        List<ProductSurface> productSurfaces,
        List<String> commandPalette,
        List<WorkbenchCommand> commands,
        List<String> nextActions) {

    public WayangWorkbenchModel(
            WayangPlatformStatus status,
            List<ProductSurface> productSurfaces,
            List<String> commandPalette,
            List<String> nextActions) {
        this(status, productSurfaces, commandPalette, commandsFromPalette(commandPalette), nextActions);
    }

    public WayangWorkbenchModel {
        status = status == null ? unknownStatus() : status;
        productSurfaces = SdkLists.copy(productSurfaces);
        commands = SdkLists.copy(commands);
        commandPalette = SdkLists.copy(commandPalette);
        if (commandPalette.isEmpty() && !commands.isEmpty()) {
            commandPalette = commandPalette(commands);
        }
        if (commands.isEmpty() && !commandPalette.isEmpty()) {
            commands = commandsFromPalette(commandPalette);
        }
        nextActions = SdkLists.copy(nextActions);
    }

    private static List<String> commandPalette(List<WorkbenchCommand> commands) {
        return commands.stream()
                .map(WorkbenchCommand::command)
                .toList();
    }

    private static List<WorkbenchCommand> commandsFromPalette(List<String> commandPalette) {
        List<String> commands = SdkLists.copy(commandPalette);
        if (commands.isEmpty()) {
            return List.of();
        }
        java.util.ArrayList<WorkbenchCommand> result = new java.util.ArrayList<>(commands.size());
        for (int i = 0; i < commands.size(); i++) {
            String command = commands.get(i);
            result.add(WorkbenchCommand.shared(
                    "command-" + (i + 1),
                    command,
                    command,
                    "General",
                    "",
                    List.of()));
        }
        return List.copyOf(result);
    }

    private static WayangPlatformStatus unknownStatus() {
        return new WayangPlatformStatus(
                "Wayang",
                "unknown",
                null,
                null,
                null,
                null,
                null,
                0,
                List.of("No Wayang workbench status provider is configured."));
    }
}
