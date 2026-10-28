package br.com.fiap.autorepairshop.identity.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_user")
class AdminUserEntity {

    @Id
    UUID id;

    @Column(nullable = false, unique = true)
    String username;

    @Column(name = "password_hash", nullable = false)
    String passwordHash;

    @Column(nullable = false)
    String role;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;
}
