package tech.kayys.wayang.workbench;

import java.util.List;
import java.util.Optional;

/**
 * Deprecated: Use {@link WorkbenchCatalog} instead.
 * This class is maintained for backward compatibility and delegates all methods to WorkbenchCatalog.
 */
@Deprecated(forRemoval = true)
public final class WayangWorkbenchCatalog {

    private WayangWorkbenchCatalog() {
    }

    // Delegate to WorkbenchCatalog

    public static List<WorkbenchCommand> sharedCommands() {
        return WorkbenchCatalog.sharedCommands();
    }

    public static List<WorkbenchCommand> localCommands() {
        return WorkbenchCatalog.localCommands();
    }

    public static List<WorkbenchCommand> remoteCommands() {
        return WorkbenchCatalog.remoteCommands();
    }

    public static List<WorkbenchCommand> sharedCommandsForSurface(String surfaceId) {
        return WorkbenchCatalog.sharedCommandsForSurface(surfaceId);
    }

    public static List<WorkbenchCommand> localCommandsForSurface(String surfaceId) {
        return WorkbenchCatalog.localCommandsForSurface(surfaceId);
    }

    public static List<WorkbenchCommand> remoteCommandsForSurface(String surfaceId) {
        return WorkbenchCatalog.remoteCommandsForSurface(surfaceId);
    }

    public static List<WorkbenchCommand> commandsForSurface(List<WorkbenchCommand> commands, String surfaceId) {
        return WorkbenchCatalog.commandsForSurface(commands, surfaceId);
    }

    public static List<WorkbenchCommand> commandsForCategory(List<WorkbenchCommand> commands, String category) {
        return WorkbenchCatalog.commandsForCategory(commands, category);
    }

    public static List<WorkbenchCommand> commandsForId(List<WorkbenchCommand> commands, String commandId) {
        return WorkbenchCatalog.commandsForId(commands, commandId);
    }

    public static Optional<WorkbenchCommand> findCommand(List<WorkbenchCommand> commands, String commandId) {
        return WorkbenchCatalog.findCommand(commands, commandId);
    }

    public static List<String> knownCommandIds(List<WorkbenchCommand> commands) {
        return WorkbenchCatalog.knownCommandIds(commands);
    }

    public static List<String> knownCommandCategories(List<WorkbenchCommand> commands) {
        return WorkbenchCatalog.knownCommandCategories(commands);
    }

    public static List<String> sharedCommandPalette() {
        return WorkbenchCatalog.sharedCommandPalette();
    }

    public static List<String> localCommandPalette() {
        return WorkbenchCatalog.localCommandPalette();
    }

    public static List<String> remoteCommandPalette() {
        return WorkbenchCatalog.remoteCommandPalette();
    }

    public static List<String> sharedCommandPaletteForSurface(String surfaceId) {
        return WorkbenchCatalog.sharedCommandPaletteForSurface(surfaceId);
    }

    public static List<String> localCommandPaletteForSurface(String surfaceId) {
        return WorkbenchCatalog.localCommandPaletteForSurface(surfaceId);
    }

    public static List<String> remoteCommandPaletteForSurface(String surfaceId) {
        return WorkbenchCatalog.remoteCommandPaletteForSurface(surfaceId);
    }
}
