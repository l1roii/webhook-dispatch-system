package com.webhook.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record EventMessage(
    @NotBlank String eventType,
    @NotNull Map<String, Object> payload) {
}
