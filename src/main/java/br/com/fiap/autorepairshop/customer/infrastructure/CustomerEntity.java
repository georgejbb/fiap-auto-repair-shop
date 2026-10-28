package br.com.fiap.autorepairshop.customer.infrastructure;

import br.com.fiap.autorepairshop.customer.domain.DocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customer")
class CustomerEntity {

    @Id
    UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    DocumentType documentType;

    @Column(nullable = false)
    String document;

    @Column(nullable = false)
    String name;

    @Column(nullable = false)
    String email;

    String phone;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "deleted_at")
    Instant deletedAt;
}
