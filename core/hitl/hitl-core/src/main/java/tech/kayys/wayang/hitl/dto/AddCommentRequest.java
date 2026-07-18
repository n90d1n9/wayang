package tech.kayys.wayang.hitl.dto;

import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(
        @NotBlank String comment) {
}