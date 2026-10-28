package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.catalog.application.PartService;
import br.com.fiap.autorepairshop.catalog.application.ServiceOfferingService;
import br.com.fiap.autorepairshop.catalog.domain.Part;
import br.com.fiap.autorepairshop.catalog.domain.ServiceOffering;
import br.com.fiap.autorepairshop.customer.application.CustomerService;
import br.com.fiap.autorepairshop.customer.application.VehicleService;
import br.com.fiap.autorepairshop.customer.domain.Customer;
import br.com.fiap.autorepairshop.customer.domain.Vehicle;
import br.com.fiap.autorepairshop.shared.domain.BusinessRuleException;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderDetails.CustomerSummary;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderDetails.VehicleSummary;
import br.com.fiap.autorepairshop.workorder.domain.Estimate;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Casos de uso da equipe da oficina sobre a ordem de serviço.
 *
 * <p>Cliente, veículo, serviços e peças vêm dos serviços de aplicação dos contextos Cadastro e Catálogo e
 * Estoque. Uma referência inexistente é tratada como regra de negócio violada da OS, e não como recurso da
 * URL inexistente.
 */
@ApplicationScoped
public class WorkOrderService {

    private final WorkOrderRepository workOrders;
    private final CustomerService customers;
    private final VehicleService vehicles;
    private final ServiceOfferingService offerings;
    private final PartService parts;
    private final Event<EstimateIssued> estimateIssued;
    private final Clock clock;

    public WorkOrderService(WorkOrderRepository workOrders, CustomerService customers, VehicleService vehicles,
            ServiceOfferingService offerings, PartService parts, Event<EstimateIssued> estimateIssued, Clock clock) {
        this.workOrders = workOrders;
        this.customers = customers;
        this.vehicles = vehicles;
        this.offerings = offerings;
        this.parts = parts;
        this.estimateIssued = estimateIssued;
        this.clock = clock;
    }

    @Transactional
    public WorkOrderDetails open(UUID customerId, UUID vehicleId) {
        Customer customer = reference(() -> customers.get(customerId), "Cliente inexistente ou excluído.");
        Vehicle vehicle = reference(() -> vehicles.get(vehicleId), "Veículo inexistente ou excluído.");
        if (!vehicle.ownerId().equals(customer.id())) {
            throw new BusinessRuleException("O veículo informado não pertence ao cliente.");
        }
        WorkOrder workOrder = WorkOrder.open(customerId, vehicleId, now());
        workOrders.add(workOrder);
        return details(workOrder);
    }

    public WorkOrderDetails get(UUID code) {
        return details(find(code));
    }

    @Transactional
    public WorkOrderDetails startDiagnosis(UUID code) {
        return change(code, workOrder -> workOrder.startDiagnosis(now()));
    }

    @Transactional
    public WorkOrderDetails addService(UUID code, UUID serviceOfferingId) {
        ServiceOffering offering = reference(() -> offerings.get(serviceOfferingId),
                "Serviço inexistente ou excluído do catálogo.");
        return change(code, workOrder -> workOrder.addService(offering.id(), offering.name(), offering.price()));
    }

    @Transactional
    public WorkOrderDetails removeService(UUID code, UUID lineId) {
        return change(code, workOrder -> workOrder.removeServiceLine(lineId));
    }

    @Transactional
    public WorkOrderDetails addPart(UUID code, UUID partId, int quantity) {
        Part part = reference(() -> parts.get(partId), "Peça ou insumo inexistente ou excluído do catálogo.");
        return change(code, workOrder -> workOrder.addPart(part.id(), part.name(), part.unitPrice(), quantity,
                part.stockQuantity()));
    }

    @Transactional
    public WorkOrderDetails removePart(UUID code, UUID lineId) {
        return change(code, workOrder -> workOrder.removePartLine(lineId));
    }

    /**
     * Gera o orçamento e dispara {@link EstimateIssued}. O e-mail é enviado por um observador que só é
     * executado depois da confirmação desta transação.
     */
    @Transactional
    public WorkOrderDetails completeDiagnosis(UUID code) {
        WorkOrder workOrder = find(code);
        Estimate estimate = workOrder.completeDiagnosis(now());
        workOrders.update(workOrder);
        WorkOrderDetails details = details(workOrder);
        estimateIssued.fire(toEvent(details, estimate));
        return details;
    }

    @Transactional
    public WorkOrderDetails finish(UUID code) {
        return change(code, workOrder -> workOrder.finish(now()));
    }

    @Transactional
    public WorkOrderDetails deliver(UUID code) {
        return change(code, workOrder -> workOrder.deliver(now()));
    }

    WorkOrder find(UUID code) {
        return workOrders.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Ordem de serviço não encontrada."));
    }

    WorkOrderDetails details(WorkOrder workOrder) {
        Customer customer = customers.getIncludingDeleted(workOrder.customerId());
        Vehicle vehicle = vehicles.getIncludingDeleted(workOrder.vehicleId());
        return new WorkOrderDetails(workOrder,
                new CustomerSummary(customer.id(), customer.name(), customer.document().number(), customer.email()),
                new VehicleSummary(vehicle.id(), vehicle.licensePlate().value(), vehicle.brand(), vehicle.model()));
    }

    private WorkOrderDetails change(UUID code, Consumer<WorkOrder> action) {
        WorkOrder workOrder = find(code);
        action.accept(workOrder);
        workOrders.update(workOrder);
        return details(workOrder);
    }

    private static EstimateIssued toEvent(WorkOrderDetails details, Estimate estimate) {
        WorkOrder workOrder = details.workOrder();
        List<EstimateIssued.Item> items = new ArrayList<>();
        workOrder.serviceLines().forEach(line -> items.add(new EstimateIssued.Item(line.name(), 1, line.price())));
        workOrder.partLines().forEach(line -> items.add(
                new EstimateIssued.Item(line.name(), line.quantity(), line.total())));
        return new EstimateIssued(workOrder.id(), details.customer().name(), details.customer().email(),
                details.vehicle().licensePlate(), items, estimate.total());
    }

    private static <T> T reference(Supplier<T> lookup, String message) {
        try {
            return lookup.get();
        } catch (ResourceNotFoundException e) {
            throw new BusinessRuleException(message);
        }
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
