package tech.kayys.wayang.tool.mcp;

import io.smallrye.mutiny.Uni;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class McpToolDiscoverySyncServiceTestDouble extends McpToolDiscoverySyncService {
    private final List<McpToolDiscoverySyncHistoryEntry> latest;
    private final List<McpToolDiscoverySyncHistoryEntry> latestSuccess;
    private final List<McpToolDiscoverySyncHistoryEntry> latestError;
    private final List<McpToolDiscoverySyncHistoryEntry> recentHistory;
    private final McpToolDiscoverySyncResult registeredResult;
    private final Throwable registeredFailure;
    private final String registeredForbiddenMessage;
    private final List<String> latestStatuses = new ArrayList<>();
    private final List<Integer> latestLimits = new ArrayList<>();
    private final List<Integer> historyLimits = new ArrayList<>();
    private String expectedRegisteredRequestId;
    private String expectedRegisteredServerName;
    private String lastRegisteredRequestId;
    private String lastRegisteredServerName;

    McpToolDiscoverySyncServiceTestDouble(
            List<McpToolDiscoverySyncHistoryEntry> latest,
            List<McpToolDiscoverySyncHistoryEntry> latestSuccess,
            List<McpToolDiscoverySyncHistoryEntry> latestError) {
        this(latest, latestSuccess, latestError, latest, null, null, null);
    }

    McpToolDiscoverySyncServiceTestDouble(
            List<McpToolDiscoverySyncHistoryEntry> latest,
            List<McpToolDiscoverySyncHistoryEntry> latestSuccess,
            List<McpToolDiscoverySyncHistoryEntry> latestError,
            List<McpToolDiscoverySyncHistoryEntry> recentHistory) {
        this(latest, latestSuccess, latestError, recentHistory, null, null, null);
    }

    private McpToolDiscoverySyncServiceTestDouble(
            List<McpToolDiscoverySyncHistoryEntry> latest,
            List<McpToolDiscoverySyncHistoryEntry> latestSuccess,
            List<McpToolDiscoverySyncHistoryEntry> latestError,
            List<McpToolDiscoverySyncHistoryEntry> recentHistory,
            McpToolDiscoverySyncResult registeredResult,
            Throwable registeredFailure,
            String registeredForbiddenMessage) {
        this.latest = latest;
        this.latestSuccess = latestSuccess;
        this.latestError = latestError;
        this.recentHistory = recentHistory;
        this.registeredResult = registeredResult;
        this.registeredFailure = registeredFailure;
        this.registeredForbiddenMessage = registeredForbiddenMessage;
    }

    static McpToolDiscoverySyncServiceTestDouble registered(McpToolDiscoverySyncResult result) {
        return new McpToolDiscoverySyncServiceTestDouble(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                result,
                null,
                null);
    }

    static McpToolDiscoverySyncServiceTestDouble failingRegistered(Throwable failure) {
        return new McpToolDiscoverySyncServiceTestDouble(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                failure,
                null);
    }

    static McpToolDiscoverySyncServiceTestDouble forbiddenRegistered(String message) {
        return new McpToolDiscoverySyncServiceTestDouble(
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                null,
                null,
                message);
    }

    McpToolDiscoverySyncServiceTestDouble expectingRegisteredServer(
            String requestId,
            String serverName) {
        expectedRegisteredRequestId = requestId;
        expectedRegisteredServerName = serverName;
        return this;
    }

    List<String> latestStatuses() {
        return latestStatuses;
    }

    List<Integer> latestLimits() {
        return latestLimits;
    }

    List<Integer> historyLimits() {
        return historyLimits;
    }

    String lastRegisteredRequestId() {
        return lastRegisteredRequestId;
    }

    String lastRegisteredServerName() {
        return lastRegisteredServerName;
    }

    @Override
    public Uni<McpToolDiscoverySyncResult> syncRegisteredServer(
            String requestId,
            String serverName) {
        lastRegisteredRequestId = requestId;
        lastRegisteredServerName = serverName;
        assertExpectedRegisteredServer(requestId, serverName);
        if (registeredForbiddenMessage != null) {
            throw new AssertionError(registeredForbiddenMessage);
        }
        if (registeredFailure != null) {
            return Uni.createFrom().failure(registeredFailure);
        }
        if (registeredResult == null) {
            return Uni.createFrom().nullItem();
        }
        return Uni.createFrom().item(registeredResult);
    }

    @Override
    public Uni<List<McpToolDiscoverySyncHistoryEntry>> listLatestHistory(
            String requestId,
            String serverName,
            String status,
            int limit) {
        latestStatuses.add(status);
        latestLimits.add(limit);
        if (McpToolDiscoverySyncStatuses.SUCCESS.equalsIgnoreCase(status)) {
            return Uni.createFrom().item(latestSuccess);
        }
        if (McpToolDiscoverySyncStatuses.ERROR.equalsIgnoreCase(status)) {
            return Uni.createFrom().item(latestError);
        }
        return Uni.createFrom().item(latest);
    }

    @Override
    public Uni<List<McpToolDiscoverySyncHistoryEntry>> listHistory(
            String requestId,
            String serverName,
            String status,
            int limit) {
        historyLimits.add(limit);
        return Uni.createFrom().item(recentHistory);
    }

    private void assertExpectedRegisteredServer(String requestId, String serverName) {
        if (expectedRegisteredRequestId != null) {
            assertEquals(expectedRegisteredRequestId, requestId);
        }
        if (expectedRegisteredServerName != null) {
            assertEquals(expectedRegisteredServerName, serverName);
        }
    }
}
