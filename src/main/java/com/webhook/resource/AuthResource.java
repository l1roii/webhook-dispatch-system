package com.webhook.resource;

import com.webhook.dto.LoginRequest;
import com.webhook.dto.TokenResponse;
import com.webhook.service.AuthService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

  @Inject
  AuthService authService;

  @POST
  @Path("/login")
  public Response login(@Valid LoginRequest req) {
    return Response.ok(new TokenResponse(authService.login(req))).build();
  }
}
