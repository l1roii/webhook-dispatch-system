package com.webhook.exception;

import com.webhook.dto.ErrorResponse;

public enum ErrorCode {

  INVALID_CREDENTIALS("invalid credentials"),
  MISSING_OR_INVALID_TOKEN("missing or invalid X-Internal-Token"),
  EVENT_PUBLISH_FAILED("failed to publish event");

  private final String message;

  ErrorCode(String message) {
    this.message = message;
  }

  public String getMessage() {
    return message;
  }

  public ErrorResponse toResponse() {
    return new ErrorResponse(message);
  }
}
