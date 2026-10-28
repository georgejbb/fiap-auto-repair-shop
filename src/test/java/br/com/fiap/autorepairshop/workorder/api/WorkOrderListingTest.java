package br.com.fiap.autorepairshop.workorder.api;

import static br.com.fiap.autorepairshop.support.ApiFixtures.admin;
import static br.com.fiap.autorepairshop.support.ApiFixtures.customerWithVehicle;
import static br.com.fiap.autorepairshop.support.ApiFixtures.serviceOffering;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isOneOf;
import static org.hamcrest.Matchers.nullValue;

import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.support.ApiFixtures.CustomerWithVehicle;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WorkOrderListingTest {

    private static final String PATH = "/api/admin/work-orders";
    private static final Instant T0 = Instant.parse("2026-10-20T08:00:00Z");

    @Inject
    WorkOrderRepository repository;

    private CustomerWithVehicle owner;
    private UUID serviceId;
    private UUID receivedFirst;
    private UUID receivedSecond;
    private UUID inProgress;
    private UUID awaitingApproval;

    @BeforeEach
    void workOrdersOfOneCustomer() {
        owner = customerWithVehicle();
        serviceId = UUID.fromString(serviceOffering("Troca de óleo", "120.00"));
        receivedFirst = store(T0, 0);
        receivedSecond = store(T0.plusSeconds(3600), 0);
        inProgress = store(T0.plusSeconds(7200), 3);
        awaitingApproval = store(T0.plusSeconds(10800), 2);
    }

    @Test
    void ordersByStatusPriorityThenOpeningTime() {
        admin().queryParam("customerId", owner.customerId()).when().get(PATH)
                .then().statusCode(200)
                .body("totalItems", equalTo(4))
                .body("items.code", contains(inProgress.toString(), awaitingApproval.toString(),
                        receivedFirst.toString(), receivedSecond.toString()))
                .body("items[0].customerName", equalTo("Maria Silva"))
                .body("items[0].licensePlate", equalTo(owner.licensePlate()))
                .body("items[0].estimateTotal", equalTo(120.0f))
                .body("items[2].estimateTotal", nullValue());
    }

    @Test
    void filtersByMultipleStatuses() {
        admin().queryParam("customerId", owner.customerId())
                .queryParam("status", "RECEIVED").queryParam("status", "IN_PROGRESS")
                .when().get(PATH)
                .then().statusCode(200)
                .body("items", hasSize(3))
                .body("items.status", everyItem(isOneOf("RECEIVED", "IN_PROGRESS")));
    }

    @Test
    void paginates() {
        admin().queryParam("customerId", owner.customerId()).queryParam("size", 2).queryParam("page", 1)
                .when().get(PATH)
                .then().statusCode(200)
                .body("items.code", contains(receivedFirst.toString(), receivedSecond.toString()))
                .body("totalItems", equalTo(4));
    }

    @Test
    void listsAcrossCustomersWithoutFilter() {
        admin().queryParam("size", 100).when().get(PATH)
                .then().statusCode(200)
                .body("items.code", hasItem(inProgress.toString()));
    }

    @Test
    void invalidStatusReturns400() {
        admin().queryParam("status", "CANCELLED").when().get(PATH)
                .then().statusCode(400).body("errors[0].field", equalTo("status"));
    }

    @Test
    void requiresToken() {
        given().when().get(PATH).then().statusCode(401);
    }

    /** Grava uma OS com abertura controlada; steps: 0 recebida, 2 aguardando aprovação, 3 em execução. */
    private UUID store(Instant openedAt, int steps) {
        WorkOrder workOrder = WorkOrder.open(UUID.fromString(owner.customerId()), UUID.fromString(owner.vehicleId()),
                openedAt);
        if (steps >= 2) {
            workOrder.startDiagnosis(openedAt.plusSeconds(60));
            workOrder.addService(serviceId, "Troca de óleo", Money.of("price", new BigDecimal("120.00")));
            workOrder.completeDiagnosis(openedAt.plusSeconds(120));
        }
        if (steps >= 3) {
            workOrder.approve(openedAt.plusSeconds(180));
        }
        QuarkusTransaction.requiringNew().run(() -> repository.add(workOrder));
        return workOrder.id();
    }
}
