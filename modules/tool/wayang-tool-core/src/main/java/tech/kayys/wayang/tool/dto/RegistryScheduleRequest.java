package tech.kayys.wayang.tool.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistryScheduleRequest(@NotBlank String interval) {
}

