package com.phabdev.starter.project;

import com.phabdev.starter.user.UserAccount;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService projects;

    public ProjectController(ProjectService projects) {
        this.projects = projects;
    }

    public record Project(
            UUID id, String name, String description, Instant createdAt, Instant updatedAt) {}

    public record SaveProject(
            @NotBlank @Size(max = 120) String name, @Size(max = 2000) String description) {}

    @GetMapping
    public List<Project> list(@AuthenticationPrincipal UserAccount user) {
        return projects.list(user.id());
    }

    @PostMapping
    public ResponseEntity<Project> create(
            @AuthenticationPrincipal UserAccount user, @Valid @RequestBody SaveProject body) {
        return ResponseEntity.status(201).body(projects.create(user.id(), body));
    }

    @PutMapping("/{id}")
    public Project update(
            @AuthenticationPrincipal UserAccount user,
            @PathVariable UUID id,
            @Valid @RequestBody SaveProject body) {
        return projects.update(user.id(), id, body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserAccount user, @PathVariable UUID id) {
        projects.delete(user.id(), id);
        return ResponseEntity.noContent().build();
    }
}
