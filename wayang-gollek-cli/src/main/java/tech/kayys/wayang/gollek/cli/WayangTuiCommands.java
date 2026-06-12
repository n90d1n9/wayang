package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

/**
 * CLI command module for opening the local Tamboui workbench app.
 *
 * <p>The command supplies the shared {@link WayangCliContext} client facade to
 * the TUI so terminal rendering stays behind the same SDK boundary as other wrappers.</p>
 */
final class WayangTuiCommands {

    private WayangTuiCommands() {
    }

    @Command(name = "tui", description = "Open the Tamboui-powered Wayang dashboard.")
    static final class TuiCommand implements Callable<Integer> {
        @ParentCommand
        WayangGollekCli parent;

        @Override
        public Integer call() {
            WayangCliContext context = parent.context();
            try {
                new WayangGollekTuiApp(context.client()).run();
                return 0;
            } catch (Exception e) {
                context.err().println("Unable to open Wayang TUI: " + e.getMessage());
                return 1;
            }
        }
    }
}
