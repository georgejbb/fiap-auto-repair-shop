package br.com.fiap.autorepairshop.workorder.domain;

import java.time.Instant;
import java.util.Objects;

public record StatusChange(WorkOrderStatus from, WorkOrderStatus to, Instant changedAt) {

    public StatusChange {
        Objects.requireNonNull(to);
        Objects.requireNonNull(changedAt);
    }
}
