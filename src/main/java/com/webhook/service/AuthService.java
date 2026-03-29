package com.webhook.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.webhook.dto.LoginRequest;
import com.webhook.entity.Client;
import com.webhook.repository.ClientRepository;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;

@ApplicationScoped
public class AuthService {

  private static final Duration TOKEN_TTL = Duration.ofHours(1);

  @Inject
  ClientRepository clientRepository;

  @ConfigProperty(name = "mp.jwt.verify.issuer")
  String issuer;

  @Transactional
  public String login(LoginRequest req) {
    Client client = clientRepository.findByUsername(req.username());
    if (client == null || !passwordMatches(req.password(), client.passwordHash)) {
      throw new NotAuthorizedException("invalid credentials");
    }

    return Jwt.issuer(issuer)
        .subject(client.username)
        .groups("user")
        .expiresIn(TOKEN_TTL)
        .sign();
  }

  private boolean passwordMatches(String plaintext, String hash) {
    return BCrypt.verifyer().verify(plaintext.toCharArray(), hash).verified;
  }
}
