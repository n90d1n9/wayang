package tech.kayys.wayang.tool.mcp;

final class McpToolDiscoverySyncStatuses {

    static final String SUCCESS = "SUCCESS";
    static final String ERROR = "ERROR";

    private McpToolDiscoverySyncStatuses() {
    }

    static boolean isSuccess(String status) {
        return matches(SUCCESS, status);
    }

    static boolean isError(String status) {
        return matches(ERROR, status);
    }

    private static boolean matches(String expected, String status) {
        return status != null && expected.equalsIgnoreCase(status);
    }
}
