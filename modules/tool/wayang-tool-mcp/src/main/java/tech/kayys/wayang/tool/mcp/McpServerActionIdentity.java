package tech.kayys.wayang.tool.mcp;

import java.util.Locale;

record McpServerActionIdentity(
        String actionId,
        String serverName,
        String actionCode) {

    static McpServerActionIdentity parse(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return null;
        }
        int separator = actionId.lastIndexOf(':');
        if (separator <= 0 || separator == actionId.length() - 1) {
            return null;
        }
        String serverName = actionId.substring(0, separator).trim();
        String actionCode = normalizeActionCode(actionId.substring(separator + 1));
        if (serverName.isBlank() || actionCode == null) {
            return null;
        }
        return new McpServerActionIdentity(actionId, serverName, actionCode);
    }

    static String normalizeServerName(String serverName) {
        if (serverName == null || serverName.isBlank()) {
            return null;
        }
        return serverName.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeActionCode(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            return null;
        }
        return actionCode.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    static String normalizeActionId(String actionId) {
        if (actionId == null || actionId.isBlank()) {
            return null;
        }
        McpServerActionIdentity identity = parse(actionId);
        if (identity != null) {
            return normalizeServerName(identity.serverName()) + ":" + identity.actionCode();
        }
        return actionId.trim().toLowerCase(Locale.ROOT);
    }

    String normalizedServerName() {
        return normalizeServerName(serverName);
    }

    String normalizedActionId() {
        String normalizedServerName = normalizedServerName();
        return normalizedServerName == null ? normalizeActionId(actionId) : normalizedServerName + ":" + actionCode;
    }
}
