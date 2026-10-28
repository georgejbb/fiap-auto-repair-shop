package br.com.fiap.autorepairshop.workorder.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.autorepairshop.catalog.application.PartService;
import br.com.fiap.autorepairshop.catalog.application.ServiceOfferingService;
import br.com.fiap.autorepairshop.catalog.domain.Part;
import br.com.fiap.autorepairshop.catalog.domain.PartCategory;
import br.com.fiap.autorepairshop.catalog.domain.ServiceOffering;
import br.com.fiap.autorepairshop.customer.application.CustomerService;
import br.com.fiap.autorepairshop.customer.application.VehicleService;
import br.com.fiap.autorepairshop.customer.domain.Customer;
import br.com.fiap.autorepairshop.customer.domain.Document;
import br.com.fiap.autorepairshop.customer.domain.LicensePlate;
import br.com.fiap.autorepairshop.customer.domain.Vehicle;
import br.com.fiap.autorepairshop.shared.domain.BusinessRuleException;
import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderRepository;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import jakarta.enterprise.event.Event;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WorkOrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    private final WorkOrderRepository workOrders = mock(WorkOrderRepository.class);
    private final CustomerService customers = mock(CustomerService.class);
    private final VehicleService vehicles = mock(VehicleService.class);
    private final ServiceOfferingService offerings = mock(ServiceOfferingService.class);
    private final PartService parts = mock(PartService.class);
    @SuppressWarnings("unchecked")
    private final Event<EstimateIssued> estimateIssued = mock(Event.class);
    private final WorkOrderService service = new WorkOrderService(workOrders, customers, vehicles, offerings, parts,
            estimateIssued, Clock.fixed(NOW, ZoneOffset.UTC));

    private final Customer customer =
            Customer.register(Document.of("52998224725"), "Maria Silva", "maria@email.com", null, NOW);
    private final Vehicle vehicle =
            Vehicle.register(LicensePlate.of("BRA2E19"), "Fiat", "Argo", 2022, customer.id(), NOW);

    @BeforeEach
    void references() {
        when(customers.get(customer.id())).thenReturn(customer);
        when(customers.getIncludingDeleted(customer.id())).thenReturn(customer);
        when(vehicles.get(vehicle.id())).thenReturn(vehicle);
        when(vehicles.getIncludingDeleted(vehicle.id())).thenReturn(vehicle);
    }

    @Test
    void opensWorkOrderForCustomerVehicle() {
        WorkOrderDetails details = service.open(customer.id(), vehicle.id());

        assertThat(details.workOrder().status()).isEqualTo(WorkOrderStatus.RECEIVED);
        assertThat(details.workOrder().openedAt()).isEqualTo(NOW);
        assertThat(details.customer().name()).isEqualTo("Maria Silva");
        assertThat(details.vehicle().licensePlate()).isEqualTo("BRA2E19");
        verify(workOrders).add(details.workOrder());
    }

    @Test
    void rejectsVehicleOfAnotherCustomer() {
        Vehicle otherVehicle = Vehicle.register(LicensePlate.of("ABC1234"), "VW", "Gol", 2015, UUID.randomUUID(), NOW);
        when(vehicles.get(otherVehicle.id())).thenReturn(otherVehicle);

        assertThatThrownBy(() -> service.open(customer.id(), otherVehicle.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O veículo informado não pertence ao cliente.");
        verify(workOrders, never()).add(any());
    }

    @Test
    void unknownReferencesBecomeBusinessRuleViolations() {
        UUID unknown = UUID.randomUUID();
        when(customers.get(unknown)).thenThrow(new ResourceNotFoundException("Cliente não encontrado."));
        when(vehicles.get(unknown)).thenThrow(new ResourceNotFoundException("Veículo não encontrado."));
        when(offerings.get(unknown)).thenThrow(new ResourceNotFoundException("Serviço não encontrado."));
        when(parts.get(unknown)).thenThrow(new ResourceNotFoundException("Peça não encontrada."));

        assertThatThrownBy(() -> service.open(unknown, vehicle.id())).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.open(customer.id(), unknown)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.addService(UUID.randomUUID(), unknown))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.addPart(UUID.randomUUID(), unknown, 1))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void unknownWorkOrderIsNotFound() {
        UUID code = UUID.randomUUID();
        when(workOrders.findByCode(code)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(code))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ordem de serviço não encontrada.");
    }

    @Test
    void addsCatalogItemsWithCurrentPriceAndStock() {
        WorkOrder workOrder = stored();
        workOrder.startDiagnosis(NOW);
        ServiceOffering oilChange = ServiceOffering.create("Troca de óleo", null, money("120.00"), NOW);
        Part filter = Part.create("Filtro de óleo", PartCategory.PART, money("35.50"), NOW);
        filter.registerInbound(1, NOW);
        when(offerings.get(oilChange.id())).thenReturn(oilChange);
        when(parts.get(filter.id())).thenReturn(filter);

        service.addService(workOrder.id(), oilChange.id());
        service.addPart(workOrder.id(), filter.id(), 1);

        assertThat(workOrder.serviceLines().getFirst().price()).isEqualTo(money("120.00"));
        assertThat(workOrder.partLines().getFirst().quantity()).isEqualTo(1);
        assertThatThrownBy(() -> service.addPart(workOrder.id(), filter.id(), 1))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void transitionsArePersisted() {
        WorkOrder workOrder = stored();

        service.startDiagnosis(workOrder.id());

        assertThat(workOrder.status()).isEqualTo(WorkOrderStatus.IN_DIAGNOSIS);
        verify(workOrders).update(workOrder);
    }

    @Test
    void completingDiagnosisFiresEstimateIssued() {
        WorkOrder workOrder = stored();
        workOrder.startDiagnosis(NOW);
        workOrder.addService(UUID.randomUUID(), "Troca de óleo", money("120.00"));
        workOrder.addPart(UUID.randomUUID(), "Filtro de óleo", money("35.50"), 2, 5);

        service.completeDiagnosis(workOrder.id());

        ArgumentCaptor<EstimateIssued> event = ArgumentCaptor.forClass(EstimateIssued.class);
        verify(estimateIssued).fire(event.capture());
        assertThat(event.getValue().workOrderCode()).isEqualTo(workOrder.id());
        assertThat(event.getValue().customerEmail()).isEqualTo("maria@email.com");
        assertThat(event.getValue().licensePlate()).isEqualTo("BRA2E19");
        assertThat(event.getValue().total()).isEqualTo(money("191.00"));
        assertThat(event.getValue().items()).extracting(EstimateIssued.Item::quantity).containsExactly(1, 2);
        verify(workOrders).update(workOrder);
    }

    @Test
    void failedCompletionDoesNotFireEvent() {
        WorkOrder workOrder = stored();
        workOrder.startDiagnosis(NOW);

        assertThatThrownBy(() -> service.completeDiagnosis(workOrder.id())).isInstanceOf(BusinessRuleException.class);
        verify(estimateIssued, never()).fire(any());
    }

    @Test
    void finishesAndDelivers() {
        WorkOrder workOrder = stored();
        workOrder.startDiagnosis(NOW);
        workOrder.addService(UUID.randomUUID(), "Troca de óleo", money("120.00"));
        workOrder.completeDiagnosis(NOW);
        workOrder.approve(NOW);

        service.finish(workOrder.id());
        service.deliver(workOrder.id());

        assertThat(workOrder.status()).isEqualTo(WorkOrderStatus.DELIVERED);
    }

    @Test
    void removesLines() {
        WorkOrder workOrder = stored();
        workOrder.startDiagnosis(NOW);
        UUID serviceLine = workOrder.addService(UUID.randomUUID(), "Troca de óleo", money("120.00")).id();
        UUID partLine = workOrder.addPart(UUID.randomUUID(), "Filtro", money("35.50"), 1, 5).id();

        service.removeService(workOrder.id(), serviceLine);
        service.removePart(workOrder.id(), partLine);

        assertThat(workOrder.serviceLines()).isEmpty();
        assertThat(workOrder.partLines()).isEmpty();
    }

    private WorkOrder stored() {
        WorkOrder workOrder = WorkOrder.open(customer.id(), vehicle.id(), NOW);
        when(workOrders.findByCode(workOrder.id())).thenReturn(Optional.of(workOrder));
        return workOrder;
    }

    private static Money money(String value) {
        return Money.of("price", new BigDecimal(value));
    }
}
