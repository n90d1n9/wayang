package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.service.RegistrySyncService;

import java.time.Duration;
import java.time.Instant;

final class McpToolDiscoverySyncSchedule {

    private McpToolDiscoverySyncSchedule() {
    }

    static McpToolDiscoverySyncDueDecision dueDecision(McpServerRegistry server) {
        return dueDecision(server, Instant.now());
    }

    static McpToolDiscoverySyncDueDecision dueDecision(McpServerRegistry server, Instant now) {
        if (server == null || !server.isEnabled()) {
            return McpToolDiscoverySyncDueDecision.skip();
        }
        String schedule = server.getSyncSchedule();
        if (schedule == null || schedule.isBlank()) {
            return McpToolDiscoverySyncDueDecision.skip();
        }
        try {
            Duration interval = RegistrySyncService.parseInterval(schedule);
            if (server.getLastSyncAt() == null || now.isAfter(server.getLastSyncAt().plus(interval))) {
                return McpToolDiscoverySyncDueDecision.dueNow();
            }
            return McpToolDiscoverySyncDueDecision.skip();
        } catch (IllegalArgumentException error) {
            return McpToolDiscoverySyncDueDecision.warning(McpToolDiscoverySyncMessages.invalidSchedule(
                    server.getName(),
                    error.getMessage()));
        }
    }
}
