package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import java.time.Instant;
import java.util.UUID;

public record WorkOrderSummary(UUID code, WorkOrderStatus status, String customerName, String licensePlate,
        Money estimateTotal, Instant openedAt) {
}
