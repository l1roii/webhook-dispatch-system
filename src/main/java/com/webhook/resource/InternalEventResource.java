package com.webhook.resource;

import com.webhook.dto.EventMessage;
import com.webhook.exception.ErrorCode;
import com.webhook.service.EventProducer;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Path("/internal/events")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InternalEventResource {

    @Inject
    EventProducer eventProducer;

    @ConfigProperty(name = "internal.token")
    String internalToken;

    @POST
    public Response publish(@HeaderParam("X-Internal-Token") String token,
                            @Valid EventMessage event) {
        if (!isValidToken(token)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ErrorCode.MISSING_OR_INVALID_TOKEN.toResponse())
                    .build();
        }

        eventProducer.publish(event);
        return Response.accepted().build();
    }

    private boolean isValidToken(String token) {
        if (token == null) return false;
        byte[] provided = token.getBytes(StandardCharsets.UTF_8);
        byte[] expected = internalToken.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(provided, expected);
    }
}
