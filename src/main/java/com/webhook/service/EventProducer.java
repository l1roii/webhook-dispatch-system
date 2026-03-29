package com.webhook.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.webhook.dto.EventMessage;
import com.webhook.exception.EventPublishException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

@ApplicationScoped
public class EventProducer {

    @Inject
    @Channel("platform-events-out")
    Emitter<String> emitter;

    @Inject
    ObjectMapper objectMapper;

    public void publish(EventMessage event) {
        try {
            emitter.send(objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            throw new EventPublishException("Failed to publish event to Kafka", e);
        }
    }
}
