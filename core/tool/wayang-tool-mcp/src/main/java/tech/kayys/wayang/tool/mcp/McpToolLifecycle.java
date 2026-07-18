package tech.kayys.wayang.tool.mcp;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class McpToolLifecycle {

    static final String MCP_TAG = "mcp";
    static final String TOOL_TAG = "tool";
    static final String READ_CAPABILITY = "read";
    static final String STALE_TAG = "stale";
    static final String SERVER_DISABLED_TAG = "server-disabled";
    static final String SERVER_RETIRED_TAG = "server-retired";
    static final String LIFECYCLE_ACTIVE = "ACTIVE";
    static final String LIFECYCLE_DISABLED = "DISABLED";
    static final String LIFECYCLE_RETIRED = "RETIRED";
    static final String LIFECYCLE_SERVER_DISABLED = "SERVER_DISABLED";
    static final String LIFECYCLE_STALE = "STALE";

    private static final String SERVER_CAPABILITY_PREFIX = "mcp:";

    private McpToolLifecycle() {
    }

    static boolean belongsToServer(tech.kayys.wayang.tool.entity.McpTool tool, String serverName) {
        if (tool == null || serverName == null || serverName.isBlank()) {
            return false;
        }
        boolean hasMcpMarker = hasValueIgnoreCase(tool.getCapabilities(), MCP_TAG)
                || hasValueIgnoreCase(tool.getTags(), MCP_TAG);
        return hasValueIgnoreCase(tool.getCapabilities(), serverCapability(serverName))
                || (hasMcpMarker && hasValueIgnoreCase(tool.getTags(), serverName));
    }

    static String serverName(tech.kayys.wayang.tool.entity.McpTool tool) {
        if (tool == null) {
            return null;
        }
        String serverName = serverNameFromCapabilities(tool.getCapabilities());
        if (serverName == null) {
            return null;
        }
        if (tool.getTags() != null) {
            return tool.getTags().stream()
                    .filter(tag -> tag != null && tag.equalsIgnoreCase(serverName))
                    .sorted()
                    .findFirst()
                    .orElse(serverName);
        }
        return serverName;
    }

    static String serverCapability(String serverName) {
        return SERVER_CAPABILITY_PREFIX + serverName.toLowerCase(Locale.ROOT);
    }

    static Set<String> importTags(String serverName, String namespace) {
        Set<String> tags = new LinkedHashSet<>();
        tags.add(MCP_TAG);
        if (namespace != null && !namespace.isBlank()) {
            tags.add(namespace);
        }
        if (serverName != null && !serverName.isBlank()) {
            tags.add(serverName);
        }
        return Set.copyOf(tags);
    }

    static Set<String> importCapabilities(String serverName, boolean readOnly) {
        Set<String> capabilities = new LinkedHashSet<>();
        capabilities.add(MCP_TAG);
        capabilities.add(TOOL_TAG);
        if (serverName != null && !serverName.isBlank()) {
            capabilities.add(serverCapability(serverName));
        }
        if (readOnly) {
            capabilities.add(READ_CAPABILITY);
        }
        return Set.copyOf(capabilities);
    }

    static boolean isStale(tech.kayys.wayang.tool.entity.McpTool tool) {
        return tool != null
                && (hasValueIgnoreCase(tool.getTags(), STALE_TAG)
                || hasValueIgnoreCase(tool.getCapabilities(), STALE_TAG));
    }

    static boolean isServerDisabled(tech.kayys.wayang.tool.entity.McpTool tool) {
        return tool != null && hasValueIgnoreCase(tool.getTags(), SERVER_DISABLED_TAG);
    }

    static boolean isRetired(tech.kayys.wayang.tool.entity.McpTool tool) {
        return tool != null && hasValueIgnoreCase(tool.getTags(), SERVER_RETIRED_TAG);
    }

    static String lifecycleState(tech.kayys.wayang.tool.entity.McpTool tool) {
        if (isRetired(tool)) {
            return LIFECYCLE_RETIRED;
        }
        if (isStale(tool)) {
            return LIFECYCLE_STALE;
        }
        if (isServerDisabled(tool)) {
            return LIFECYCLE_SERVER_DISABLED;
        }
        if (tool == null || !tool.isEnabled()) {
            return LIFECYCLE_DISABLED;
        }
        return LIFECYCLE_ACTIVE;
    }

    static boolean hasValue(Set<String> values, String value) {
        return values != null && values.contains(value);
    }

    static boolean hasValueIgnoreCase(Set<String> values, String value) {
        return values != null && values.stream()
                .anyMatch(candidate -> candidate != null && candidate.equalsIgnoreCase(value));
    }

    static Set<String> withValue(Set<String> values, String value) {
        Set<String> copy = new LinkedHashSet<>();
        if (values != null) {
            copy.addAll(values);
        }
        copy.add(value);
        return Set.copyOf(copy);
    }

    static Set<String> withValues(Set<String> values, String... additions) {
        Set<String> copy = new LinkedHashSet<>();
        if (values != null) {
            copy.addAll(values);
        }
        for (String addition : additions) {
            copy.add(addition);
        }
        return Set.copyOf(copy);
    }

    static Set<String> withoutValue(Set<String> values, String value) {
        Set<String> copy = new LinkedHashSet<>();
        if (values != null) {
            copy.addAll(values);
        }
        copy.removeIf(candidate -> candidate != null && candidate.equalsIgnoreCase(value));
        return Set.copyOf(copy);
    }

    private static String serverNameFromCapabilities(Set<String> capabilities) {
        if (capabilities == null) {
            return null;
        }
        return capabilities.stream()
                .filter(value -> value != null
                        && value.length() > SERVER_CAPABILITY_PREFIX.length()
                        && value.regionMatches(true, 0, SERVER_CAPABILITY_PREFIX, 0, SERVER_CAPABILITY_PREFIX.length()))
                .map(value -> value.substring(SERVER_CAPABILITY_PREFIX.length()))
                .sorted()
                .findFirst()
                .orElse(null);
    }
}
