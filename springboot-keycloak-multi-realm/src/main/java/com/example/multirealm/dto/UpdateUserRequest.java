package com.example.multirealm.dto;

public record UpdateUserRequest(
  String email,
  String firstName,
  String lastName
) {}
