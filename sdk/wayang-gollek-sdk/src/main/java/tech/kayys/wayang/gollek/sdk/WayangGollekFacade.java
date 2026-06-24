package tech.kayys.wayang.gollek.sdk;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Facade for interacting with Gollek: prefer Gollek SDK when available on the
 * classpath; otherwise fall back to shelling out to the `gollek` CLI.
 */
public final class WayangGollekFacade {

    private WayangGollekFacade() {}

    public static List<?> listModels() throws Exception {
        Object sdk = tryCreateSdk();
        if (sdk != null) {
            Method m = sdk.getClass().getMethod("listModels");
            Object res = m.invoke(sdk);
            return (List<?>) res;
        }
        // Fallback: run `gollek list` and return as list of strings
        ProcessBuilder pb = new ProcessBuilder("gollek", "list");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            return br.lines().toList();
        } finally {
            try { p.waitFor(); } catch (InterruptedException ignored) {}
        }
    }

    public static boolean modelExists(String modelId) throws Exception {
        Object sdk = tryCreateSdk();
        if (sdk != null) {
            try {
                Method getInfo = sdk.getClass().getMethod("getModelInfo", String.class);
                Object opt = getInfo.invoke(sdk, modelId);
                if (opt instanceof Optional) {
                    return ((Optional<?>) opt).isPresent();
                }
                return opt != null;
            } catch (NoSuchMethodException ns) {
                // ignore and fall back to list
            }
            Method list = sdk.getClass().getMethod("listModels");
            Object res = list.invoke(sdk);
            if (res instanceof List) {
                for (Object o : (List<?>) res) {
                    if (o != null && o.toString().contains(modelId)) return true;
                }
            }
            return false;
        }

        // Shell fallback
        ProcessBuilder pb = new ProcessBuilder("gollek", "list");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        boolean found = false;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            String loweredModelId = modelId == null ? "" : modelId.toLowerCase();
            boolean hasSlash = loweredModelId.contains("/");
            String owner = null, name = null;
            if (hasSlash) {
                String[] parts = loweredModelId.split("/", 2);
                owner = parts[0];
                name = parts[1];
            }
            while ((line = br.readLine()) != null) {
                String low = line.toLowerCase();
                if (line == null) continue;
                // direct substring match
                if (low.contains(loweredModelId)) { found = true; break; }
                // if model specified as owner/name, match both tokens in the line
                if (hasSlash) {
                    if (owner != null && name != null && low.contains(owner) && low.contains(name)) { found = true; break; }
                    // also try matching name only (common)
                    if (name != null && low.contains(name)) { found = true; break; }
                } else {
                    // match token equality for typical 'gollek list' columns (ID OWNER NAME ...)
                    String[] toks = low.trim().split("\\s+");
                    if (toks.length >= 3) {
                        String idToken = toks[0];
                        String ownerToken = toks[1];
                        String nameToken = toks[2];
                        if (idToken.equalsIgnoreCase(loweredModelId) || nameToken.equalsIgnoreCase(loweredModelId) || ownerToken.equalsIgnoreCase(loweredModelId)) { found = true; break; }
                    }
                }
            }
        }
        p.waitFor();
        return found;
    }

    /**
     * Pull a model. Returns 0 on success, non-zero on failure.
     */
    public static int pullModel(PrintStream out, String modelSpec) {
        try {
            Object sdk = tryCreateSdk();
            if (sdk != null) {
                try {
                    Method pull = sdk.getClass().getMethod("pullModel", String.class, java.util.function.Consumer.class);
                    Consumer<Object> cb = progress -> {
                        try { out.println(String.valueOf(progress)); } catch (Throwable ignored) {}
                    };
                    pull.invoke(sdk, modelSpec, cb);
                    return 0;
                } catch (NoSuchMethodException ns) {
                    // try legacy single-arg pullModel(String)
                    try {
                        Method pull2 = sdk.getClass().getMethod("pullModel", String.class);
                        pull2.invoke(sdk, modelSpec);
                        return 0;
                    } catch (NoSuchMethodException ns2) {
                        // fallthrough
                    }
                }
            }
        } catch (Throwable t) {
            try { out.println("Warning: SDK pullModel failed: " + t.getMessage()); } catch (Throwable ignored) {}
        }

        // Shell fallback
        try {
            ProcessBuilder pb = new ProcessBuilder("gollek", "pull", modelSpec);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    out.println(line);
                }
            }
            return p.waitFor();
        } catch (IOException | InterruptedException e) {
            out.println("Error: failed to run gollek pull: " + e.getMessage());
            return 2;
        }
    }

    private static Object tryCreateSdk() {
        try {
            Class<?> factory = Class.forName("tech.kayys.gollek.factory.GollekSdkFactory");
            Method m = factory.getMethod("createLocalSdk");
            return m.invoke(null);
        } catch (Throwable t) {
            return null;
        }
    }
}
