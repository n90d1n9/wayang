package tech.kayys.wayang.rag.slo;

import java.time.Instant;
import java.util.List;
import tech.kayys.wayang.rag.runtime.RagObservabilityMetrics.RagSloSnapshot;

public record RagSloStatus(
                boolean healthy,
                RagSloThresholds thresholds,
                RagSloSnapshot snapshot,
                List<RagSloBreach> breaches,
                Instant evaluatedAt) {
}
