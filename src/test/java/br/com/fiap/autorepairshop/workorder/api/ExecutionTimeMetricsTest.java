package br.com.fiap.autorepairshop.workorder.api;

import static br.com.fiap.autorepairshop.support.ApiFixtures.admin;
import static br.com.fiap.autorepairshop.support.ApiFixtures.customerWithVehicle;
import static br.com.fiap.autorepairshop.support.ApiFixtures.serviceOffering;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.support.ApiFixtures.CustomerWithVehicle;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;

/**
 * As OS deste teste são concluídas num intervalo de quatro dias sorteado entre 2000 e 2019 e consultadas por
 * esse intervalo, o que as isola das OS criadas pelos demais testes com a data atual e de execuções anteriores
 * que permaneçam no banco de teste.
 */
@QuarkusTest
class ExecutionTimeMetricsTest {

    private static final String PATH = "/api/admin/work-orders/metrics/execution-time";
    private static final Instant START = Instant.parse("2000-01-01T08:00:00Z")
            .plus(Duration.ofDays(ThreadLocalRandom.current().nextLong(7000)));
    private static final Money PRICE = Money.of("price", new BigDecimal("120.00"));

    @Inject
    WorkOrderRepository repository;

    private final Map<UUID, String> serviceNames = new HashMap<>();

    @Test
    void averagesExecutedWorkOrdersOverallAndByService() {
        CustomerWithVehicle owner = customerWithVehicle();
        UUID oilChange = service("Troca de óleo");
        UUID alignment = service("Alinhamento");

        executed(owner, START, 3600, oilChange);
        executed(owner, START.plus(Duration.ofDays(1)), 3 * 3600, oilChange, alignment);
        rejected(owner, START.plus(Duration.ofDays(2)), oilChange);
        inProgress(owner, START.plus(Duration.ofDays(3)), oilChange);

        admin().queryParam("from", START.toString()).queryParam("to", START.plus(Duration.ofDays(4)).toString())
                .when().get(PATH)
                .then().statusCode(200)
                .body("executedCount", equalTo(2))
                .body("averageSeconds", equalTo(7200))
                .body("byService", hasSize(2))
                .body("byService[0].name", equalTo("Alinhamento"))
                .body("byService[0].executedCount", equalTo(1))
                .body("byService[0].averageSeconds", equalTo(10800))
                .body("byService[1].name", equalTo("Troca de óleo"))
                .body("byService[1].executedCount", equalTo(2))
                .body("byService[1].averageSeconds", equalTo(7200));
    }

    @Test
    void periodWithoutCompletionsIsEmpty() {
        admin().queryParam("from", "1990-01-01T00:00:00Z").queryParam("to", "1990-01-31T23:59:59Z")
                .when().get(PATH)
                .then().statusCode(200)
                .body("executedCount", equalTo(0))
                .body("averageSeconds", nullValue())
                .body("byService", hasSize(0));
    }

    @Test
    void withoutPeriodConsidersAllCompletions() {
        admin().when().get(PATH).then().statusCode(200);
    }

    @Test
    void invertedOrMalformedPeriodReturns400() {
        admin().queryParam("from", "2020-03-31T00:00:00Z").queryParam("to", "2020-03-01T00:00:00Z")
                .when().get(PATH).then().statusCode(400).body("errors[0].field", equalTo("from"));
        admin().queryParam("to", "31/03/2020").when().get(PATH)
                .then().statusCode(400).body("errors[0].field", equalTo("to"));
    }

    @Test
    void requiresToken() {
        given().when().get(PATH).then().statusCode(401);
    }

    private void executed(CustomerWithVehicle owner, Instant start, long durationSeconds, UUID... services) {
        WorkOrder workOrder = awaitingApproval(owner, start, services);
        workOrder.approve(start.plusSeconds(180));
        workOrder.finish(start.plusSeconds(180 + durationSeconds));
        save(workOrder);
    }

    private void rejected(CustomerWithVehicle owner, Instant start, UUID service) {
        WorkOrder workOrder = awaitingApproval(owner, start, service);
        workOrder.reject(start.plusSeconds(180));
        save(workOrder);
    }

    private void inProgress(CustomerWithVehicle owner, Instant start, UUID service) {
        WorkOrder workOrder = awaitingApproval(owner, start, service);
        workOrder.approve(start.plusSeconds(180));
        save(workOrder);
    }

    private UUID service(String name) {
        UUID id = UUID.fromString(serviceOffering(name, "100.00"));
        serviceNames.put(id, name);
        return id;
    }

    private WorkOrder awaitingApproval(CustomerWithVehicle owner, Instant start, UUID... services) {
        WorkOrder workOrder = WorkOrder.open(UUID.fromString(owner.customerId()), UUID.fromString(owner.vehicleId()),
                start);
        workOrder.startDiagnosis(start.plusSeconds(60));
        for (UUID service : services) {
            workOrder.addService(service, serviceNames.get(service), PRICE);
        }
        workOrder.completeDiagnosis(start.plusSeconds(120));
        return workOrder;
    }

    private void save(WorkOrder workOrder) {
        QuarkusTransaction.requiringNew().run(() -> repository.add(workOrder));
    }
}
