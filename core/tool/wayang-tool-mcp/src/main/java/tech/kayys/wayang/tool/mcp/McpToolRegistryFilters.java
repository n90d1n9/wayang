package tech.kayys.wayang.tool.mcp;

import java.util.Locale;
import java.util.Set;

record McpToolRegistryFilters(
        String namespace,
        String capability,
        String serverName,
        Boolean enabled,
        Boolean stale,
        Boolean serverDisabled,
        Boolean retired,
        String lifecycleState) {

    McpToolRegistryFilters {
        namespace = blankToNull(namespace);
        capability = blankToNull(capability);
        serverName = blankToNull(serverName);
        lifecycleState = normalizeLifecycleState(lifecycleState);
    }

    static McpToolRegistryFilters available(String namespace, String capability) {
        return new McpToolRegistryFilters(namespace, capability, null, true, null, null, null, null);
    }

    static McpToolRegistryFilters registry(
            String namespace,
            String capability,
            String serverName,
            Boolean enabled,
            Boolean stale,
            Boolean serverDisabled,
            Boolean retired,
            String lifecycleState) {
        return new McpToolRegistryFilters(
                namespace,
                capability,
                serverName,
                enabled,
                stale,
                serverDisabled,
                retired,
                lifecycleState);
    }

    boolean matches(tech.kayys.wayang.tool.entity.McpTool tool) {
        return tool != null
                && supportsCapability(tool)
                && matchesServerName(tool)
                && matchesEnabled(tool)
                && matchesStale(tool)
                && matchesServerDisabled(tool)
                && matchesRetired(tool)
                && matchesLifecycleState(tool);
    }

    private boolean supportsCapability(tech.kayys.wayang.tool.entity.McpTool tool) {
        if (capability == null) {
            return true;
        }
        Set<String> capabilities = tool.getCapabilities();
        return capabilities != null && capabilities.contains(capability);
    }

    private boolean matchesEnabled(tech.kayys.wayang.tool.entity.McpTool tool) {
        return enabled == null || tool.isEnabled() == enabled;
    }

    private boolean matchesServerName(tech.kayys.wayang.tool.entity.McpTool tool) {
        if (serverName == null) {
            return true;
        }
        String toolServerName = McpToolLifecycle.serverName(tool);
        return toolServerName != null && toolServerName.equalsIgnoreCase(serverName);
    }

    private boolean matchesStale(tech.kayys.wayang.tool.entity.McpTool tool) {
        return stale == null || McpToolLifecycle.isStale(tool) == stale;
    }

    private boolean matchesServerDisabled(tech.kayys.wayang.tool.entity.McpTool tool) {
        return serverDisabled == null || McpToolLifecycle.isServerDisabled(tool) == serverDisabled;
    }

    private boolean matchesRetired(tech.kayys.wayang.tool.entity.McpTool tool) {
        return retired == null || McpToolLifecycle.isRetired(tool) == retired;
    }

    private boolean matchesLifecycleState(tech.kayys.wayang.tool.entity.McpTool tool) {
        if (lifecycleState == null) {
            return true;
        }
        return McpToolLifecycle.lifecycleState(tool).equals(lifecycleState);
    }

    private static String normalizeLifecycleState(String lifecycleState) {
        String value = blankToNull(lifecycleState);
        return value == null ? null : value.replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
