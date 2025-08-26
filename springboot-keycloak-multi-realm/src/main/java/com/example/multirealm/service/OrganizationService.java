package com.example.multirealm.service;

import com.example.multirealm.domain.Organization;
import com.example.multirealm.dto.OrganizationDto;
import com.example.multirealm.repo.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrganizationService {

  private final OrganizationRepository repo;
  private final KeycloakAdminService kc;

  @Transactional
  public Organization create(OrganizationDto dto) {
    if (repo.existsByName(dto.name())) throw new IllegalArgumentException("Organization already exists");
    Organization org = Organization.builder()
        .name(dto.name())
        .description(dto.description())
        .enabled(dto.enabled() == null || dto.enabled())
        .build();
    Organization saved = repo.save(org);

    //Keycloak realm exists & enabled/disabled 
    kc.ensureRealmExists(org.getName());
    kc.enableRealm(org.getName(), org.isEnabled());
    return saved;
  }

  public List<Organization> list() { return repo.findAll(); }

  public Organization get(Long id) {
    return repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Not found"));
  }

  @Transactional
  public Organization update(Long id, OrganizationDto dto) {
    var org = get(id);
    if (dto.name()!=null && !dto.name().equals(org.getName())) {
      // keep name==realm invariant in this sample
      throw new IllegalArgumentException("Renaming organization (realm) not supported");
    }
    if (dto.description()!=null) org.setDescription(dto.description());
    if (dto.enabled()!=null) {
      org.setEnabled(dto.enabled());
      kc.enableRealm(org.getName(), org.isEnabled());
    }
    return org;
  }

  @Transactional
  public void delete(Long id) {
    var org = get(id);
    // we don't delete the Keycloak realm in this sample (dangerous)
    repo.delete(org);
  }

  @Transactional
  public Organization setEnabled(Long id, boolean enabled) {
    var org = get(id);
    org.setEnabled(enabled);
    kc.enableRealm(org.getName(), enabled);
    return org;
  }
}
