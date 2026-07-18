package tech.kayys.wayang.tool.dto;

import java.time.Instant;

public record RegistryScheduleResponse(
        String sourceKind,
        String sourceRef,
        String interval,
        Instant lastSyncAt) {
}

