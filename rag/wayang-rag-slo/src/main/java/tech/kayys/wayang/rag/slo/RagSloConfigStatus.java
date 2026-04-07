package tech.kayys.wayang.rag.slo;

import java.time.Instant;

public record RagSloConfigStatus(
        RagSloThresholds thresholds,
        Instant refreshedAt) {
}
