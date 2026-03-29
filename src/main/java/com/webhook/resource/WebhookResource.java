package com.webhook.resource;

import com.webhook.dto.WebhookRequest;
import com.webhook.dto.WebhookResponse;
import com.webhook.dto.WebhookUpdateRequest;
import com.webhook.service.WebhookService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.jwt.JsonWebToken;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/webhooks")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed("user")
public class WebhookResource {

    @Inject
    WebhookService webhookService;

    @Inject
    JsonWebToken jwt;

    @POST
    public Response create(@Valid WebhookRequest req) {
        WebhookResponse response = WebhookResponse.from(
                webhookService.create(jwt.getSubject(), req));
        return Response.status(Response.Status.CREATED).entity(response).build();
    }

    @GET
    public List<WebhookResponse> list() {
        return webhookService.listForClient(jwt.getSubject())
                .stream()
                .map(WebhookResponse::from)
                .toList();
    }

    @GET
    @Path("/{id}")
    public Response getOne(@PathParam("id") UUID id) {
        return webhookService.findForClient(id, jwt.getSubject())
                .map(w -> Response.ok(WebhookResponse.from(w)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") UUID id, @Valid WebhookUpdateRequest req) {
        return webhookService.update(id, jwt.getSubject(), req)
                .map(w -> Response.ok(WebhookResponse.from(w)).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") UUID id) {
        boolean deleted = webhookService.delete(id, jwt.getSubject());
        return deleted
                ? Response.noContent().build()
                : Response.status(Response.Status.NOT_FOUND).build();
    }
}
