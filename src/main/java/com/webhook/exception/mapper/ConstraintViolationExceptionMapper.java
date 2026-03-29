package com.webhook.exception.mapper;

import com.webhook.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.stream.Collectors;

@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    String message = exception.getConstraintViolations().stream()
        .map(v -> fieldName(v.getPropertyPath().toString()) + ": " + v.getMessage())
        .collect(Collectors.joining(", "));

    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON)
        .entity(new ErrorResponse(message))
        .build();
  }

  private String fieldName(String propertyPath) {
    String[] parts = propertyPath.split("\\.");
    return parts[parts.length - 1];
  }
}
