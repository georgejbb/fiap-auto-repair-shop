package br.com.fiap.autorepairshop.workorder.api;

import static br.com.fiap.autorepairshop.support.ApiFixtures.admin;
import static br.com.fiap.autorepairshop.support.ApiFixtures.customerWithVehicle;
import static br.com.fiap.autorepairshop.support.ApiFixtures.openWorkOrder;
import static br.com.fiap.autorepairshop.support.ApiFixtures.partWithStock;
import static br.com.fiap.autorepairshop.support.ApiFixtures.serviceOffering;
import static br.com.fiap.autorepairshop.support.ApiFixtures.stockOf;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import br.com.fiap.autorepairshop.support.ApiFixtures.CustomerWithVehicle;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.MockMailbox;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class WorkOrderResourceTest {

    private static final String PATH = "/api/admin/work-orders";

    @Inject
    MockMailbox mailbox;

    @BeforeEach
    void clearMailbox() {
        mailbox.clear();
    }

    @Test
    void opensReceivedWorkOrderWithHistory() {
        CustomerWithVehicle owner = customerWithVehicle();

        admin().body(Map.of("customerId", owner.customerId(), "vehicleId", owner.vehicleId()))
                .when().post(PATH)
                .then().statusCode(201)
                .header("Location", containsString(PATH + "/"))
                .body("status", equalTo("RECEIVED"))
                .body("customer.id", equalTo(owner.customerId()))
                .body("vehicle.licensePlate", equalTo(owner.licensePlate()))
                .body("estimate", nullValue())
                .body("statusHistory", hasSize(1))
                .body("statusHistory[0].to", equalTo("RECEIVED"));
    }

    @Test
    void rejectsVehicleOfAnotherCustomerOrUnknownReferences() {
        CustomerWithVehicle owner = customerWithVehicle();
        CustomerWithVehicle other = customerWithVehicle();

        admin().body(Map.of("customerId", owner.customerId(), "vehicleId", other.vehicleId()))
                .when().post(PATH).then().statusCode(422);
        admin().body(Map.of("customerId", "00000000-0000-0000-0000-000000000000", "vehicleId", owner.vehicleId()))
                .when().post(PATH).then().statusCode(422);
    }

    @Test
    void diagnosisWithItemsGeneratesEstimateAndSendsEmail() {
        CustomerWithVehicle owner = customerWithVehicle();
        String oilChange = serviceOffering("Troca de óleo", "120.00");
        String filter = partWithStock("Filtro de óleo", "35.50", 5);
        String code = openWorkOrder(owner);
        String base = PATH + "/" + code;

        admin().when().post(base + "/start-diagnosis").then().statusCode(200).body("status", equalTo("IN_DIAGNOSIS"));
        admin().body(Map.of("serviceOfferingId", oilChange)).when().post(base + "/services")
                .then().statusCode(200).body("services", hasSize(1));
        admin().body(Map.of("partId", filter, "quantity", 2)).when().post(base + "/parts")
                .then().statusCode(200).body("parts[0].quantity", equalTo(2));
        assertThat(stockOf(filter)).isEqualTo(5);

        admin().when().post(base + "/complete-diagnosis")
                .then().statusCode(200)
                .body("status", equalTo("AWAITING_APPROVAL"))
                .body(containsString("\"total\":191.00"))
                .body("estimate.decision", nullValue());

        List<Mail> sent = mailbox.getMailsSentTo("maria." + owner.document() + "@email.com");
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().getText()).contains(code).contains("191,00").contains(owner.licensePlate());
    }

    @Test
    void serviceKeepsPriceFromInclusion() {
        String oilChange = serviceOffering("Troca de óleo", "120.00");
        String code = openWorkOrder(customerWithVehicle());
        admin().when().post(PATH + "/" + code + "/start-diagnosis").then().statusCode(200);
        admin().body(Map.of("serviceOfferingId", oilChange)).when().post(PATH + "/" + code + "/services")
                .then().statusCode(200);

        admin().body("{\"name\": \"Troca de óleo\", \"price\": 150.00}")
                .when().put("/api/admin/service-offerings/" + oilChange).then().statusCode(200);

        admin().when().get(PATH + "/" + code).then().statusCode(200).body(containsString("\"price\":120.00"));
    }

    @Test
    void itemRulesDuringDiagnosis() {
        String oilChange = serviceOffering("Troca de óleo", "120.00");
        String filter = partWithStock("Filtro de óleo", "35.50", 3);
        String code = openWorkOrder(customerWithVehicle());
        String base = PATH + "/" + code;

        admin().body(Map.of("serviceOfferingId", oilChange)).when().post(base + "/services").then().statusCode(422);
        admin().when().post(base + "/start-diagnosis").then().statusCode(200);

        admin().body(Map.of("serviceOfferingId", oilChange)).when().post(base + "/services").then().statusCode(200);
        admin().body(Map.of("serviceOfferingId", oilChange)).when().post(base + "/services").then().statusCode(422);

        admin().body(Map.of("partId", filter, "quantity", 2)).when().post(base + "/parts").then().statusCode(200);
        String lineId = admin().body(Map.of("partId", filter, "quantity", 1)).when().post(base + "/parts")
                .then().statusCode(200)
                .body("parts", hasSize(1))
                .body("parts[0].quantity", equalTo(3))
                .extract().path("parts[0].lineId");
        admin().body(Map.of("partId", filter, "quantity", 1)).when().post(base + "/parts").then().statusCode(422);

        admin().when().delete(base + "/parts/" + lineId).then().statusCode(200).body("parts", hasSize(0));
        String serviceLine = admin().when().get(base).then().extract().path("services[0].lineId");
        admin().when().delete(base + "/services/" + serviceLine).then().statusCode(200).body("services", hasSize(0));
    }

    @Test
    void diagnosisWithoutItemsIsRejectedAndSendsNoEmail() {
        String code = openWorkOrder(customerWithVehicle());
        admin().when().post(PATH + "/" + code + "/start-diagnosis").then().statusCode(200);

        admin().when().post(PATH + "/" + code + "/complete-diagnosis")
                .then().statusCode(422).body("title", equalTo("Regra de negócio violada"));

        admin().when().get(PATH + "/" + code).then().body("status", equalTo("IN_DIAGNOSIS"));
        assertThat(mailbox.getTotalMessagesSent()).isZero();
    }

    @Test
    void transitionOutOfOrderIsRejected() {
        String code = openWorkOrder(customerWithVehicle());
        admin().when().post(PATH + "/" + code + "/start-diagnosis").then().statusCode(200);

        admin().when().post(PATH + "/" + code + "/finish").then().statusCode(422);
        admin().when().post(PATH + "/" + code + "/deliver").then().statusCode(422);
        admin().when().get(PATH + "/" + code).then().body("status", equalTo("IN_DIAGNOSIS"));
    }

    @Test
    void unknownWorkOrderReturns404AndRoutesRequireToken() {
        admin().when().get(PATH + "/00000000-0000-0000-0000-000000000000").then().statusCode(404);
        given().when().get(PATH + "/00000000-0000-0000-0000-000000000000").then().statusCode(401);
    }
}
