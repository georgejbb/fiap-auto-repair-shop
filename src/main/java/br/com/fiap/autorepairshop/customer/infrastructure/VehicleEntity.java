package br.com.fiap.autorepairshop.customer.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicle")
class VehicleEntity {

    @Id
    UUID id;

    @Column(name = "license_plate", nullable = false)
    String licensePlate;

    @Column(nullable = false)
    String brand;

    @Column(nullable = false)
    String model;

    @Column(nullable = false)
    int year;

    @Column(name = "owner_id", nullable = false)
    UUID ownerId;

    @Column(name = "created_at", nullable = false)
    Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    Instant updatedAt;

    @Column(name = "deleted_at")
    Instant deletedAt;
}
