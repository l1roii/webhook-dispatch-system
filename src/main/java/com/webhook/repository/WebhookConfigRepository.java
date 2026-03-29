package com.webhook.repository;

import com.webhook.entity.WebhookConfig;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class WebhookConfigRepository implements PanacheRepository<WebhookConfig> {

  public List<WebhookConfig> findByClientId(String clientId) {
    return list("clientId", clientId);
  }

  public WebhookConfig findByIdAndClientId(UUID id, String clientId) {
    return find("id = ?1 and clientId = ?2", id, clientId).firstResult();
  }

  public List<WebhookConfig> findActiveByEventType(String eventType) {
    return list("eventType = ?1 and active = true", eventType);
  }
}
