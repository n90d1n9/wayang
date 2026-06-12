package tech.kayys.gamelan.cli;

import jakarta.inject.Inject;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import tech.kayys.gamelan.config.ApprovalMode;
import tech.kayys.gamelan.config.GamelanConfigStore;
import tech.kayys.gamelan.util.AnsiPrinter;

import java.util.Set;

/**
 * Approval mode and permission management commands.
 *
 * <pre>
 * Usage:
 *   gamelan approve mode auto              # Set approval mode
 *   gamelan approve trust read_file        # Trust a specific tool
 *   gamelan approve untrust shell          # Untrust a specific tool
 *   gamelan approve list                   # List trusted tools
 *   gamelan approve permissions            # Show current permissions
 * </pre>
 */
@Command(
    name = "approve",
    description = "Manage approval modes and tool permissions",
    mixinStandardHelpOptions = true,
    subcommands = {
        ApproveModeCommand.class,
        TrustToolCommand.class,
        UntrustToolCommand.class,
        ListTrustedCommand.class,
        ShowPermissionsCommand.class
    }
)
public class ApprovalCommand implements Runnable {
    @Override
    public void run() { new ShowPermissionsCommand().run(); }
}

@Command(name = "mode", description = "Set approval mode")
class ApproveModeCommand implements Runnable {

    @Inject GamelanConfigStore configStore;

    @Parameters(index = "0", description = "Approval mode: auto, trusted-tools, always", paramLabel = "MODE")
    String mode;

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(true);
        try {
            ApprovalMode approvalMode = ApprovalMode.valueOf(mode.toUpperCase().replace("-", "_"));
            configStore.setApprovalMode(approvalMode);
            printer.success("Approval mode set to: " + approvalMode.toDisplayString());
            printer.info(approvalMode.getDescription());
        } catch (IllegalArgumentException e) {
            printer.error("Invalid mode: " + mode);
            printer.info("Valid modes: auto, trusted-tools, always");
            System.exit(1);
        }
    }
}

@Command(name = "trust", description = "Trust a specific tool")
class TrustToolCommand implements Runnable {

    @Inject GamelanConfigStore configStore;

    @Parameters(index = "0", description = "Tool name to trust", paramLabel = "TOOL")
    String toolName;

    @Option(names = {"--all"}, description = "Trust all instances of this tool pattern")
    boolean all;

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(true);
        configStore.addTrustedTool(toolName, all);
        printer.success("Now trusting: " + toolName + (all ? " (all instances)" : ""));
    }
}

@Command(name = "untrust", description = "Remove trust from a tool")
class UntrustToolCommand implements Runnable {

    @Inject GamelanConfigStore configStore;

    @Parameters(index = "0", description = "Tool name to untrust", paramLabel = "TOOL")
    String toolName;

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(true);
        boolean removed = configStore.removeTrustedTool(toolName);
        if (removed) printer.success("No longer trusting: " + toolName);
        else printer.warn("Tool was not trusted: " + toolName);
    }
}

@Command(name = "list", aliases = {"ls"}, description = "List trusted tools")
class ListTrustedCommand implements Runnable {

    @Inject GamelanConfigStore configStore;

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(true);
        Set<String> trusted = configStore.getTrustedTools();
        printer.sectionHeader("Trusted Tools (" + trusted.size() + ")");
        if (trusted.isEmpty()) {
            printer.info("No tools are currently trusted.");
        } else {
            trusted.forEach(tool -> printer.info("  ✓ " + tool));
        }
    }
}

@Command(name = "permissions", aliases = {"perms"}, description = "Show current permission configuration")
class ShowPermissionsCommand implements Runnable {

    @Inject GamelanConfigStore configStore;

    @Override
    public void run() {
        AnsiPrinter printer = new AnsiPrinter(true);
        printer.sectionHeader("Permission Configuration");
        printer.println();

        ApprovalMode mode = configStore.getApprovalMode();
        printer.info("Approval Mode: " + mode.toDisplayString());
        printer.info("  " + mode.getDescription());
        printer.println();

        Set<String> trusted = configStore.getTrustedTools();
        printer.info("Trusted Tools: " + trusted.size());
        trusted.forEach(tool -> printer.info("  ✓ " + tool));
        printer.println();

        boolean sandbox = configStore.isSandboxEnabled();
        printer.info("Sandbox Mode: " + (sandbox ? "Enabled" : "Disabled"));
    }
}
