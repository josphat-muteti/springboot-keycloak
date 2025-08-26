package com.example.multirealm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

  @Value("${keycloak.required-admin-role:admin}")
  private String requiredAdminRole;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    JwtAuthenticationConverter jwtConv = new JwtAuthenticationConverter();
    jwtConv.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());

    http
      .csrf(csrf -> csrf.disable())
      .authorizeHttpRequests(auth -> auth
          .requestMatchers("/ping", "/actuator/health").permitAll()
          .requestMatchers("/api/**").hasRole(requiredAdminRole.toUpperCase())
          .anyRequest().authenticated()
      )
      .oauth2ResourceServer(oauth2 -> oauth2.jwt(j -> j.jwtAuthenticationConverter(jwtConv)));

    return http.build();
  }
}
