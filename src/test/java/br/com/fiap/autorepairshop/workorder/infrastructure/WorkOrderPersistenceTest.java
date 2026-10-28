package br.com.fiap.autorepairshop.workorder.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import br.com.fiap.autorepairshop.catalog.application.PartService;
import br.com.fiap.autorepairshop.catalog.application.ServiceOfferingService;
import br.com.fiap.autorepairshop.catalog.domain.PartCategory;
import br.com.fiap.autorepairshop.customer.application.CustomerService;
import br.com.fiap.autorepairshop.customer.application.VehicleService;
import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.support.TestDocuments;
import br.com.fiap.autorepairshop.workorder.domain.EstimateDecision;
import br.com.fiap.autorepairshop.workorder.domain.PartLine;
import br.com.fiap.autorepairshop.workorder.domain.ServiceLine;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderRepository;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WorkOrderPersistenceTest {

    private static final Instant T0 = Instant.parse("2026-10-28T08:00:00Z");

    @Inject
    WorkOrderRepository repository;

    @Inject
    CustomerService customers;

    @Inject
    VehicleService vehicles;

    @Inject
    ServiceOfferingService offerings;

    @Inject
    PartService parts;

    @Test
    void persistsAndReloadsAggregateAcrossTransactions() {
        UUID customer = customers.register(TestDocuments.cpf(), "Maria", "maria@email.com", null).id();
        UUID vehicle = vehicles.register(TestDocuments.mercosulPlate(), "Fiat", "Argo", 2022, customer).id();
        UUID oilChange = offerings.create("Troca de óleo", null, new BigDecimal("120.00")).id();
        UUID filter = parts.create("Filtro de óleo", PartCategory.PART, new BigDecimal("35.50")).id();
        UUID spark = parts.create("Vela", PartCategory.PART, new BigDecimal("20.00")).id();

        WorkOrder opened = WorkOrder.open(customer, vehicle, T0);
        QuarkusTransaction.requiringNew().run(() -> repository.add(opened));

        UUID[] sparkLine = new UUID[1];
        QuarkusTransaction.requiringNew().run(() -> {
            WorkOrder order = repository.findByCode(opened.id()).orElseThrow();
            order.startDiagnosis(T0.plusSeconds(60));
            order.addService(oilChange, "Troca de óleo", money("120.00"));
            order.addPart(filter, "Filtro de óleo", money("35.50"), 1, 10);
            sparkLine[0] = order.addPart(spark, "Vela", money("20.00"), 4, 10).id();
            repository.update(order);
        });

        QuarkusTransaction.requiringNew().run(() -> {
            WorkOrder order = repository.findByCode(opened.id()).orElseThrow();
            order.addPart(filter, "Filtro de óleo", money("35.50"), 1, 10);
            order.removePartLine(sparkLine[0]);
            order.completeDiagnosis(T0.plusSeconds(120));
            order.approve(T0.plusSeconds(180));
            repository.update(order);
        });

        WorkOrder reloaded = QuarkusTransaction.requiringNew()
                .call(() -> repository.findByCode(opened.id()).orElseThrow());

        assertThat(reloaded.status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
        assertThat(reloaded.serviceLines()).extracting(ServiceLine::name, ServiceLine::price)
                .containsExactly(tuple("Troca de óleo", money("120.00")));
        assertThat(reloaded.partLines()).extracting(PartLine::partId, PartLine::quantity)
                .containsExactly(tuple(filter, 2));
        assertThat(reloaded.estimate().total()).isEqualTo(money("191.00"));
        assertThat(reloaded.estimate().decision()).isEqualTo(EstimateDecision.APPROVED);
        assertThat(reloaded.statusHistory()).extracting(change -> change.to()).containsExactly(
                WorkOrderStatus.RECEIVED, WorkOrderStatus.IN_DIAGNOSIS, WorkOrderStatus.AWAITING_APPROVAL,
                WorkOrderStatus.IN_PROGRESS);
        assertThat(reloaded.version()).isPositive();
    }

    @Test
    void unknownCodeIsEmpty() {
        assertThat(QuarkusTransaction.requiringNew().call(() -> repository.findByCode(UUID.randomUUID())))
                .isEmpty();
    }

    private static Money money(String value) {
        return Money.of("price", new BigDecimal(value));
    }
}
