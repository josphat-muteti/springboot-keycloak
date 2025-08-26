package com.example.multirealm.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "organization")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Organization {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String name; // (will be equal to Keycloak realm later)

  @Column(length = 2000)
  private String description;

  @Column(nullable = false)
  private boolean enabled = true;
}
