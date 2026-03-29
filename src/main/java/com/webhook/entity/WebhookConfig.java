package com.webhook.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_config")
public class WebhookConfig extends PanacheEntityBase {

  @Id
  @GeneratedValue
  public UUID id;

  @Column(name = "client_id", nullable = false)
  public String clientId;

  @Column(name = "target_url", nullable = false)
  public String targetUrl;

  @Column(name = "event_type", nullable = false)
  public String eventType;

  @Column(nullable = false)
  public String secret;

  @Column(nullable = false)
  public boolean active = true;

  @Column(name = "created_at", nullable = false)
  public LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  public LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
