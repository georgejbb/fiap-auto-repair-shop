package br.com.fiap.autorepairshop.workorder.domain;

import br.com.fiap.autorepairshop.shared.domain.BusinessRuleException;
import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Ordem de serviço: raiz do agregado que reúne os itens, o orçamento e o histórico de status.
 *
 * <p>Cada transição de status é um método que confere o status de origem e registra a mudança no
 * histórico. Os itens só podem ser alterados durante o diagnóstico.
 */
public final class WorkOrder {

    private final UUID id;
    private final UUID customerId;
    private final UUID vehicleId;
    private WorkOrderStatus status;
    private final List<ServiceLine> serviceLines;
    private final List<PartLine> partLines;
    private Estimate estimate;
    private final List<StatusChange> statusHistory;
    private final long version;
    private final Instant openedAt;

    public WorkOrder(UUID id, UUID customerId, UUID vehicleId, WorkOrderStatus status, List<ServiceLine> serviceLines,
            List<PartLine> partLines, Estimate estimate, List<StatusChange> statusHistory, long version,
            Instant openedAt) {
        this.id = Objects.requireNonNull(id);
        this.customerId = Objects.requireNonNull(customerId);
        this.vehicleId = Objects.requireNonNull(vehicleId);
        this.status = Objects.requireNonNull(status);
        this.serviceLines = new ArrayList<>(serviceLines);
        this.partLines = new ArrayList<>(partLines);
        this.estimate = estimate;
        this.statusHistory = new ArrayList<>(statusHistory);
        this.version = version;
        this.openedAt = Objects.requireNonNull(openedAt);
    }

    public static WorkOrder open(UUID customerId, UUID vehicleId, Instant now) {
        List<StatusChange> history = List.of(new StatusChange(null, WorkOrderStatus.RECEIVED, now));
        return new WorkOrder(UUID.randomUUID(), customerId, vehicleId, WorkOrderStatus.RECEIVED, List.of(), List.of(),
                null, history, 0, now);
    }

    public void startDiagnosis(Instant now) {
        requireStatus(WorkOrderStatus.RECEIVED, "iniciar o diagnóstico");
        moveTo(WorkOrderStatus.IN_DIAGNOSIS, now);
    }

    public ServiceLine addService(UUID serviceOfferingId, String name, Money price) {
        requireStatus(WorkOrderStatus.IN_DIAGNOSIS, "incluir serviços");
        boolean alreadyIncluded = serviceLines.stream()
                .anyMatch(line -> line.serviceOfferingId().equals(serviceOfferingId));
        if (alreadyIncluded) {
            throw new BusinessRuleException("O serviço já está incluído nesta ordem de serviço.");
        }
        ServiceLine line = new ServiceLine(UUID.randomUUID(), serviceOfferingId, name, price);
        serviceLines.add(line);
        return line;
    }

    public void removeServiceLine(UUID lineId) {
        requireStatus(WorkOrderStatus.IN_DIAGNOSIS, "remover serviços");
        if (!serviceLines.removeIf(line -> line.id().equals(lineId))) {
            throw new ResourceNotFoundException("Serviço não encontrado nesta ordem de serviço.");
        }
    }

    /**
     * Inclui a peça ou insumo; se o item já estiver na OS, soma à quantidade existente. O total na OS não
     * pode superar o estoque disponível informado, que é conferido de novo na aprovação.
     */
    public PartLine addPart(UUID partId, String name, Money unitPrice, int quantity, int availableStock) {
        requireStatus(WorkOrderStatus.IN_DIAGNOSIS, "incluir peças e insumos");
        if (quantity <= 0) {
            throw new InvalidValueException("quantity", "deve ser maior que zero");
        }
        int index = indexOfPart(partId);
        int total = index < 0 ? quantity : partLines.get(index).quantity() + quantity;
        if (total > availableStock) {
            throw new BusinessRuleException(
                    "Estoque insuficiente: a OS teria " + total + " unidade(s) e há " + availableStock + " em estoque.");
        }
        PartLine line = index < 0
                ? new PartLine(UUID.randomUUID(), partId, name, unitPrice, quantity)
                : partLines.get(index).withQuantity(total);
        if (index < 0) {
            partLines.add(line);
        } else {
            partLines.set(index, line);
        }
        return line;
    }

    public void removePartLine(UUID lineId) {
        requireStatus(WorkOrderStatus.IN_DIAGNOSIS, "remover peças e insumos");
        if (!partLines.removeIf(line -> line.id().equals(lineId))) {
            throw new ResourceNotFoundException("Peça ou insumo não encontrado nesta ordem de serviço.");
        }
    }

    /** Gera o orçamento com o total dos itens e coloca a OS aguardando a decisão do cliente. */
    public Estimate completeDiagnosis(Instant now) {
        requireStatus(WorkOrderStatus.IN_DIAGNOSIS, "concluir o diagnóstico");
        if (serviceLines.isEmpty() && partLines.isEmpty()) {
            throw new BusinessRuleException("Inclua ao menos um serviço ou peça antes de concluir o diagnóstico.");
        }
        Money total = Money.ZERO;
        for (ServiceLine line : serviceLines) {
            total = total.plus(line.price());
        }
        for (PartLine line : partLines) {
            total = total.plus(line.total());
        }
        estimate = Estimate.generated(total, now);
        moveTo(WorkOrderStatus.AWAITING_APPROVAL, now);
        return estimate;
    }

    /** Registra a aprovação e inicia a execução. A baixa de estoque é feita pelo caso de uso, na mesma transação. */
    public void approve(Instant now) {
        requireStatus(WorkOrderStatus.AWAITING_APPROVAL, "aprovar o orçamento");
        estimate = estimate.decide(EstimateDecision.APPROVED, now);
        moveTo(WorkOrderStatus.IN_PROGRESS, now);
    }

    /** Registra a rejeição e encerra a OS sem execução. */
    public void reject(Instant now) {
        requireStatus(WorkOrderStatus.AWAITING_APPROVAL, "rejeitar o orçamento");
        estimate = estimate.decide(EstimateDecision.REJECTED, now);
        moveTo(WorkOrderStatus.COMPLETED, now);
    }

    public void finish(Instant now) {
        requireStatus(WorkOrderStatus.IN_PROGRESS, "finalizar a execução");
        moveTo(WorkOrderStatus.COMPLETED, now);
    }

    public void deliver(Instant now) {
        requireStatus(WorkOrderStatus.COMPLETED, "registrar a entrega");
        moveTo(WorkOrderStatus.DELIVERED, now);
    }

    private void requireStatus(WorkOrderStatus expected, String action) {
        if (status != expected) {
            throw new BusinessRuleException(
                    "Não é possível " + action + " com a ordem de serviço no status " + status + ".");
        }
    }

    private void moveTo(WorkOrderStatus next, Instant now) {
        statusHistory.add(new StatusChange(status, next, now));
        status = next;
    }

    private int indexOfPart(UUID partId) {
        for (int i = 0; i < partLines.size(); i++) {
            if (partLines.get(i).partId().equals(partId)) {
                return i;
            }
        }
        return -1;
    }

    public UUID id() {
        return id;
    }

    public UUID customerId() {
        return customerId;
    }

    public UUID vehicleId() {
        return vehicleId;
    }

    public WorkOrderStatus status() {
        return status;
    }

    public List<ServiceLine> serviceLines() {
        return List.copyOf(serviceLines);
    }

    public List<PartLine> partLines() {
        return List.copyOf(partLines);
    }

    public Estimate estimate() {
        return estimate;
    }

    public List<StatusChange> statusHistory() {
        return List.copyOf(statusHistory);
    }

    public long version() {
        return version;
    }

    public Instant openedAt() {
        return openedAt;
    }
}
