package tech.kayys.wayang.tool.dto;

import java.time.Instant;

public record RegistrySyncHistoryResponse(
        String sourceKind,
        String sourceRef,
        String status,
        String message,
        int itemsAffected,
        Instant startedAt,
        Instant finishedAt) {
}

