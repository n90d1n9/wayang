package tech.kayys.wayang.agent.run;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import tech.kayys.wayang.client.ProductSurfacePolicy;
import tech.kayys.wayang.client.SdkText;
import tech.kayys.wayang.client.WayangProductCatalog;

public final class AgentRunSpec {

    private static final Pattern LIST_SEPARATOR = Pattern.compile("[,\\n]");
    private static final String CONTEXT_PREFIX = "context.";

    private AgentRunSpec() {
    }

    public static AgentRunRequest fromProperties(Properties properties) {
        return fromProperties(properties, AgentRunRequest.builder().build());
    }

    public static AgentRunRequest fromProperties(Properties properties, AgentRunRequest baseRequest) {
        Properties source = properties == null ? new Properties() : properties;
        AgentRunRequest.Builder builder = AgentRunRequest.builder(baseRequest);
        if (has(source, "prompt")) {
            builder.prompt(value(source, "prompt"));
        }
        if (has(source, "systemPrompt")) {
            builder.systemPrompt(value(source, "systemPrompt"));
        }
        if (has(source, "tenantId")) {
            builder.tenantId(value(source, "tenantId"));
        }
        if (has(source, "modelId")) {
            builder.modelId(value(source, "modelId"));
        }
        if (has(source, "workflowId")) {
            builder.workflowId(value(source, "workflowId"));
        }
        if (has(source, "surfaceId")) {
            builder.surfaceId(value(source, "surfaceId"));
        }
        if (has(source, "sessionId")) {
            builder.sessionId(value(source, "sessionId"));
        }
        if (has(source, "userId")) {
            builder.userId(value(source, "userId"));
        }
        context(source).forEach(builder::context);

        String skills = value(source, "skills");
        if (has(source, "skills")) {
            builder.skills(List.of());
            for (String skill : LIST_SEPARATOR.split(skills)) {
                builder.skill(skill);
            }
        }
        if (has(source, "memoryEnabled")) {
            builder.memoryEnabled(booleanValue(source, "memoryEnabled"));
        }
        if (has(source, "maxSteps")) {
            builder.maxSteps(positiveInt(source, "maxSteps"));
        }
        if (has(source, "workspacePath")) {
            builder.workspace(value(source, "workspacePath"));
        }
        if (has(source, "workspaceEnabled")) {
            builder.workspaceEnabled(booleanValue(source, "workspaceEnabled"));
        }
        if (has(source, "workspaceMaxEntries")) {
            builder.workspaceMaxEntries(positiveInt(source, "workspaceMaxEntries"));
        }
        if (has(source, "harnessEnabled")) {
            builder.harness(booleanValue(source, "harnessEnabled"));
        }
        if (has(source, "harnessMaxChecks")) {
            builder.harnessMaxChecks(positiveInt(source, "harnessMaxChecks"));
        }
        if (has(source, "harnessIncludeOptional")) {
            builder.harnessIncludeOptional(booleanValue(source, "harnessIncludeOptional"));
        }
        return builder.build();
    }

    public static AgentRunRequest template(String surfaceId) {
        ProductSurfacePolicy policy = WayangProductCatalog.policyFor(surfaceId);
        AgentRunRequest.Builder builder = AgentRunRequest.builder()
                .prompt("Describe the task here.")
                .surfaceId(policy.surfaceId())
                .skills(policy.suggestedSkills())
                .memoryEnabled(policy.memoryPreferred());
        if (policy.workspacePreferred()) {
            builder.workspace(".");
        }
        if (policy.harnessPreferred()) {
            builder.harness(true);
        }
        if (policy.workflowPreferred()) {
            builder.workflowId("gamelan-workflow");
        }
        return builder.build();
    }

    public static Properties toProperties(AgentRunRequest request) {
        AgentRunRequest source = AgentRunRequest.builder(request).build();
        Properties properties = new Properties();
        put(properties, "prompt", source.prompt());
        put(properties, "systemPrompt", source.systemPrompt());
        put(properties, "tenantId", source.tenantId());
        put(properties, "modelId", source.modelId());
        put(properties, "workflowId", source.workflowId());
        put(properties, "surfaceId", source.surfaceId());
        put(properties, "sessionId", source.sessionId());
        put(properties, "userId", source.userId());
        put(properties, "skills", String.join(",", source.skills()));
        put(properties, "memoryEnabled", String.valueOf(source.memoryEnabled()));
        put(properties, "maxSteps", String.valueOf(source.maxSteps()));
        put(properties, "workspacePath", source.workspacePath());
        put(properties, "workspaceEnabled", String.valueOf(source.workspaceEnabled()));
        put(properties, "workspaceMaxEntries", String.valueOf(source.workspaceMaxEntries()));
        put(properties, "harnessEnabled", String.valueOf(source.harnessEnabled()));
        put(properties, "harnessMaxChecks", String.valueOf(source.harnessMaxChecks()));
        put(properties, "harnessIncludeOptional", String.valueOf(source.harnessIncludeOptional()));
        source.context().forEach((key, value) -> put(properties, CONTEXT_PREFIX + key, String.valueOf(value)));
        return properties;
    }

    public static String formatProperties(AgentRunRequest request) {
        Properties properties = toProperties(request);
        StringBuilder output = new StringBuilder();
        for (String key : orderedKeys(properties)) {
            output.append(escapeKey(key))
                    .append('=')
                    .append(escapeValue(properties.getProperty(key)))
                    .append(System.lineSeparator());
        }
        return output.toString();
    }

    private static Map<String, Object> context(Properties properties) {
        Map<String, Object> context = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames()) {
            if (key.startsWith(CONTEXT_PREFIX) && key.length() > CONTEXT_PREFIX.length()) {
                context.put(key.substring(CONTEXT_PREFIX.length()), properties.getProperty(key));
            }
        }
        return context;
    }

    private static boolean has(Properties properties, String key) {
        return properties.containsKey(key);
    }

    private static void put(Properties properties, String key, String value) {
        if (value != null && !value.isBlank()) {
            properties.setProperty(key, value);
        }
    }

    private static List<String> orderedKeys(Properties properties) {
        return properties.stringPropertyNames().stream()
                .sorted((left, right) -> Integer.compare(order(left), order(right)) == 0
                        ? left.compareTo(right)
                        : Integer.compare(order(left), order(right)))
                .toList();
    }

    private static int order(String key) {
        return switch (key) {
            case "prompt" -> 10;
            case "systemPrompt" -> 20;
            case "tenantId" -> 30;
            case "modelId" -> 40;
            case "workflowId" -> 50;
            case "surfaceId" -> 60;
            case "sessionId" -> 70;
            case "userId" -> 80;
            case "skills" -> 90;
            case "memoryEnabled" -> 100;
            case "maxSteps" -> 110;
            case "workspacePath" -> 120;
            case "workspaceEnabled" -> 130;
            case "workspaceMaxEntries" -> 140;
            case "harnessEnabled" -> 150;
            case "harnessMaxChecks" -> 160;
            case "harnessIncludeOptional" -> 170;
            default -> key.startsWith(CONTEXT_PREFIX) ? 200 : 1_000;
        };
    }

    private static String escapeKey(String value) {
        return escape(value).replace(":", "\\:").replace("=", "\\=");
    }

    private static String escapeValue(String value) {
        return escape(value);
    }

    private static String escape(String value) {
        String normalized = value == null ? "" : value;
        StringBuilder output = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            switch (ch) {
                case '\\' -> output.append("\\\\");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> output.append(ch);
            }
        }
        return output.toString();
    }

    private static String value(Properties properties, String key) {
        return SdkText.trimToEmpty(properties.getProperty(key));
    }

    private static boolean booleanValue(Properties properties, String key) {
        String value = value(properties, key);
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new IllegalArgumentException("Run spec key '" + key + "' must be true or false.");
    }

    private static int positiveInt(Properties properties, String key) {
        String value = value(properties, key);
        try {
            int parsed = Integer.parseInt(value);
            if (parsed > 0) {
                return parsed;
            }
        } catch (NumberFormatException ignored) {
            // Throw the uniform message below.
        }
        throw new IllegalArgumentException("Run spec key '" + key + "' must be a positive integer.");
    }
}
