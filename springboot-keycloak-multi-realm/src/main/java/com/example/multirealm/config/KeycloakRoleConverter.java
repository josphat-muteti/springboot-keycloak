package com.example.multirealm.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class KeycloakRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
  @Override
  public Collection<GrantedAuthority> convert(Jwt jwt) {
    Object realmAccessObj = jwt.getClaim("realm_access");
    if (!(realmAccessObj instanceof java.util.Map<?,?> realmAccess)) return List.of();
    Object rolesObj = realmAccess.get("roles");
    if (!(rolesObj instanceof List<?> rolesList)) return List.of();
    @SuppressWarnings("unchecked")
    List<String> roles = (List<String>) rolesList;
    return roles.stream()
        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toUpperCase()))
        .collect(Collectors.toSet());
  }
}
