package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.entity.RegistrySyncHistory;
import tech.kayys.wayang.tool.repository.RegistrySyncHistoryRepository;

import java.time.Instant;

final class McpToolDiscoverySyncRecorder {

    static final String SOURCE_KIND = McpToolDiscoverySyncHistorySource.MCP_TOOLS;
    static final String STATUS_SUCCESS = McpToolDiscoverySyncStatuses.SUCCESS;
    static final String STATUS_ERROR = McpToolDiscoverySyncStatuses.ERROR;

    private McpToolDiscoverySyncRecorder() {
    }

    static Uni<Void> recordSuccess(
            RegistrySyncHistoryRepository historyRepository,
            McpServerRegistry server,
            McpToolDiscoveryImportResult result,
            Instant startedAt,
            Instant finishedAt) {
        return record(
                historyRepository,
                server.getRequestId(),
                server.getName(),
                STATUS_SUCCESS,
                successMessage(result),
                affectedItems(result),
                startedAt,
                finishedAt);
    }

    static Uni<Void> recordError(
            RegistrySyncHistoryRepository historyRepository,
            McpServerRegistry server,
            String message,
            Instant startedAt,
            Instant finishedAt) {
        return record(
                historyRepository,
                server.getRequestId(),
                server.getName(),
                STATUS_ERROR,
                message,
                0,
                startedAt,
                finishedAt);
    }

    static String failureWarning(String serverName, String message) {
        return McpToolDiscoverySyncMessages.syncFailedForServer(serverName, message);
    }

    static String successMessage(McpToolDiscoveryImportResult result) {
        return McpToolDiscoverySyncMessages.success(result);
    }

    static int affectedItems(McpToolDiscoveryImportResult result) {
        return result.imported() + result.stale();
    }

    static Uni<Void> record(
            RegistrySyncHistoryRepository historyRepository,
            String requestId,
            String sourceRef,
            String status,
            String message,
            int items,
            Instant startedAt,
            Instant finishedAt) {
        return historyRepository.save(history(
                requestId,
                sourceRef,
                status,
                message,
                items,
                startedAt,
                finishedAt))
                .replaceWithVoid();
    }

    private static RegistrySyncHistory history(
            String requestId,
            String sourceRef,
            String status,
            String message,
            int items,
            Instant startedAt,
            Instant finishedAt) {
        RegistrySyncHistory history = new RegistrySyncHistory();
        history.setRequestId(requestId);
        history.setSourceKind(SOURCE_KIND);
        history.setSourceRef(sourceRef);
        history.setStatus(status);
        history.setMessage(message);
        history.setItemsAffected(items);
        history.setStartedAt(startedAt);
        history.setFinishedAt(finishedAt);
        return history;
    }
}
