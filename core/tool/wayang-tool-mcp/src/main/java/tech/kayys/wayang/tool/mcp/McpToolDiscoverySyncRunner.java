package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;
import tech.kayys.wayang.tool.entity.McpServerRegistry;
import tech.kayys.wayang.tool.repository.RegistrySyncHistoryRepository;

import java.time.Instant;
import java.util.Map;

final class McpToolDiscoverySyncRunner {

    private static final String SYSTEM_USER = "system-scheduler";

    private McpToolDiscoverySyncRunner() {
    }

    static Uni<McpToolDiscoveryServerSyncResult> syncServer(
            McpToolDiscoveryImportService importService,
            RegistrySyncHistoryRepository historyRepository,
            McpServerRegistry server) {
        Instant startedAt = Instant.now();
        McpToolDiscoveryImportRequest request = new McpToolDiscoveryImportRequest(
                server.getName(),
                null,
                null,
                SYSTEM_USER,
                Map.of());

        return importService.discoverAndImport(server.getRequestId(), request)
                .flatMap(result -> {
                    if (result.success()) {
                        return McpToolDiscoverySyncRecorder.recordSuccess(
                                historyRepository,
                                server,
                                result,
                                startedAt,
                                Instant.now())
                                .replaceWith(McpToolDiscoveryServerSyncResult.success(result));
                    }
                    return recordImportError(historyRepository, server, result.error(), startedAt);
                })
                .onFailure().recoverWithUni(error -> recordImportError(
                        historyRepository,
                        server,
                        errorMessage(error),
                        startedAt));
    }

    static Uni<McpToolDiscoverySyncResult> recordServerError(
            RegistrySyncHistoryRepository historyRepository,
            McpServerRegistry server,
            String message) {
        Instant startedAt = Instant.now();
        String warning = McpToolDiscoverySyncRecorder.failureWarning(server.getName(), message);
        return McpToolDiscoverySyncRecorder.recordError(
                historyRepository,
                server,
                message,
                startedAt,
                Instant.now())
                .replaceWith(McpToolDiscoverySyncResults.serverError(warning));
    }

    private static Uni<McpToolDiscoveryServerSyncResult> recordImportError(
            RegistrySyncHistoryRepository historyRepository,
            McpServerRegistry server,
            String message,
            Instant startedAt) {
        String warning = McpToolDiscoverySyncRecorder.failureWarning(server.getName(), message);
        return McpToolDiscoverySyncRecorder.recordError(
                historyRepository,
                server,
                message,
                startedAt,
                Instant.now())
                .replaceWith(McpToolDiscoveryServerSyncResult.error(warning));
    }

    private static String errorMessage(Throwable error) {
        if (error.getMessage() == null || error.getMessage().isBlank()) {
            return error.getClass().getSimpleName();
        }
        return error.getMessage();
    }
}
