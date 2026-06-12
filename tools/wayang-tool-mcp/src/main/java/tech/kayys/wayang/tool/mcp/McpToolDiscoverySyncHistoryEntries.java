package tech.kayys.wayang.tool.mcp;

import tech.kayys.wayang.tool.entity.RegistrySyncHistory;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

final class McpToolDiscoverySyncHistoryEntries {

    private McpToolDiscoverySyncHistoryEntries() {
    }

    static McpToolDiscoverySyncHistoryEntry toEntry(RegistrySyncHistory history) {
        return new McpToolDiscoverySyncHistoryEntry(
                history.getSourceRef(),
                history.getStatus(),
                history.getMessage(),
                history.getItemsAffected(),
                durationMs(history),
                history.getStartedAt(),
                history.getFinishedAt());
    }

    static long durationMs(RegistrySyncHistory history) {
        if (history.getStartedAt() == null || history.getFinishedAt() == null) {
            return 0L;
        }
        return Math.max(0L, Duration.between(history.getStartedAt(), history.getFinishedAt()).toMillis());
    }

    static String serverKey(String serverName) {
        return serverName == null ? "" : serverName.toLowerCase(Locale.ROOT);
    }

    static int compareLatestEntries(
            McpToolDiscoverySyncHistoryEntry left,
            McpToolDiscoverySyncHistoryEntry right) {
        int byTime = compareHistoryTime(right, left);
        if (byTime != 0) {
            return byTime;
        }
        return serverKey(left.serverName()).compareTo(serverKey(right.serverName()));
    }

    static int compareHistoryTime(
            McpToolDiscoverySyncHistoryEntry left,
            McpToolDiscoverySyncHistoryEntry right) {
        Instant leftTime = historyTime(left);
        Instant rightTime = historyTime(right);
        if (leftTime == null && rightTime == null) {
            return 0;
        }
        if (leftTime == null) {
            return -1;
        }
        if (rightTime == null) {
            return 1;
        }
        return leftTime.compareTo(rightTime);
    }

    static boolean isNewer(
            McpToolDiscoverySyncHistoryEntry candidate,
            Instant currentStartedAt,
            Instant currentFinishedAt) {
        Instant candidateTime = historyTime(candidate);
        Instant currentTime = currentFinishedAt != null ? currentFinishedAt : currentStartedAt;
        if (currentTime == null) {
            return true;
        }
        return candidateTime != null && candidateTime.isAfter(currentTime);
    }

    static Instant historyTime(McpToolDiscoverySyncHistoryEntry entry) {
        return entry.finishedAt() != null ? entry.finishedAt() : entry.startedAt();
    }
}
