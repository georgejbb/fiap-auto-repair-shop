package br.com.fiap.autorepairshop.catalog.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Variação de estoque de um item. A quantidade é negativa quando o saldo diminui, e a OS só é informada na
 * baixa.
 */
public record StockMovement(UUID id, UUID partId, StockMovementType type, int quantity, Instant occurredAt,
        UUID workOrderId) {

    public StockMovement {
        Objects.requireNonNull(id);
        Objects.requireNonNull(partId);
        Objects.requireNonNull(type);
        Objects.requireNonNull(occurredAt);
    }

    static StockMovement of(UUID partId, StockMovementType type, int quantity, Instant occurredAt) {
        return new StockMovement(UUID.randomUUID(), partId, type, quantity, occurredAt, null);
    }
}
