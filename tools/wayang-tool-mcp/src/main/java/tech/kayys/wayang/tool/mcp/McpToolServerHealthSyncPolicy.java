package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.service.RegistrySyncService;

import java.time.Duration;
import java.time.Instant;

final class McpToolServerHealthSyncPolicy {

    private McpToolServerHealthSyncPolicy() {
    }

    static McpToolServerHealthSyncPolicyStatus from(McpServerRegistry server) {
        return from(server, Instant.now());
    }

    static McpToolServerHealthSyncPolicyStatus from(McpServerRegistry server, Instant now) {
        if (server == null) {
            return new McpToolServerHealthSyncPolicyStatus(null, false, null);
        }
        String schedule = server.getSyncSchedule();
        if (schedule == null || schedule.isBlank()) {
            return new McpToolServerHealthSyncPolicyStatus(null, false, null);
        }
        try {
            Duration interval = RegistrySyncService.parseInterval(schedule);
            if (server.getLastSyncAt() == null) {
                return new McpToolServerHealthSyncPolicyStatus(null, server.isEnabled(), null);
            }
            Instant nextSyncAt = server.getLastSyncAt().plus(interval);
            return new McpToolServerHealthSyncPolicyStatus(
                    nextSyncAt,
                    server.isEnabled() && !now.isBefore(nextSyncAt),
                    null);
        } catch (IllegalArgumentException error) {
            return new McpToolServerHealthSyncPolicyStatus(null, false, error.getMessage());
        }
    }
}
