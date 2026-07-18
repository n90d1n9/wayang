package tech.kayys.wayang.workbench;

import java.util.List;
import java.util.Optional;

/**
 * Public facade for accessing workbench command catalog.
 * Provides static methods for querying and filtering commands.
 */
public final class WorkbenchCatalog {

    private WorkbenchCatalog() {
    }

    // Accessor methods for command lists

    public static List<WorkbenchCommand> sharedCommands() {
        return WorkbenchEnvelopes.SHARED_COMMANDS;
    }

    public static List<WorkbenchCommand> localCommands() {
        return WorkbenchEnvelopes.LOCAL_COMMANDS;
    }

    public static List<WorkbenchCommand> remoteCommands() {
        return WorkbenchEnvelopes.SHARED_COMMANDS;
    }

    // Surface-filtered accessors

    public static List<WorkbenchCommand> sharedCommandsForSurface(String surfaceId) {
        return commandsForSurface(WorkbenchEnvelopes.SHARED_COMMANDS, surfaceId);
    }

    public static List<WorkbenchCommand> localCommandsForSurface(String surfaceId) {
        return commandsForSurface(WorkbenchEnvelopes.LOCAL_COMMANDS, surfaceId);
    }

    public static List<WorkbenchCommand> remoteCommandsForSurface(String surfaceId) {
        return commandsForSurface(WorkbenchEnvelopes.SHARED_COMMANDS, surfaceId);
    }

    // Query methods delegating to WorkbenchCommandIndex

    public static List<WorkbenchCommand> commandsForSurface(List<WorkbenchCommand> commands, String surfaceId) {
        return WorkbenchCommandIndex.of(commands).commandsForSurface(surfaceId);
    }

    public static List<WorkbenchCommand> commandsForCategory(List<WorkbenchCommand> commands, String category) {
        return WorkbenchCommandIndex.of(commands).commandsForCategory(category);
    }

    public static List<WorkbenchCommand> commandsForId(List<WorkbenchCommand> commands, String commandId) {
        return WorkbenchCommandIndex.of(commands).commandsForId(commandId);
    }

    public static Optional<WorkbenchCommand> findCommand(List<WorkbenchCommand> commands, String commandId) {
        return WorkbenchCommandIndex.of(commands).findCommand(commandId);
    }

    public static List<String> knownCommandIds(List<WorkbenchCommand> commands) {
        return WorkbenchCommandIndex.of(commands).commandIds();
    }

    public static List<String> knownCommandCategories(List<WorkbenchCommand> commands) {
        return WorkbenchCommandIndex.of(commands).categories();
    }

    // Palette methods

    public static List<String> sharedCommandPalette() {
        return WorkbenchEnvelopes.palette(WorkbenchEnvelopes.SHARED_COMMANDS);
    }

    public static List<String> localCommandPalette() {
        return WorkbenchEnvelopes.palette(WorkbenchEnvelopes.LOCAL_COMMANDS);
    }

    public static List<String> remoteCommandPalette() {
        return WorkbenchEnvelopes.palette(WorkbenchEnvelopes.SHARED_COMMANDS);
    }

    public static List<String> sharedCommandPaletteForSurface(String surfaceId) {
        return WorkbenchEnvelopes.palette(sharedCommandsForSurface(surfaceId));
    }

    public static List<String> localCommandPaletteForSurface(String surfaceId) {
        return WorkbenchEnvelopes.palette(localCommandsForSurface(surfaceId));
    }

    public static List<String> remoteCommandPaletteForSurface(String surfaceId) {
        return WorkbenchEnvelopes.palette(remoteCommandsForSurface(surfaceId));
    }
}
