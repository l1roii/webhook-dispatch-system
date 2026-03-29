 CREATE TABLE client (
      id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      username      VARCHAR(100) NOT NULL UNIQUE,
      password_hash VARCHAR(255) NOT NULL
  );

  Then seed two clients:
  INSERT INTO client (id, username, password_hash) VALUES
      (gen_random_uuid(), 'client_a',
  '$2a$10$GRBPsmLwxYleilSJtEkgdeB7XP5bWd50BXSrrdXJF3K6GuCOY3Fwy'),
      (gen_random_uuid(), 'client_b',
  '$2a$10$WnVYAhreb74Ps955wiqDnO90aGhKE2q1pwMKjrCdNTb2dRiIQBAse');
