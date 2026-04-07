package tech.kayys.wayang.hitl.dto;

import jakarta.validation.constraints.NotBlank;

public record DelegateTaskRequest(
        @NotBlank String toUserId,
        @NotBlank String reason) {
}