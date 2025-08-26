package com.example.multirealm.web;

import com.example.multirealm.dto.CreateUserRequest;
import com.example.multirealm.dto.UpdateUserRequest;
import com.example.multirealm.dto.UserResponse;
import com.example.multirealm.repo.OrganizationRepository;
import com.example.multirealm.service.KeycloakAdminService;
import com.example.multirealm.service.MailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/realms/{realm}/users")
@RequiredArgsConstructor
public class UserController {

  private final KeycloakAdminService kc;
  private final OrganizationRepository orgRepo;
  private final MailService mail;

  @PostMapping
  public UserResponse create(@PathVariable String realm, @RequestBody @Valid CreateUserRequest req) {
    orgRepo.findByName(realm).orElseThrow(() -> new IllegalArgumentException("Unknown organization/realm: " + realm));
    UserResponse user = kc.createUser(realm, req);
    // notify
    mail.sendUserCreated(req.email(), req.username(), req.temporaryPassword(), realm);
    return user;
  }

  @GetMapping public List<UserResponse> list(@PathVariable String realm) { return kc.listUsers(realm); }

  @GetMapping("/{userId}") public UserResponse get(@PathVariable String realm, @PathVariable String userId) {
    return kc.getUser(realm, userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
  }

  @PutMapping("/{userId}")
  public void update(@PathVariable String realm, @PathVariable String userId, @RequestBody @Valid UpdateUserRequest req) {
    // perform update
    kc.updateUser(realm, userId, req);
    // fetch updated user details
    UserResponse u = kc.getUser(realm, userId).orElseThrow(() -> new IllegalArgumentException("User not found post-update"));
    // send to the current (possibly new) email
    mail.sendUserUpdated(u.email(), u.username(), realm);
  }

  @DeleteMapping("/{userId}")
  public void delete(@PathVariable String realm, @PathVariable String userId) {
    // get details BEFORE delete so we have email/username
    UserResponse u = kc.getUser(realm, userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    kc.deleteUser(realm, userId);
    mail.sendUserDeleted(u.email(), u.username(), realm);
  }

  @PostMapping("/{userId}/enable")
  public void enable(@PathVariable String realm, @PathVariable String userId) {
    kc.setUserEnabled(realm, userId, true);
    UserResponse u = kc.getUser(realm, userId).orElseThrow(() -> new IllegalArgumentException("User not found after enable"));
    mail.sendUserEnabled(u.email(), u.username(), realm);
  }

  @PostMapping("/{userId}/disable")
  public void disable(@PathVariable String realm, @PathVariable String userId) {
    kc.setUserEnabled(realm, userId, false);
    UserResponse u = kc.getUser(realm, userId).orElseThrow(() -> new IllegalArgumentException("User not found after disable"));
    mail.sendUserDisabled(u.email(), u.username(), realm);
  }
}
