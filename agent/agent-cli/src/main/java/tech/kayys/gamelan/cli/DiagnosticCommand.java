package tech.kayys.gamelan.cli;

import picocli.CommandLine.Command;
import tech.kayys.gamelan.cli.diagnostic.ImportCheckCommand;

/**
 * Diagnostic commands for code analysis and validation.
 * 
 * <p>This command group provides tools for analyzing code for common issues
 * such as broken imports, unused code, and migration problems.
 * 
 * <pre>
 * Usage examples:
 *   gamelan diagnostic import-check /path/to/project
 *   gamelan diagnostic import-check --help
 * </pre>
 */
@Command(
    name = "diagnostic",
    description = "Code diagnostic and analysis tools",
    mixinStandardHelpOptions = true,
    subcommands = {
        ImportCheckCommand.class
    }
)
public class DiagnosticCommand implements Runnable {
    
    @Override
    public void run() {
        // When invoked without subcommand, show help
        System.out.println("Code diagnostic tools for the Wayang coding agent.");
        System.out.println();
        System.out.println("Available subcommands:");
        System.out.println("  import-check - Check Java import statements for validity");
        System.out.println();
        System.out.println("Use 'gamelan diagnostic <command> --help' for more information.");
    }
}
