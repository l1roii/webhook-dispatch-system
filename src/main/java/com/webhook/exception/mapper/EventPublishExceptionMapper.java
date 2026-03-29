package com.webhook.exception.mapper;

import com.webhook.exception.ErrorCode;
import com.webhook.exception.EventPublishException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class EventPublishExceptionMapper implements ExceptionMapper<EventPublishException> {

    private static final Logger LOG = Logger.getLogger(EventPublishExceptionMapper.class);

    @Override
    public Response toResponse(EventPublishException exception) {
        LOG.error("Failed to publish event to Kafka", exception);
        return Response.serverError()
                .type(MediaType.APPLICATION_JSON)
                .entity(ErrorCode.EVENT_PUBLISH_FAILED.toResponse())
                .build();
    }
}
