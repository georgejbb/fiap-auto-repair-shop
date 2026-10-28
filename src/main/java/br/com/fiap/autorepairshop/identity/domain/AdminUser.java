package br.com.fiap.autorepairshop.identity.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuário com acesso às APIs administrativas. Guarda apenas o hash da senha.
 */
public final class AdminUser {

    public static final String ADMIN_ROLE = "ADMIN";

    private final UUID id;
    private final String username;
    private final String passwordHash;
    private final String role;
    private final Instant createdAt;

    public AdminUser(UUID id, String username, String passwordHash, String role, Instant createdAt) {
        this.id = Objects.requireNonNull(id);
        this.username = Objects.requireNonNull(username);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.role = Objects.requireNonNull(role);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public static AdminUser create(String username, String passwordHash, Instant now) {
        return new AdminUser(UUID.randomUUID(), username, passwordHash, ADMIN_ROLE, now);
    }

    public UUID id() {
        return id;
    }

    public String username() {
        return username;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String role() {
        return role;
    }

    public Instant createdAt() {
        return createdAt;
    }
}
