package com.webhook.dto;

import com.webhook.entity.WebhookConfig;
import java.time.LocalDateTime;
import java.util.UUID;

public record WebhookResponse(
    UUID id,
    String clientId,
    String targetUrl,
    String eventType,
    String secret,
    boolean active,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {
  public static WebhookResponse from(WebhookConfig w) {
    return new WebhookResponse(
        w.id, w.clientId, w.targetUrl, w.eventType,
        "****", w.active, w.createdAt, w.updatedAt);
  }
}
