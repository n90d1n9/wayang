package tech.kayys.wayang.rag.slo;

import java.time.Instant;

public record RagSloAlertSnoozeSnapshot(
        String scope,
        String fingerprint,
        Instant snoozedAt,
        Instant expiresAt,
        boolean active) {
}
