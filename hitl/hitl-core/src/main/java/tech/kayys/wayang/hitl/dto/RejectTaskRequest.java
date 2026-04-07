package tech.kayys.wayang.hitl.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

public record RejectTaskRequest(
        @NotBlank String reason,
        Map<String, Object> data) {
}