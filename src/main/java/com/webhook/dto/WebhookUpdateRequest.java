package com.webhook.dto;

public record WebhookUpdateRequest(
    String targetUrl,
    String eventType,
    String secret,
    Boolean active) {
}
