package tech.kayys.wayang.gollek.cli;

import tech.kayys.wayang.gollek.sdk.WayangGollekFacade;

import java.io.PrintStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Lightweight adapter that provides a stable runtime surface for the CLI
 * without requiring the full Gollek SDK classes on the classpath.
 *
 * It prefers the local Gollek SDK when available (via WayangGollekFacade reflection),
 * otherwise falls back to shell-based operations implemented in WayangGollekFacade.
 */
public final class GollekSdkAdapter {

    private String preferredProvider;

    public GollekSdkAdapter() {}

    public static GollekSdkAdapter create(String preferredProvider) {
        GollekSdkAdapter a = new GollekSdkAdapter();
        a.preferredProvider = preferredProvider;
        return a;
    }

    public Optional<String> resolveDefaultModel() {
        // Precedence: ~/.wayang/config.json (authoritative) > environment
        try {
            java.nio.file.Path cfg = java.nio.file.Paths.get(System.getProperty("user.home"), ".wayang", "config.json");
            if (java.nio.file.Files.exists(cfg)) {
                String content = java.nio.file.Files.readString(cfg);
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\\"(?:model|defaultModel|default_model)\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
                java.util.regex.Matcher m = p.matcher(content);
                if (m.find()) {
                    String cfgModel = m.group(1).trim();
                    // Try to resolve to a local gollek model id by scanning 'gollek list' output.
                    try {
                        List<?> models = tech.kayys.wayang.gollek.sdk.WayangGollekFacade.listModels();
                        String loweredCfg = cfgModel.toLowerCase();
                        boolean hasSlash = loweredCfg.contains("/");
                        String owner = null, name = null;
                        if (hasSlash) {
                            String[] parts = loweredCfg.split("/", 2);
                            owner = parts[0];
                            name = parts[1];
                        }

                        for (Object o : models) {
                            if (o == null) continue;
                            
                            // If it's a ModelInfo object, try to extract the modelId
                            String modelIdStr = null;
                            try {
                                java.lang.reflect.Method getModelId = o.getClass().getMethod("getModelId");
                                Object id = getModelId.invoke(o);
                                if (id != null) modelIdStr = id.toString();
                            } catch (Exception e) {
                                // not a ModelInfo object, fall back to toString
                            }
                            if (modelIdStr == null) {
                                modelIdStr = o.toString();
                            }
                            
                            String line = modelIdStr;
                            // strip ANSI color sequences
                            String plain = line.replaceAll("\u001B\\[[;\\d]*m", "");
                            String low = plain.toLowerCase();

                            // direct substring or owner/name/tokenized matching (mirrors WayangGollekFacade.modelExists)
                            boolean matched = false;
                            if (low.contains(loweredCfg)) matched = true;
                            if (!matched && hasSlash) {
                                if (owner != null && name != null && low.contains(owner) && low.contains(name)) matched = true;
                                if (!matched && name != null && low.contains(name)) matched = true;
                            }
                            if (!matched) {
                                // token columns (ID OWNER NAME ...)
                                String[] toksAll = low.trim().split("\\s+");
                                if (toksAll.length >= 3) {
                                    String nameToken = toksAll[2];
                                    String lastSegment = hasSlash ? name : loweredCfg;
                                    if (nameToken.equalsIgnoreCase(lastSegment)) matched = true;
                                }
                            }

                            if (!matched) continue;

                            // extract leading ID token
                            java.util.regex.Matcher idm = java.util.regex.Pattern.compile("^\\s*([0-9a-fA-F]{4,})").matcher(plain);
                            if (idm.find()) {
                                return Optional.of(idm.group(1));
                            }
                            // fallback: first token from plain text
                            String[] toks = plain.trim().split("\\s+", 2);
                            if (toks.length > 0) return Optional.of(toks[0]);
                            return Optional.of(cfgModel);
                        }
                    } catch (Throwable ignore) {
                        // ignore failure, fallback to configured model
                    }
                    return Optional.of(cfgModel);
                }
            }
        } catch (Exception ignored) {
        }
        String env = System.getenv("WAYANG_MODEL");
        if (env != null && !env.isBlank()) return Optional.of(env.trim());
        return Optional.empty();
    }

    /**
     * Returns a stable identifier for a model row: prefers modelId (the actual
     * filename / provider key) over shortId (the hex hash).
     */
    public record ModelRow(String shortId, String name, String format, String sizeStr) {
        /** The value to pass to agent.setModelId() – same as shortId field here which stores modelId. */
        public String modelId() { return shortId; }
    }

    public List<ModelRow> listModelsStructured() {
        try {
            List<?> raw = WayangGollekFacade.listModels();
            List<ModelRow> result = new java.util.ArrayList<>();
            for (Object o : raw) {
                if (o == null) continue;

                // Attempt to extract fields via reflection (handles ModelInfo objects)
                String modelId = null;
                String shortId = null;
                String name = null;
                String format = null;
                String sizeStr = null;

                try {
                    try { modelId = invokeStrMethod(o, "getModelId"); } catch (Exception ignored) {}
                    try { shortId = invokeStrMethod(o, "getShortId"); } catch (Exception ignored) {}
                    try { name    = invokeStrMethod(o, "getName"); }    catch (Exception ignored) {}
                    try { format  = invokeStrMethod(o, "getFormat"); }  catch (Exception ignored) {}
                    try { sizeStr = invokeStrMethod(o, "getSizeFormatted"); } catch (Exception ignored) {}
                } catch (Exception ignored) {}

                if (modelId != null && !modelId.isBlank()) {
                    // Use modelId as the stable key to pass to the provider.
                    // Display shortId (hex) if available, else truncate modelId.
                    String displayId = (shortId != null && !shortId.isBlank()) ? shortId : modelId.substring(0, Math.min(8, modelId.length()));
                    String displayName = (name != null && !name.isBlank()) ? name : modelId;
                    result.add(new ModelRow(modelId, displayName, format != null ? format : "", sizeStr != null ? sizeStr : ""));
                    continue;
                }

                // Fallback: parse the toString() / plain string line
                String plain = o.toString().replaceAll("\u001B\\[[;\\d]*m", "");
                String[] toks = plain.trim().split("\\s+");
                if (toks.length >= 6) {
                    String id = toks[0];
                    if (id.equals("ID")) continue; // header
                    String tName = "";
                    String tFormat = "";
                    String tSize = "";
                    for (int i = 1; i < toks.length; i++) {
                        if (toks[i].length() > 3 && toks[i].contains("-") && tName.isEmpty()) tName = toks[i];
                        if (toks[i].equalsIgnoreCase("gguf") || toks[i].equalsIgnoreCase("safetensors") || toks[i].equalsIgnoreCase("onnx") || toks[i].equalsIgnoreCase("litert")) tFormat = toks[i];
                        if ((toks[i].equals("GB") || toks[i].equals("MB") || toks[i].equals("KB")) && i > 0) tSize = toks[i-1] + " " + toks[i];
                    }
                    result.add(new ModelRow(id, tName.isEmpty() ? toks[1] : tName, tFormat, tSize));
                } else if (toks.length >= 1) {
                    result.add(new ModelRow(toks[0], toks.length > 1 ? toks[1] : toks[0], "", ""));
                }
            }
            return result;
        } catch (Exception e) {
            return List.of();
        }
    }

    private static String invokeStrMethod(Object obj, String methodName) throws Exception {
        java.lang.reflect.Method m = obj.getClass().getMethod(methodName);
        Object res = m.invoke(obj);
        return res != null ? res.toString() : null;
    }


    public List<String> listModelsStrings() {
        try {
            List<?> raw = WayangGollekFacade.listModels();
            return raw.stream().map(Object::toString).collect(Collectors.toList());
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<String> listAvailableProviders() {
        // CLI does not have provider registry without SDK; return empty list
        return List.of();
    }

    public void setPreferredProvider(String providerId) {
        this.preferredProvider = providerId;
    }

    /**
     * Read provider configured in ~/.wayang/config.json if present.
     */
    public Optional<String> readConfigProvider() {
        try {
            java.nio.file.Path cfg = java.nio.file.Paths.get(System.getProperty("user.home"), ".wayang", "config.json");
            if (java.nio.file.Files.exists(cfg)) {
                String content = java.nio.file.Files.readString(cfg);
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\\"provider\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
                java.util.regex.Matcher m = p.matcher(content);
                if (m.find()) {
                    return Optional.of(m.group(1).trim());
                }
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    public Object getSystemInfo() {
        // Provide a minimal system info map
        return java.util.Map.of(
                "os", System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")",
                "java", System.getProperty("java.version"),
                "maxMemory", Runtime.getRuntime().maxMemory(),
                "totalMemory", Runtime.getRuntime().totalMemory()
        );
    }

    /**
     * Ensure model is available locally. Returns 0 on success (downloaded or already present), non-zero otherwise.
     */
    public int ensureModelAvailable(PrintStream out, String modelSpec) {
        return WayangGollekFacade.pullModel(out, modelSpec);
    }

    public void close() {
        // no-op for adapter
    }
}
