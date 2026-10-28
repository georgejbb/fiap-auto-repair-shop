package br.com.fiap.autorepairshop.workorder.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.autorepairshop.catalog.application.PartService;
import br.com.fiap.autorepairshop.shared.domain.BusinessRuleException;
import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderDetails.CustomerSummary;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderDetails.VehicleSummary;
import br.com.fiap.autorepairshop.workorder.domain.EstimateDecision;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderRepository;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerDecisionServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");
    private static final String CPF = "52998224725";

    private final WorkOrderService workOrderService = mock(WorkOrderService.class);
    private final WorkOrderRepository workOrders = mock(WorkOrderRepository.class);
    private final PartService parts = mock(PartService.class);
    private final CustomerDecisionService service =
            new CustomerDecisionService(workOrderService, workOrders, parts, Clock.fixed(NOW, ZoneOffset.UTC));

    private final UUID filter = UUID.randomUUID();
    private final UUID spark = UUID.randomUUID();
    private WorkOrder workOrder;

    @BeforeEach
    void awaitingApproval() {
        workOrder = WorkOrder.open(UUID.randomUUID(), UUID.randomUUID(), NOW);
        workOrder.startDiagnosis(NOW);
        workOrder.addPart(filter, "Filtro", money("35.50"), 2, 5);
        workOrder.addPart(spark, "Vela", money("20.00"), 4, 4);
        workOrder.completeDiagnosis(NOW);
        when(workOrders.findByCode(workOrder.id())).thenReturn(Optional.of(workOrder));
        when(workOrderService.details(workOrder)).thenReturn(new WorkOrderDetails(workOrder,
                new CustomerSummary(workOrder.customerId(), "Maria", CPF, "maria@email.com"),
                new VehicleSummary(workOrder.vehicleId(), "BRA2E19", "Fiat", "Argo")));
    }

    @Test
    void consultAcceptsFormattedDocument() {
        assertThat(service.consult(workOrder.id(), "529.982.247-25").workOrder()).isSameAs(workOrder);
    }

    @Test
    void otherDocumentAndUnknownCodeGiveSameNotFound() {
        UUID unknown = UUID.randomUUID();
        when(workOrders.findByCode(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.consult(workOrder.id(), "11144477735"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ordem de serviço não encontrada para o código e o documento informados.");
        assertThatThrownBy(() -> service.consult(unknown, CPF))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Ordem de serviço não encontrada para o código e o documento informados.");
    }

    @Test
    void approvalConsumesEveryPartAndPersists() {
        service.approve(workOrder.id(), CPF);

        assertThat(workOrder.status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(workOrder.estimate().decision()).isEqualTo(EstimateDecision.APPROVED);
        verify(parts).consumeForWorkOrder(filter, 2, workOrder.id());
        verify(parts).consumeForWorkOrder(spark, 4, workOrder.id());
        verify(workOrders).update(workOrder);
    }

    @Test
    void stockFailurePreventsPersistingTheWorkOrder() {
        doThrow(new BusinessRuleException("Estoque insuficiente de Vela"))
                .when(parts).consumeForWorkOrder(spark, 4, workOrder.id());

        assertThatThrownBy(() -> service.approve(workOrder.id(), CPF)).isInstanceOf(BusinessRuleException.class);
        verify(workOrders, never()).update(any());
    }

    @Test
    void rejectionCompletesWithoutConsumingStock() {
        service.reject(workOrder.id(), CPF);

        assertThat(workOrder.status()).isEqualTo(WorkOrderStatus.COMPLETED);
        assertThat(workOrder.estimate().decision()).isEqualTo(EstimateDecision.REJECTED);
        verify(parts, never()).consumeForWorkOrder(any(), any(Integer.class), any());
        verify(workOrders).update(workOrder);
    }

    private static Money money(String value) {
        return Money.of("price", new BigDecimal(value));
    }
}
