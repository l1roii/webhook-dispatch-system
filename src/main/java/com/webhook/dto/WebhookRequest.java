package com.webhook.dto;

import jakarta.validation.constraints.NotBlank;

public record WebhookRequest(
    @NotBlank String targetUrl,
    @NotBlank String eventType,
    @NotBlank String secret,
    Boolean active) {
}
