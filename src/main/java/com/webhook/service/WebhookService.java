package com.webhook.service;

import com.webhook.dto.WebhookRequest;
import com.webhook.dto.WebhookUpdateRequest;
import com.webhook.entity.WebhookConfig;
import com.webhook.repository.WebhookConfigRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class WebhookService {

    @Inject
    WebhookConfigRepository webhookRepository;

    @Transactional
    public WebhookConfig create(String clientId, WebhookRequest req) {
        WebhookConfig webhook = new WebhookConfig();
        webhook.clientId = clientId;
        webhook.targetUrl = req.targetUrl();
        webhook.eventType = req.eventType();
        webhook.secret = req.secret();
        webhook.active = req.active() != null ? req.active() : true;
        webhookRepository.persist(webhook);
        return webhook;
    }

    public List<WebhookConfig> listForClient(String clientId) {
        return webhookRepository.findByClientId(clientId);
    }

    public Optional<WebhookConfig> findForClient(UUID id, String clientId) {
        return Optional.ofNullable(webhookRepository.findByIdAndClientId(id, clientId));
    }

    @Transactional
    public Optional<WebhookConfig> update(UUID id, String clientId, WebhookUpdateRequest req) {
        return findForClient(id, clientId).map(webhook -> {
            if (req.targetUrl() != null) webhook.targetUrl = req.targetUrl();
            if (req.eventType() != null) webhook.eventType = req.eventType();
            if (req.secret() != null) webhook.secret = req.secret();
            if (req.active() != null) webhook.active = req.active();
            return webhook;
        });
    }

    @Transactional
    public boolean delete(UUID id, String clientId) {
        return findForClient(id, clientId).map(webhook -> {
            webhookRepository.delete(webhook);
            return true;
        }).orElse(false);
    }
}
