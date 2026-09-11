package com.phabdev.starter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

import java.util.Map;

class ProjectIntegrationTest extends IntegrationTestSupport {
    @Test
    void crudPersistsAndEnforcesOwnershipForUsersAndAdmins() throws Exception {
        var owner = register();
        var other = register();
        var result =
                mvc.perform(
                                body(
                                        post("/api/projects")
                                                .header(
                                                        "Authorization",
                                                        "Bearer " + owner.accessToken()),
                                        Map.of("name", "Original", "description", "Owned project")))
                        .andExpect(status().isCreated())
                        .andReturn();
        String id = json.readTree(result.getResponse().getContentAsString()).get("id").asString();
        mvc.perform(get("/api/projects").header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
        for (String role : java.util.List.of("USER", "ADMIN")) {
            jdbc.sql("UPDATE app_user SET role=:role WHERE id=:id")
                    .param("role", role)
                    .param("id", other.id())
                    .update();
            mvc.perform(
                            body(
                                    put("/api/projects/" + id)
                                            .header(
                                                    "Authorization",
                                                    "Bearer " + other.accessToken()),
                                    Map.of("name", "Stolen", "description", "")))
                    .andExpect(status().isNotFound());
            mvc.perform(
                            delete("/api/projects/" + id)
                                    .header("Authorization", "Bearer " + other.accessToken()))
                    .andExpect(status().isNotFound());
        }
        mvc.perform(
                        body(
                                put("/api/projects/" + id)
                                        .header("Authorization", "Bearer " + owner.accessToken()),
                                Map.of("name", "Updated", "description", "Saved")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
        mvc.perform(get("/api/projects").header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Updated"));
        mvc.perform(
                        delete("/api/projects/" + id)
                                .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/projects").header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void invalidProjectPayloadAndAnonymousAccessAreRejected() throws Exception {
        mvc.perform(get("/api/projects")).andExpect(status().isUnauthorized());
        var owner = register();
        mvc.perform(
                        body(
                                post("/api/projects")
                                        .header("Authorization", "Bearer " + owner.accessToken()),
                                Map.of("name", " ", "description", "")))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        body(
                                post("/api/projects")
                                        .header("Authorization", "Bearer " + owner.accessToken()),
                                Map.of("name", "Valid", "description", "x".repeat(2001))))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        delete("/api/projects/not-a-uuid")
                                .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isForbidden());
    }
}
