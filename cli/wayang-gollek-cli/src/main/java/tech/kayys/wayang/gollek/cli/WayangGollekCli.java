package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import tech.kayys.wayang.gollek.sdk.Wayang;
import tech.kayys.wayang.gollek.sdk.WayangClient;
import tech.kayys.wayang.gollek.sdk.WayangGollekSdk;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;

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
                WayangGollekProxyCommand.class,
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

    public boolean isIgnoreConfig() { return sdkOptions != null && Boolean.TRUE.equals(sdkOptions.ignoreConfig); }

    public WayangGollekSdk sdk() {
        if (injectedSdk != null) {
            return injectedSdk;
        }
        if (resolvedSdk == null) {
            resolvedSdk = Wayang.create(sdkOptions.toConfig());

            // Apply preferred provider from ~/.wayang/config.json if present
            try {
                Path cfg = Paths.get(System.getProperty("user.home"), ".wayang", "config.json");
                if (Files.exists(cfg) && !Boolean.TRUE.equals(sdkOptions.ignoreConfig)) {
                        boolean debug = Boolean.getBoolean("wayang.cli.debug");
                    String content = Files.readString(cfg);
                    Pattern p = Pattern.compile("\"provider\"\s*:\s*\"([^\"]+)\"");
                    Matcher m = p.matcher(content);
                    if (m.find()) {
                        String provider = m.group(1).trim();
                        try {
                            // Use reflection so this compiles against older SDK jars that may not
                            // have provider APIs yet.
                            Method listMethod = null;
                            Method setMethod = null;
                            try {
                                listMethod = resolvedSdk.getClass().getMethod("listAvailableProviders");
                            } catch (NoSuchMethodException nsme) {
                                // ignore
                            }
                            try {
                                setMethod = resolvedSdk.getClass().getMethod("setPreferredProvider", String.class);
                            } catch (NoSuchMethodException nsme) {
                                // ignore
                            }

                            List<?> available = List.of();
                            if (listMethod != null) {
                                Object availObj = listMethod.invoke(resolvedSdk);
                                if (availObj instanceof List<?> l) {
                                    available = l;
                                }
                            }

                            boolean found = false;
                            for (Object o : available) {
                                if (o == null) continue;
                                if (o instanceof String s) {
                                    if (s.equals(provider)) { found = true; break; }
                                } else {
                                    try {
                                        Method idMethod = o.getClass().getMethod("id");
                                        Object idv = idMethod.invoke(o);
                                        if (idv != null && provider.equals(String.valueOf(idv))) { found = true; break; }
                                    } catch (NoSuchMethodException ignored) {
                                    }
                                }
                            }

                            if (!found) {
                                String known = available.isEmpty() ? "<unknown>" : available.stream().map(o -> {
                                    if (o instanceof String s) return s;
                                    try { Method idM = o.getClass().getMethod("id"); Object v = idM.invoke(o); return v == null ? "<unknown>" : String.valueOf(v); } catch (Exception ex) { return "<unknown>"; }
                                }).toList().toString();
                                logToFile("Preferred provider '" + provider + "' from ~/.wayang/config.json is not available. Known providers: " + known);

                                // Attempt to auto-load provider JAR from local Maven repository if present
                                try {
                                    Method loadMethod = null; logToFile("DEBUG: probing sdk for dynamic loadProviderJar support...");
                                    try { loadMethod = resolvedSdk.getClass().getMethod("loadProviderJar", String.class); logToFile("DEBUG: sdk exposes loadProviderJar"); } catch (NoSuchMethodException nsme) { logToFile("DEBUG: sdk does NOT expose loadProviderJar"); }
                                    try {
                                        loadMethod = resolvedSdk.getClass().getMethod("loadProviderJar", String.class);
                                    } catch (NoSuchMethodException nsme) {
                                        // SDK may not support dynamic loading
                                    }
                                    Path candidateDir = Paths.get(System.getProperty("user.home"), ".m2", "repository", "tech", "kayys", "gollek", "gollek-plugin-" + provider);
                                    if (Files.exists(candidateDir)) {
                                        java.util.Optional<Path> jarOpt = Files.walk(candidateDir)
                                                .filter(x -> x.getFileName().toString().endsWith(".jar"))
                                                .findFirst();
                                        if (jarOpt.isPresent()) {
                                            Path jarPath = jarOpt.get();
                                            logToFile("DEBUG: Found provider JAR candidate: " + jarPath);
                                            // If SDK exposes loadProviderJar, use it; else try to register capability via provider registry reflectively
                                            if (loadMethod != null) {
                                                try {
                                                    loadMethod.invoke(resolvedSdk, jarPath.toString());
                                                    if (debug) logToFile("DEBUG: Loaded provider JAR via SDK: " + jarPath);
                                                } catch (Throwable t) {
                                                    logToFile("Warning: failed to invoke SDK.loadProviderJar: " + t.getMessage());
                                                }
                                            } else {                                                try {
                                                    // reflectively register a capability descriptor into providerCapabilityRegistry
                                                    Method regMethod = resolvedSdk.getClass().getMethod("providerCapabilityRegistry");
                                                    Object registry = regMethod.invoke(resolvedSdk);
                                                    if (registry != null) {
                                                        Class<?> descClass = Class.forName("tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityDescriptor");
                                                        Class<?> stateClass = Class.forName("tech.kayys.wayang.gollek.sdk.WayangProviderCapabilityState");
                                                        java.lang.reflect.Constructor<?> ctor = descClass.getConstructor(
                                                                String.class, String.class, String.class, String.class,
                                                                String.class, String.class, String.class, stateClass,
                                                                java.util.List.class, java.util.List.class, java.util.List.class, java.util.Map.class);
                                                        String moduleId = jarPath.getFileName().toString().replaceAll("\\\\.jar$", "");
                                                        String providerIdGuess = provider;
                                                        String capabilityId = providerIdGuess + ".inference";
                                                        Object stateVal = java.lang.Enum.valueOf((Class<Enum>) stateClass, "AVAILABLE");
                                                        Object descriptor = ctor.newInstance(
                                                                capabilityId,
                                                                providerIdGuess,
                                                                "gollek",
                                                                moduleId,
                                                                "inference",
                                                                Character.toUpperCase(providerIdGuess.charAt(0)) + providerIdGuess.substring(1) + " Provider",
                                                                "Dynamically registered provider from JAR: " + jarPath.toString(),
                                                                stateVal,
                                                                java.util.List.of("coding-agent", "assistant-agent"),
                                                                java.util.List.of(),
                                                                java.util.List.of("gollek", "provider", "inference"),
                                                                java.util.Map.of("jar", jarPath.toString())
                                                        );
                                                        Method registerMethod = registry.getClass().getMethod("register", descClass);
                                                        registerMethod.invoke(registry, descriptor);
                                                        if (debug) logToFile("DEBUG: Registered provider capability for: " + providerIdGuess);
                                                        found = true;
                                                        if (setMethod != null) {
                                                            setMethod.invoke(resolvedSdk, providerIdGuess);
                                                            logToFile("Applied preferred provider from ~/.wayang/config.json after registration: " + providerIdGuess);
                                                        }
                                                    }
                                                } catch (Throwable t) {
                                                    logToFile("Warning: failed to register provider capability reflectively: " + t.getMessage());
                                                }
                                            }
                                            // requery availability
                                            if (listMethod != null) {
                                                try {
                                                    Object availObj2 = listMethod.invoke(resolvedSdk);
                                                    if (availObj2 instanceof List<?> l2) {
                                                        for (Object o2 : l2) {
                                                            if (o2 instanceof String s2 && s2.equals(provider)) { found = true; break; }
                                                        }
                                                    }
                                                } catch (Throwable ignored) {
                                                }
                                            }
                                        }
                                    }
                                } catch (Throwable t) {
                                    logToFile("Warning: provider auto-load attempt failed: " + t.getMessage());
                                }
                            } else if (setMethod != null) {
                                setMethod.invoke(resolvedSdk, provider);
                                logToFile("Applied preferred provider from ~/.wayang/config.json: " + provider);
                            } else {
                                logToFile("Note: preferred provider '" + provider + "' is available, but SDK does not expose setPreferredProvider API yet.");
                            }
                        } catch (Throwable t) {
                            logToFile("Warning: failed to apply preferred provider '" + provider + "': " + t.getMessage());
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return resolvedSdk;
    }

    private void logToFile(String msg) {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(System.getProperty("user.home"), ".wayang", "logs");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path file = dir.resolve("wayang-cli.log");
            String line = java.time.Instant.now().toString() + " " + msg + System.lineSeparator();
            java.nio.file.Files.writeString(file, line, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (Exception e) {
            if (Boolean.getBoolean("wayang.cli.debug")) {
                err.println("  Logging failed: " + e.getMessage());
            }
        }
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
