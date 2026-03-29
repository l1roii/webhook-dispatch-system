  CREATE TABLE webhook_config (
      id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      client_id  VARCHAR(100) NOT NULL,
      target_url VARCHAR(500) NOT NULL,
      event_type VARCHAR(100) NOT NULL,
      secret     VARCHAR(255) NOT NULL,
      active     BOOLEAN NOT NULL DEFAULT TRUE,
      created_at TIMESTAMP NOT NULL DEFAULT now(),
      updated_at TIMESTAMP NOT NULL DEFAULT now()
  );

  CREATE INDEX idx_webhook_config_client_id
      ON webhook_config (client_id);

  CREATE INDEX idx_webhook_config_event_type_active
      ON webhook_config (event_type, active);
