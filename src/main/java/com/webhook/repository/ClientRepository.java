package com.webhook.repository;

import com.webhook.entity.Client;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClientRepository implements PanacheRepository<Client> {

  public Client findByUsername(String username) {
    return find("username", username).firstResult();
  }
}
