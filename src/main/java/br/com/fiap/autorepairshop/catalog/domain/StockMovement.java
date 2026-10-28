package br.com.fiap.autorepairshop.catalog.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Registro de uma alteração no estoque de uma peça ou insumo.
 *
 * @param id         identificador da movimentação
 * @param partId     item movimentado
 * @param type       origem da movimentação
 * @param quantity   variação do estoque; negativa quando o saldo diminui
 * @param occurredAt instante da movimentação
 * @param workOrderId ordem de serviço que originou a baixa; nulo em entradas e ajustes
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
