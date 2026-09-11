package com.phabdev.starter.user;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class UserRepository {
    private final JdbcClient jdbc;

    public UserRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UserAccount> byEmail(String email) {
        return jdbc.sql("SELECT * FROM app_user WHERE email = :email")
                .param("email", email)
                .query(UserRepository::map)
                .optional();
    }

    public Optional<UserAccount> byId(UUID id) {
        return jdbc.sql("SELECT * FROM app_user WHERE id = :id")
                .param("id", id)
                .query(UserRepository::map)
                .optional();
    }

    public UserAccount create(String email, String hash, String name, Role role) {
        UUID id = UUID.randomUUID();
        jdbc.sql(
                        "INSERT INTO app_user(id,email,password_hash,display_name,role)"
                            + " VALUES(:id,:email,:hash,:name,:role)")
                .param("id", id)
                .param("email", email)
                .param("hash", hash)
                .param("name", name)
                .param("role", role.name())
                .update();
        return byId(id).orElseThrow();
    }

    public List<UserView> list() {
        return jdbc
                .sql("SELECT * FROM app_user ORDER BY created_at DESC LIMIT 200")
                .query(UserRepository::map)
                .list()
                .stream()
                .map(UserAccount::view)
                .toList();
    }

    private static UserAccount map(java.sql.ResultSet rs, int row) throws java.sql.SQLException {
        return new UserAccount(
                rs.getObject("id", UUID.class),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("display_name"),
                Role.valueOf(rs.getString("role")),
                rs.getInt("auth_version"));
    }
}
