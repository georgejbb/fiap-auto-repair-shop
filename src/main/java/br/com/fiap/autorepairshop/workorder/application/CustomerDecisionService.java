package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.catalog.application.PartService;
import br.com.fiap.autorepairshop.customer.domain.Document;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import br.com.fiap.autorepairshop.workorder.domain.PartLine;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Casos de uso do cliente sobre a própria ordem de serviço: consulta e decisão sobre o orçamento.
 *
 * <p>O cliente não tem conta: identifica-se pelo código da OS e pelo documento. Código inexistente e
 * documento divergente produzem a mesma resposta, para não revelar quais códigos existem.
 */
@ApplicationScoped
public class CustomerDecisionService {

    private static final String NOT_FOUND = "Ordem de serviço não encontrada para o código e o documento informados.";

    private final WorkOrderService workOrderService;
    private final WorkOrderRepository workOrders;
    private final PartService parts;
    private final Clock clock;

    public CustomerDecisionService(WorkOrderService workOrderService, WorkOrderRepository workOrders,
            PartService parts, Clock clock) {
        this.workOrderService = workOrderService;
        this.workOrders = workOrders;
        this.parts = parts;
        this.clock = clock;
    }

    public WorkOrderDetails consult(UUID code, String document) {
        return verified(code, document);
    }

    /**
     * Aprova o orçamento e baixa o estoque de todas as peças e insumos na mesma transação. Se faltar estoque
     * de qualquer item, a exceção desfaz a aprovação e as baixas já feitas.
     */
    @Transactional
    public WorkOrderDetails approve(UUID code, String document) {
        WorkOrderDetails details = verified(code, document);
        WorkOrder workOrder = details.workOrder();
        workOrder.approve(Instant.now(clock));
        for (PartLine line : workOrder.partLines()) {
            parts.consumeForWorkOrder(line.partId(), line.quantity(), workOrder.id());
        }
        workOrders.update(workOrder);
        return details;
    }

    @Transactional
    public WorkOrderDetails reject(UUID code, String document) {
        WorkOrderDetails details = verified(code, document);
        details.workOrder().reject(Instant.now(clock));
        workOrders.update(details.workOrder());
        return details;
    }

    private WorkOrderDetails verified(UUID code, String document) {
        WorkOrder workOrder = workOrders.findByCode(code).orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
        WorkOrderDetails details = workOrderService.details(workOrder);
        if (!details.customer().document().equals(Document.normalize(document))) {
            throw new ResourceNotFoundException(NOT_FOUND);
        }
        return details;
    }
}
