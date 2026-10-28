package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * Linha da listagem de ordens de serviço.
 *
 * @param code          código da OS
 * @param status        status atual
 * @param customerName  nome do cliente
 * @param licensePlate  placa do veículo
 * @param estimateTotal total do orçamento; nulo antes da conclusão do diagnóstico
 * @param openedAt      abertura da OS
 */
public record WorkOrderSummary(UUID code, WorkOrderStatus status, String customerName, String licensePlate,
        Money estimateTotal, Instant openedAt) {
}
