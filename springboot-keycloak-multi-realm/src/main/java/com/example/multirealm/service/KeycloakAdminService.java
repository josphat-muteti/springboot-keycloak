package com.example.multirealm.service;

import com.example.multirealm.dto.CreateUserRequest;
import com.example.multirealm.dto.UpdateUserRequest;
import com.example.multirealm.dto.UserResponse;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KeycloakAdminService {

  @Value("${keycloak.url}") private String keycloakBaseUrl;
  @Value("${keycloak.admin.realm}") private String adminRealm;
  @Value("${keycloak.admin.client-id}") private String adminClientId;
  @Value("${keycloak.admin.username}") private String adminUsername;
  @Value("${keycloak.admin.password}") private String adminPassword;

  private Keycloak admin;

  @PostConstruct
  void init() {
    admin = KeycloakBuilder.builder()
        .serverUrl(keycloakBaseUrl)
        .realm(adminRealm)
        .clientId(adminClientId)
        .username(adminUsername)
        .password(adminPassword)
        .grantType(OAuth2Constants.PASSWORD)
        .build();
  }

  /** Create realm if missing (maps 1:1 to organization). */
  public void ensureRealmExists(String realmName) {
    var realms = admin.realms().findAll();
    boolean exists = realms.stream().anyMatch(r -> realmName.equals(r.getRealm()));
    if (exists) return;

    RealmRepresentation rep = new RealmRepresentation();
    rep.setRealm(realmName);
    rep.setEnabled(true);
    rep.setRegistrationAllowed(false);

    admin.realms().create(rep);
  }

  public void enableRealm(String realmName, boolean enabled) {
    var rep = admin.realm(realmName).toRepresentation();
    rep.setEnabled(enabled);
    admin.realm(realmName).update(rep);
  }

  public UserResponse createUser(String realmName, CreateUserRequest req) {
    ensureRealmExists(realmName);

    UserRepresentation user = new UserRepresentation();
    user.setUsername(req.username());
    user.setEmail(req.email());
    user.setFirstName(req.firstName());
    user.setLastName(req.lastName());
    user.setEnabled(true);
    user.setEmailVerified(false);

    Response resp = admin.realm(realmName).users().create(user);
    if (resp.getStatus() >= 300) throw new RuntimeException("Failed to create user: " + resp.getStatus());
    String userId = CreatedResponseUtil.getCreatedId(resp);

    CredentialRepresentation cred = new CredentialRepresentation();
    cred.setType(CredentialRepresentation.PASSWORD);
    cred.setTemporary(true);
    cred.setValue(req.temporaryPassword());
    admin.realm(realmName).users().get(userId).resetPassword(cred);

    var u = admin.realm(realmName).users().get(userId).toRepresentation();
    return new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFirstName(), u.getLastName(), Boolean.TRUE.equals(u.isEnabled()));
  }

  public List<UserResponse> listUsers(String realmName) {
    return admin.realm(realmName).users().list().stream()
        .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFirstName(), u.getLastName(), Boolean.TRUE.equals(u.isEnabled())))
        .toList();
  }

  public Optional<UserResponse> getUser(String realmName, String userId) {
    try {
      var u = admin.realm(realmName).users().get(userId).toRepresentation();
      return Optional.of(new UserResponse(u.getId(), u.getUsername(), u.getEmail(), u.getFirstName(), u.getLastName(), Boolean.TRUE.equals(u.isEnabled())));
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public void updateUser(String realmName, String userId, UpdateUserRequest req) {
    var u = admin.realm(realmName).users().get(userId).toRepresentation();
    if (req.email()!=null) u.setEmail(req.email());
    if (req.firstName()!=null) u.setFirstName(req.firstName());
    if (req.lastName()!=null) u.setLastName(req.lastName());
    admin.realm(realmName).users().get(userId).update(u);
  }

  public void setUserEnabled(String realmName, String userId, boolean enabled) {
    var u = admin.realm(realmName).users().get(userId).toRepresentation();
    u.setEnabled(enabled);
    admin.realm(realmName).users().get(userId).update(u);
  }

  public void deleteUser(String realmName, String userId) {
    admin.realm(realmName).users().get(userId).remove();
  }
}
