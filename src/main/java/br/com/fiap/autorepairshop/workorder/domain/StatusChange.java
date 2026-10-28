package br.com.fiap.autorepairshop.workorder.domain;

import java.time.Instant;
import java.util.Objects;

/**
 * Registro de uma mudança de status da OS.
 *
 * @param from      status de origem; nulo na abertura
 * @param to        status de destino
 * @param changedAt instante da mudança
 */
public record StatusChange(WorkOrderStatus from, WorkOrderStatus to, Instant changedAt) {

    public StatusChange {
        Objects.requireNonNull(to);
        Objects.requireNonNull(changedAt);
    }
}
