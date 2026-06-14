package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import tech.kayys.wayang.gollek.sdk.Wayang;
import tech.kayys.wayang.gollek.sdk.WayangClient;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Root picocli application for the Wayang agentic platform command line.
 *
 * <p>The root command owns SDK configuration and client construction, while
 * subcommands consume the shared {@link WayangClient} through {@link WayangCliContext}.</p>
 */
@Command(
        name = "wayang",
        aliases = "wayang-gollek",
        description = "Wayang agentic platform CLI. Compatibility alias: wayang-gollek.",
        mixinStandardHelpOptions = true,
        subcommands = {
                WayangCodeCommand.class,
                WayangPlatformCommands.StatusCommand.class,
                WayangPlatformCommands.ProductsCommand.class,
                WayangPlatformCommands.SdkBoundariesCommand.class,
                WayangPlatformCommands.ReadinessProfilesCommand.class,
                WayangPlatformCommands.ProfilesCommand.class,
                WayangContextCommands.WorkspaceCommand.class,
                WayangContextCommands.HarnessCommand.class,
                WayangSpecCommands.SpecCommand.class,
                WayangRunCommands.RunCommand.class,
                WayangSkillCommands.SkillsCommand.class,
                WayangProviderCapabilityCommands.ProvidersCommand.class,
                WayangContractCommands.ContractsCommand.class,
                WayangStandardsCommands.StandardsCommand.class,
                WayangWorkbenchCommands.CommandsCommand.class,
                WayangWorkbenchCommands.WorkbenchCommand.class,
                WayangTuiCommands.TuiCommand.class
        })
public final class WayangGollekCli implements Runnable {

    private final WayangGollekSdk injectedSdk;
    private final InputStream in;
    private final PrintStream out;
    private final PrintStream err;
    private final WayangCliContext context;
    private WayangGollekSdk resolvedSdk;
    private WayangClient resolvedClient;

    @Mixin
    private WayangCliSdkOptions sdkOptions = new WayangCliSdkOptions();

    public WayangGollekCli() {
        this(null, System.in, System.out, System.err);
    }

    WayangGollekCli(WayangGollekSdk injectedSdk, PrintStream out, PrintStream err) {
        this(injectedSdk, System.in, out, err);
    }

    WayangGollekCli(WayangGollekSdk injectedSdk, InputStream in, PrintStream out, PrintStream err) {
        this.injectedSdk = injectedSdk;
        this.in = in == null ? InputStream.nullInputStream() : in;
        this.out = out;
        this.err = err;
        this.context = new WayangCliContext(
                this::client,
                this.in,
                this.out,
                this.err);
    }

    public static int execute(String... args) {
        return execute(null, System.out, System.err, args);
    }

    static int execute(WayangGollekSdk service, PrintStream out, PrintStream err, String... args) {
        return execute(service, System.in, out, err, args);
    }

    static int execute(WayangGollekSdk service, InputStream in, PrintStream out, PrintStream err, String... args) {
        CommandLine commandLine = new CommandLine(new WayangGollekCli(service, in, out, err));
        commandLine.setOut(new PrintWriter(out, true));
        commandLine.setErr(new PrintWriter(err, true));
        return commandLine.execute(args);
    }

    public static void main(String[] args) {
        System.exit(execute(args));
    }

    @Override
    public void run() {
        CommandLine.usage(this, out);
    }

    private WayangGollekSdk sdk() {
        if (injectedSdk != null) {
            return injectedSdk;
        }
        if (resolvedSdk == null) {
            resolvedSdk = Wayang.create(sdkOptions.toConfig());
        }
        return resolvedSdk;
    }

    private WayangClient client() {
        if (resolvedClient == null) {
            resolvedClient = Wayang.client(sdk());
        }
        return resolvedClient;
    }

    WayangCliContext context() {
        return context;
    }
}
