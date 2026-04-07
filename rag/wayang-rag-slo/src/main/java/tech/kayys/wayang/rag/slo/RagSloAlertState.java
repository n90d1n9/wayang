package tech.kayys.wayang.rag.slo;

import java.time.Instant;
import java.util.List;

public record RagSloAlertState(
        boolean alertingEnabled,
        boolean shouldAlert,
        boolean snoozed,
        String snoozeScope,
        Instant snoozeExpiresAt,
        String highestSeverity,
        int activeBreachCount,
        long cooldownMs,
        long cooldownRemainingMs,
        String fingerprint,
        String reason,
        Instant evaluatedAt,
        Instant lastAlertAt,
        List<RagSloBreach> activeBreaches) {
}
