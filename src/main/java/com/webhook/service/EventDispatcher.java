package com.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhook.dto.EventMessage;
import com.webhook.entity.WebhookConfig;
import com.webhook.repository.WebhookConfigRepository;
import com.webhook.util.HmacUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EventDispatcher {

    private static final Logger LOG = Logger.getLogger(EventDispatcher.class);
    private static final int CONNECT_TIMEOUT_SECONDS = 5;
    private static final int READ_TIMEOUT_SECONDS = 10;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    WebhookConfigRepository webhookRepository;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .build();

    @Incoming("platform-events-in")
    @Transactional
    public void consume(String message) {
        deserialize(message).ifPresent(this::dispatchToWebhooks);
    }

    private Optional<EventMessage> deserialize(String message) {
        try {
            return Optional.of(objectMapper.readValue(message, EventMessage.class));
        } catch (Exception e) {
            LOG.error("Failed to deserialize Kafka message", e);
            return Optional.empty();
        }
    }

    private void dispatchToWebhooks(EventMessage event) {
        MDC.put("eventType", event.eventType());
        try {
            List<WebhookConfig> webhooks = webhookRepository.findActiveByEventType(event.eventType());
            if (webhooks.isEmpty()) {
                LOG.debug("No active webhooks found for event type");
                return;
            }
            webhooks.forEach(webhook -> dispatch(webhook, event));
        } finally {
            MDC.remove("eventType");
        }
    }

    private void dispatch(WebhookConfig webhook, EventMessage event) {
        MDC.put("webhookId", webhook.id.toString());
        MDC.put("targetUrl", webhook.targetUrl);
        try {
            byte[] bodyBytes = objectMapper.writeValueAsBytes(event);
            String signature = HmacUtils.sign(bodyBytes, webhook.secret);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhook.targetUrl))
                    .timeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .header("X-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(bodyBytes))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                LOG.infof("Webhook dispatched successfully, status=%d", response.statusCode());
            } else {
                LOG.warnf("Webhook returned non-2xx response, status=%d", response.statusCode());
            }
        } catch (java.net.http.HttpTimeoutException e) {
            LOG.warn("Webhook dispatch timed out", e);
        } catch (Exception e) {
            LOG.error("Webhook dispatch failed", e);
        } finally {
            MDC.remove("webhookId");
            MDC.remove("targetUrl");
        }
    }
}
