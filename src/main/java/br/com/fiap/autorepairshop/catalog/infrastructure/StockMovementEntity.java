package br.com.fiap.autorepairshop.catalog.infrastructure;

import br.com.fiap.autorepairshop.catalog.domain.StockMovementType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movement")
class StockMovementEntity {

    @Id
    UUID id;

    @Column(name = "part_id", nullable = false)
    UUID partId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    StockMovementType type;

    @Column(nullable = false)
    int quantity;

    @Column(name = "occurred_at", nullable = false)
    Instant occurredAt;

    @Column(name = "work_order_id")
    UUID workOrderId;
}
