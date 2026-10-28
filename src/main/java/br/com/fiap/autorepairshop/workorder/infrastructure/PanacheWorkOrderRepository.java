package br.com.fiap.autorepairshop.workorder.infrastructure;

import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.workorder.domain.Estimate;
import br.com.fiap.autorepairshop.workorder.domain.PartLine;
import br.com.fiap.autorepairshop.workorder.domain.ServiceLine;
import br.com.fiap.autorepairshop.workorder.domain.StatusChange;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderRepository;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
class PanacheWorkOrderRepository implements WorkOrderRepository, PanacheRepositoryBase<WorkOrderEntity, UUID> {

    @Override
    public Optional<WorkOrder> findByCode(UUID code) {
        return findByIdOptional(code).map(PanacheWorkOrderRepository::toDomain);
    }

    @Override
    public void add(WorkOrder workOrder) {
        WorkOrderEntity entity = new WorkOrderEntity();
        entity.id = workOrder.id();
        entity.customerId = workOrder.customerId();
        entity.vehicleId = workOrder.vehicleId();
        entity.openedAt = workOrder.openedAt();
        copy(workOrder, entity);
        persistAndFlush(entity);
    }

    /**
     * Sincroniza a entidade carregada na mesma transação com o agregado. As linhas são atualizadas por id,
     * em vez de recriadas, para não remover e inserir a mesma chave no mesmo flush. A coluna version faz o
     * flush falhar se outra transação alterou a OS depois da leitura.
     */
    @Override
    public void update(WorkOrder workOrder) {
        copy(workOrder, findById(workOrder.id()));
        flush();
    }

    private static void copy(WorkOrder workOrder, WorkOrderEntity entity) {
        entity.status = workOrder.status();
        Estimate estimate = workOrder.estimate();
        entity.estimateTotal = estimate == null ? null : estimate.total().amount();
        entity.estimateGeneratedAt = estimate == null ? null : estimate.generatedAt();
        entity.estimateDecision = estimate == null ? null : estimate.decision();
        entity.estimateDecidedAt = estimate == null ? null : estimate.decidedAt();
        syncServiceLines(workOrder.serviceLines(), entity.serviceLines);
        syncPartLines(workOrder.partLines(), entity.partLines);
        appendStatusChanges(workOrder.statusHistory(), entity.statusHistory);
    }

    private static void syncServiceLines(List<ServiceLine> lines, List<ServiceLineEntity> entities) {
        Set<UUID> ids = lines.stream().map(ServiceLine::id).collect(Collectors.toSet());
        entities.removeIf(entity -> !ids.contains(entity.id));
        Map<UUID, ServiceLineEntity> existing = byId(entities, entity -> entity.id);
        for (ServiceLine line : lines) {
            if (!existing.containsKey(line.id())) {
                ServiceLineEntity entity = new ServiceLineEntity();
                entity.id = line.id();
                entity.serviceOfferingId = line.serviceOfferingId();
                entity.name = line.name();
                entity.price = line.price().amount();
                entities.add(entity);
            }
        }
    }

    private static void syncPartLines(List<PartLine> lines, List<PartLineEntity> entities) {
        Set<UUID> ids = lines.stream().map(PartLine::id).collect(Collectors.toSet());
        entities.removeIf(entity -> !ids.contains(entity.id));
        Map<UUID, PartLineEntity> existing = byId(entities, entity -> entity.id);
        for (PartLine line : lines) {
            PartLineEntity entity = existing.get(line.id());
            if (entity == null) {
                entity = new PartLineEntity();
                entity.id = line.id();
                entity.partId = line.partId();
                entity.name = line.name();
                entity.unitPrice = line.unitPrice().amount();
                entities.add(entity);
            }
            entity.quantity = line.quantity();
        }
    }

    /** O histórico só cresce: grava apenas as mudanças posteriores às já persistidas. */
    private static void appendStatusChanges(List<StatusChange> history, List<StatusChangeEntity> entities) {
        for (int position = entities.size(); position < history.size(); position++) {
            StatusChange change = history.get(position);
            StatusChangeEntity entity = new StatusChangeEntity();
            entity.id = UUID.randomUUID();
            entity.position = position;
            entity.fromStatus = change.from();
            entity.toStatus = change.to();
            entity.changedAt = change.changedAt();
            entities.add(entity);
        }
    }

    private static <T> Map<UUID, T> byId(List<T> entities, Function<T, UUID> id) {
        return entities.stream().collect(Collectors.toMap(id, Function.identity()));
    }

    private static WorkOrder toDomain(WorkOrderEntity entity) {
        Estimate estimate = entity.estimateTotal == null ? null
                : new Estimate(new Money(entity.estimateTotal), entity.estimateGeneratedAt, entity.estimateDecision,
                        entity.estimateDecidedAt);
        List<ServiceLine> services = entity.serviceLines.stream()
                .map(line -> new ServiceLine(line.id, line.serviceOfferingId, line.name, new Money(line.price)))
                .toList();
        List<PartLine> parts = entity.partLines.stream()
                .map(line -> new PartLine(line.id, line.partId, line.name, new Money(line.unitPrice), line.quantity))
                .toList();
        List<StatusChange> history = entity.statusHistory.stream()
                .map(change -> new StatusChange(change.fromStatus, change.toStatus, change.changedAt))
                .toList();
        return new WorkOrder(entity.id, entity.customerId, entity.vehicleId, entity.status, services, parts, estimate,
                history, entity.version, entity.openedAt);
    }
}
