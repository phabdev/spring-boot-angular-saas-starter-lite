package com.phabdev.starter.project;

import com.phabdev.starter.common.ApiException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ProjectService {
    private final ProjectRepository projects;

    public ProjectService(ProjectRepository projects) {
        this.projects = projects;
    }

    public List<ProjectController.Project> list(UUID owner) {
        return projects.list(owner);
    }

    @Transactional
    public ProjectController.Project create(UUID owner, ProjectController.SaveProject body) {
        UUID id = UUID.randomUUID();
        projects.create(id, owner, body);
        return projects.get(id, owner).orElseThrow(ApiException::notFound);
    }

    @Transactional
    public ProjectController.Project update(
            UUID owner, UUID id, ProjectController.SaveProject body) {
        if (!projects.update(id, owner, body)) throw ApiException.notFound();
        return projects.get(id, owner).orElseThrow(ApiException::notFound);
    }

    @Transactional
    public void delete(UUID owner, UUID id) {
        if (!projects.delete(id, owner)) throw ApiException.notFound();
    }
}
