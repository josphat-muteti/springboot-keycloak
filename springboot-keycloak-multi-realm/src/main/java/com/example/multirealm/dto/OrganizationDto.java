package com.example.multirealm.dto;

import jakarta.validation.constraints.NotBlank;

public record OrganizationDto(
  Long id,
  @NotBlank String name,
  String description,
  Boolean enabled
) {}
