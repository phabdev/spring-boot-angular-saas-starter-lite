package com.phabdev.starter.project;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ProjectRepository {
    private final JdbcClient jdbc;

    public ProjectRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<ProjectController.Project> list(UUID owner) {
        return jdbc.sql(
                        "SELECT * FROM project WHERE owner_id=:owner ORDER BY created_at DESC LIMIT"
                            + " 200")
                .param("owner", owner)
                .query(ProjectRepository::map)
                .list();
    }

    public void create(UUID id, UUID owner, ProjectController.SaveProject body) {
        jdbc.sql(
                        "INSERT INTO project(id,owner_id,name,description)"
                            + " VALUES(:id,:owner,:name,:description)")
                .param("id", id)
                .param("owner", owner)
                .param("name", body.name().trim())
                .param("description", body.description() == null ? "" : body.description())
                .update();
    }

    public boolean update(UUID id, UUID owner, ProjectController.SaveProject body) {
        return jdbc.sql(
                                "UPDATE project SET"
                                    + " name=:name,description=:description,updated_at=CURRENT_TIMESTAMP"
                                    + " WHERE id=:id AND owner_id=:owner")
                        .param("name", body.name().trim())
                        .param("description", body.description() == null ? "" : body.description())
                        .param("id", id)
                        .param("owner", owner)
                        .update()
                > 0;
    }

    public boolean delete(UUID id, UUID owner) {
        return jdbc.sql("DELETE FROM project WHERE id=:id AND owner_id=:owner")
                        .param("id", id)
                        .param("owner", owner)
                        .update()
                > 0;
    }

    public Optional<ProjectController.Project> get(UUID id, UUID owner) {
        return jdbc.sql("SELECT * FROM project WHERE id=:id AND owner_id=:owner")
                .param("id", id)
                .param("owner", owner)
                .query(ProjectRepository::map)
                .optional();
    }

    private static ProjectController.Project map(java.sql.ResultSet rs, int row)
            throws java.sql.SQLException {
        return new ProjectController.Project(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant());
    }
}
