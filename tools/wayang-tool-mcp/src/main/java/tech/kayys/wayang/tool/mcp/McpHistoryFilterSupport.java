package tech.kayys.wayang.tool.mcp;

import java.time.Instant;
import java.time.format.DateTimeParseException;

final class McpHistoryFilterSupport {

    private McpHistoryFilterSupport() {
    }

    static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    static Instant parseInstant(String value) {
        String normalized = blankToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return Instant.parse(normalized);
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    static boolean matchesInstantRange(
            Instant candidate,
            Instant from,
            Instant to) {
        if (from == null && to == null) {
            return true;
        }
        if (candidate == null) {
            return false;
        }
        return (from == null || !candidate.isBefore(from))
                && (to == null || !candidate.isAfter(to));
    }

    static int boundedPageLimit(
            int limit,
            int defaultLimit,
            int maxLimit) {
        if (limit <= 0) {
            return defaultLimit;
        }
        return Math.min(limit, maxLimit);
    }

    static int boundedPageOffset(
            Integer offset,
            int defaultOffset,
            int maxOffset) {
        if (offset == null || offset <= 0) {
            return defaultOffset;
        }
        return Math.min(offset, maxOffset);
    }
}
