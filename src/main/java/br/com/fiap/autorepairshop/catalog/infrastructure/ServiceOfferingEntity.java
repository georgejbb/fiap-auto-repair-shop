package br.com.fiap.autorepairshop.catalog.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "service_offering")
class ServiceOfferingEntity {

    @Id
    UUID id;

    @Column(nullable = false)
    String name;

    String description;

    @Column(nullable = false, precision = 12, scale = 2)
    BigDecimal price;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "deleted_at")
    Instant deletedAt;
}
