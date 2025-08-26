package com.example.multirealm.dto;

import jakarta.validation.constraints.*;

public record CreateUserRequest(
  @NotBlank String username,
  @Email @NotBlank String email,
  @NotBlank String firstName,
  @NotBlank String lastName,
  @NotBlank String temporaryPassword
) {}
