package com.example.multirealm.web;

import com.example.multirealm.domain.Organization;
import com.example.multirealm.dto.OrganizationDto;
import com.example.multirealm.service.OrganizationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
public class OrganizationController {

  private final OrganizationService service;

  @PostMapping
  public ResponseEntity<Organization> create(@RequestBody @Valid OrganizationDto dto) {
    Organization created = service.create(dto);
    return ResponseEntity.created(URI.create("/api/organizations/" + created.getId())).body(created);
  }

  @GetMapping public List<Organization> list() { return service.list(); }

  @GetMapping("/{id}") public Organization get(@PathVariable Long id) { return service.get(id); }

  @PutMapping("/{id}") public Organization update(@PathVariable Long id, @RequestBody OrganizationDto dto) {
    return service.update(id, dto);
  }

  @DeleteMapping("/{id}") public void delete(@PathVariable Long id) { service.delete(id); }

  @PostMapping("/{id}/enable") public Organization enable(@PathVariable Long id) { return service.setEnabled(id, true); }

  @PostMapping("/{id}/disable") public Organization disable(@PathVariable Long id) { return service.setEnabled(id, false); }
}
