package tech.kayys.wayang.gollek.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.lang.reflect.Method;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.Callable;

@Command(name = "gollek", description = "Proxy commands to the local gollek CLI.", mixinStandardHelpOptions = true,
        subcommands = { WayangGollekProxyCommand.ListCommand.class, WayangGollekProxyCommand.PullCommand.class })
public class WayangGollekProxyCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Use subcommands: list, pull");
        return 0;
    }

    static int runProcessAndPipe(PrintStream out, String... cmd) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                out.println(line);
            }
        }
        int rc = p.waitFor();
        return rc;
    }

    @Command(name = "list", description = "Delegate to 'gollek list' to show available manifests/models.")
    public static class ListCommand implements Callable<Integer> {
        @Option(names = {"--manifest", "-m"}, description = "List manifests instead of models")
        boolean manifests;

        @Override
        public Integer call() throws Exception {
            PrintStream out = System.out;
            try {
                if (manifests) {
                    // prefer SDK if available
                    try {
                        Object sdk = Class.forName("tech.kayys.gollek.factory.GollekSdkFactory").getMethod("createLocalSdk").invoke(null);
                        Method listManifests = sdk.getClass().getMethod("listModels");
                        Object res = listManifests.invoke(sdk);
                        if (res instanceof java.util.List) {
                            for (Object o : (java.util.List<?>) res) out.println(o.toString());
                        }
                        return 0;
                    } catch (Throwable t) {
                        return runProcessAndPipe(out, "gollek", "manifest", "list");
                    }
                } else {
                    try {
                        // Prefer SDK
                        java.util.List<?> models = tech.kayys.wayang.gollek.sdk.WayangGollekFacade.listModels();
                        if (models == null || models.isEmpty()) {
                            out.println("No models found.");
                            return 0;
                        }
                        for (Object m : models) out.println(m == null ? "null" : m.toString());
                        return 0;
                    } catch (Throwable t) {
                        return runProcessAndPipe(out, "gollek", "list");
                    }
                }
            } catch (IOException | InterruptedException e) {
                out.println("Error: failed to run gollek list: " + e.getMessage());
                return 2;
            }
        }
    }

    @Command(name = "pull", description = "Delegate to 'gollek pull <model>' to download model files.")
    public static class PullCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Model id to pull")
        String modelId;

        @Option(names = {"--yes", "-y"}, description = "Auto-confirm download")
        boolean yes;

        @Override
        public Integer call() throws Exception {
            PrintStream out = System.out;
            try {
                if (!yes) {
                    // simple confirmation
                    out.print("Download model '" + modelId + "'? [Y/n]: ");
                    out.flush();
                    BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
                    String line = r.readLine();
                    if (line != null && line.trim().length() > 0 && line.trim().toLowerCase().startsWith("n")) {
                        out.println("Aborted.");
                        return 0;
                    }
                }
                // Prefer SDK-backed pull when available
                try {
                    int rc = tech.kayys.wayang.gollek.sdk.WayangGollekFacade.pullModel(out, modelId);
                    return rc;
                } catch (Throwable t) {
                    return runProcessAndPipe(out, "gollek", "pull", modelId);
                }
            } catch (IOException | InterruptedException e) {
                out.println("Error: failed to run gollek pull: " + e.getMessage());
                return 2;
            }
        }
    }
}
