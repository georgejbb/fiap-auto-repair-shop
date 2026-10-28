package br.com.fiap.autorepairshop.workorder.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.fiap.autorepairshop.shared.domain.BusinessRuleException;
import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class WorkOrderTest {

    private static final Instant T0 = Instant.parse("2026-10-28T08:00:00Z");
    private static final Instant T1 = T0.plusSeconds(600);
    private static final Instant T2 = T0.plusSeconds(1200);
    private static final Instant T3 = T0.plusSeconds(1800);
    private static final Instant T4 = T0.plusSeconds(7200);
    private static final Instant T5 = T0.plusSeconds(9000);

    private static final UUID OIL_CHANGE = UUID.randomUUID();
    private static final UUID FILTER = UUID.randomUUID();
    private static final Money OIL_CHANGE_PRICE = money("120.00");
    private static final Money FILTER_PRICE = money("35.50");

    @Test
    void opensReceivedWithFirstHistoryEntry() {
        UUID customer = UUID.randomUUID();
        UUID vehicle = UUID.randomUUID();

        WorkOrder order = WorkOrder.open(customer, vehicle, T0);

        assertThat(order.status()).isEqualTo(WorkOrderStatus.RECEIVED);
        assertThat(order.customerId()).isEqualTo(customer);
        assertThat(order.vehicleId()).isEqualTo(vehicle);
        assertThat(order.openedAt()).isEqualTo(T0);
        assertThat(order.estimate()).isNull();
        assertThat(order.statusHistory()).containsExactly(new StatusChange(null, WorkOrderStatus.RECEIVED, T0));
    }

    @Test
    void estimateFromSpecExampleTotals191() {
        WorkOrder order = inDiagnosis();
        order.addService(OIL_CHANGE, "Troca de óleo", OIL_CHANGE_PRICE);
        order.addPart(FILTER, "Filtro de óleo", FILTER_PRICE, 2, 5);

        Estimate estimate = order.completeDiagnosis(T2);

        assertThat(estimate.total()).isEqualTo(money("191.00"));
        assertThat(estimate.generatedAt()).isEqualTo(T2);
        assertThat(estimate.decision()).isNull();
        assertThat(order.status()).isEqualTo(WorkOrderStatus.AWAITING_APPROVAL);
    }

    @Test
    void fullApprovedCycleRecordsSixStepsInOrder() {
        WorkOrder order = inDiagnosis();
        order.addService(OIL_CHANGE, "Troca de óleo", OIL_CHANGE_PRICE);
        order.completeDiagnosis(T2);
        order.approve(T3);
        order.finish(T4);
        order.deliver(T5);

        assertThat(order.status()).isEqualTo(WorkOrderStatus.DELIVERED);
        assertThat(order.estimate().decision()).isEqualTo(EstimateDecision.APPROVED);
        assertThat(order.estimate().decidedAt()).isEqualTo(T3);
        assertThat(order.statusHistory()).extracting(StatusChange::to).containsExactly(
                WorkOrderStatus.RECEIVED, WorkOrderStatus.IN_DIAGNOSIS, WorkOrderStatus.AWAITING_APPROVAL,
                WorkOrderStatus.IN_PROGRESS, WorkOrderStatus.COMPLETED, WorkOrderStatus.DELIVERED);
        assertThat(order.statusHistory().get(3)).isEqualTo(
                new StatusChange(WorkOrderStatus.AWAITING_APPROVAL, WorkOrderStatus.IN_PROGRESS, T3));
    }

    @Test
    void rejectionCompletesWithoutExecutionAndCanBeDelivered() {
        WorkOrder order = inDiagnosis();
        order.addService(OIL_CHANGE, "Troca de óleo", OIL_CHANGE_PRICE);
        order.completeDiagnosis(T2);

        order.reject(T3);

        assertThat(order.status()).isEqualTo(WorkOrderStatus.COMPLETED);
        assertThat(order.estimate().decision()).isEqualTo(EstimateDecision.REJECTED);
        assertThat(order.statusHistory()).extracting(StatusChange::to)
                .doesNotContain(WorkOrderStatus.IN_PROGRESS);

        order.deliver(T4);
        assertThat(order.status()).isEqualTo(WorkOrderStatus.DELIVERED);
    }

    static Stream<Arguments> invalidTransitions() {
        Consumer<WorkOrder> start = o -> o.startDiagnosis(T1);
        Consumer<WorkOrder> complete = o -> o.completeDiagnosis(T1);
        Consumer<WorkOrder> approve = o -> o.approve(T1);
        Consumer<WorkOrder> reject = o -> o.reject(T1);
        Consumer<WorkOrder> finish = o -> o.finish(T1);
        Consumer<WorkOrder> deliver = o -> o.deliver(T1);
        return Stream.of(
                Arguments.of("concluir diagnóstico em RECEIVED", WorkOrderStatus.RECEIVED, complete),
                Arguments.of("aprovar em RECEIVED", WorkOrderStatus.RECEIVED, approve),
                Arguments.of("rejeitar em IN_DIAGNOSIS", WorkOrderStatus.IN_DIAGNOSIS, reject),
                Arguments.of("finalizar em IN_DIAGNOSIS", WorkOrderStatus.IN_DIAGNOSIS, finish),
                Arguments.of("iniciar diagnóstico em AWAITING_APPROVAL", WorkOrderStatus.AWAITING_APPROVAL, start),
                Arguments.of("entregar em IN_PROGRESS", WorkOrderStatus.IN_PROGRESS, deliver),
                Arguments.of("aprovar em COMPLETED", WorkOrderStatus.COMPLETED, approve),
                Arguments.of("finalizar em DELIVERED", WorkOrderStatus.DELIVERED, finish));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTransitions")
    void rejectsTransitionsOutOfOrder(String description, WorkOrderStatus status, Consumer<WorkOrder> action) {
        WorkOrder order = withStatus(status);
        int historySize = order.statusHistory().size();

        assertThatThrownBy(() -> action.accept(order))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("status " + status);
        assertThat(order.status()).isEqualTo(status);
        assertThat(order.statusHistory()).hasSize(historySize);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RECEIVED", "AWAITING_APPROVAL", "IN_PROGRESS"})
    void itemsOnlyChangeDuringDiagnosis(WorkOrderStatus status) {
        WorkOrder order = withStatus(status);

        assertThatThrownBy(() -> order.addService(UUID.randomUUID(), "Alinhamento", money("80.00")))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> order.addPart(UUID.randomUUID(), "Pastilha", money("90.00"), 1, 10))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> order.removeServiceLine(UUID.randomUUID())).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> order.removePartLine(UUID.randomUUID())).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void rejectsRepeatedService() {
        WorkOrder order = inDiagnosis();
        order.addService(OIL_CHANGE, "Troca de óleo", OIL_CHANGE_PRICE);

        assertThatThrownBy(() -> order.addService(OIL_CHANGE, "Troca de óleo", OIL_CHANGE_PRICE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O serviço já está incluído nesta ordem de serviço.");
        assertThat(order.serviceLines()).hasSize(1);
    }

    @Test
    void samePartAddsUpQuantityInSingleLine() {
        WorkOrder order = inDiagnosis();
        PartLine first = order.addPart(FILTER, "Filtro de óleo", FILTER_PRICE, 2, 5);

        PartLine merged = order.addPart(FILTER, "Filtro de óleo", FILTER_PRICE, 1, 5);

        assertThat(order.partLines()).containsExactly(merged);
        assertThat(merged.id()).isEqualTo(first.id());
        assertThat(merged.quantity()).isEqualTo(3);
    }

    @Test
    void rejectsPartBeyondAvailableStock() {
        WorkOrder order = inDiagnosis();
        order.addPart(FILTER, "Filtro de óleo", FILTER_PRICE, 2, 3);

        assertThatThrownBy(() -> order.addPart(FILTER, "Filtro de óleo", FILTER_PRICE, 2, 3))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Estoque insuficiente");
        assertThat(order.partLines().getFirst().quantity()).isEqualTo(2);

        assertThatThrownBy(() -> order.addPart(UUID.randomUUID(), "Vela", money("20.00"), 1, 0))
                .isInstanceOf(BusinessRuleException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositivePartQuantity(int quantity) {
        WorkOrder order = inDiagnosis();

        assertThatThrownBy(() -> order.addPart(FILTER, "Filtro", FILTER_PRICE, quantity, 10))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("quantity"));
    }

    @Test
    void removesLines() {
        WorkOrder order = inDiagnosis();
        ServiceLine service = order.addService(OIL_CHANGE, "Troca de óleo", OIL_CHANGE_PRICE);
        PartLine part = order.addPart(FILTER, "Filtro de óleo", FILTER_PRICE, 1, 5);

        order.removeServiceLine(service.id());
        order.removePartLine(part.id());

        assertThat(order.serviceLines()).isEmpty();
        assertThat(order.partLines()).isEmpty();
    }

    @Test
    void removingUnknownLineFails() {
        WorkOrder order = inDiagnosis();

        assertThatThrownBy(() -> order.removeServiceLine(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> order.removePartLine(UUID.randomUUID()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void diagnosisWithoutItemsCannotBeCompleted() {
        WorkOrder order = inDiagnosis();

        assertThatThrownBy(() -> order.completeDiagnosis(T2))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("ao menos um serviço ou peça");
        assertThat(order.status()).isEqualTo(WorkOrderStatus.IN_DIAGNOSIS);
    }

    @Test
    void diagnosisWithOnlyPartsCanBeCompleted() {
        WorkOrder order = inDiagnosis();
        order.addPart(FILTER, "Filtro de óleo", FILTER_PRICE, 1, 5);

        assertThat(order.completeDiagnosis(T2).total()).isEqualTo(FILTER_PRICE);
    }

    @Test
    void exposedCollectionsAreCopies() {
        WorkOrder order = inDiagnosis();

        assertThatThrownBy(() -> order.serviceLines().add(null)).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> order.statusHistory().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    private static WorkOrder inDiagnosis() {
        WorkOrder order = WorkOrder.open(UUID.randomUUID(), UUID.randomUUID(), T0);
        order.startDiagnosis(T1);
        return order;
    }

    private static WorkOrder withStatus(WorkOrderStatus status) {
        WorkOrder order = WorkOrder.open(UUID.randomUUID(), UUID.randomUUID(), T0);
        if (status == WorkOrderStatus.RECEIVED) {
            return order;
        }
        order.startDiagnosis(T1);
        if (status == WorkOrderStatus.IN_DIAGNOSIS) {
            return order;
        }
        order.addService(OIL_CHANGE, "Troca de óleo", OIL_CHANGE_PRICE);
        order.completeDiagnosis(T2);
        if (status == WorkOrderStatus.AWAITING_APPROVAL) {
            return order;
        }
        order.approve(T3);
        if (status == WorkOrderStatus.IN_PROGRESS) {
            return order;
        }
        order.finish(T4);
        if (status == WorkOrderStatus.COMPLETED) {
            return order;
        }
        order.deliver(T5);
        return order;
    }

    private static Money money(String value) {
        return Money.of("price", new BigDecimal(value));
    }
}
