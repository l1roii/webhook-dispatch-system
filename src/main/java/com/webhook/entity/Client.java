package com.webhook.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "client")
public class Client extends PanacheEntityBase {

  @Id
  @GeneratedValue
  public UUID id;

  @Column(nullable = false, unique = true)
  public String username;

  @Column(name = "password_hash", nullable = false)
  public String passwordHash;
}
