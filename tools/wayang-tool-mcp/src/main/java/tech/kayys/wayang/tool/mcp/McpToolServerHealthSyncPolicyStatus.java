package tech.kayys.wayang.tool.mcp;

import java.time.Instant;

record McpToolServerHealthSyncPolicyStatus(
        Instant nextSyncAt,
        boolean syncDue,
        String error) {
}
