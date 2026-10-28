package br.com.fiap.autorepairshop.workorder.api;

import static br.com.fiap.autorepairshop.support.ApiFixtures.admin;
import static br.com.fiap.autorepairshop.support.ApiFixtures.awaitingApproval;
import static br.com.fiap.autorepairshop.support.ApiFixtures.customerWithVehicle;
import static br.com.fiap.autorepairshop.support.ApiFixtures.partWithStock;
import static br.com.fiap.autorepairshop.support.ApiFixtures.serviceOffering;
import static br.com.fiap.autorepairshop.support.ApiFixtures.stockOf;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import br.com.fiap.autorepairshop.support.ApiFixtures.CustomerWithVehicle;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CustomerWorkOrderResourceTest {

    private static final String PUBLIC = "/api/public/work-orders/";
    private static final String ADMIN = "/api/admin/work-orders/";

    @Test
    void customerConsultsWithFormattedDocument() {
        CustomerWithVehicle owner = customerWithVehicle();
        String code = awaitingApproval(owner, serviceOffering("Troca de óleo", "120.00"),
                partWithStock("Filtro de óleo", "35.50", 5), 2);

        customer(formatCpf(owner.document())).when().get(PUBLIC + code)
                .then().statusCode(200)
                .body("status", equalTo("AWAITING_APPROVAL"))
                .body("licensePlate", equalTo(owner.licensePlate()))
                .body("items", hasSize(2))
                .body(containsString("\"total\":191.00"))
                .body("statusHistory", hasSize(3))
                .body(not(containsString(owner.customerId())));
    }

    @Test
    void approvalMovesToInProgressAndConsumesStock() {
        CustomerWithVehicle owner = customerWithVehicle();
        String filter = partWithStock("Filtro de óleo", "35.50", 5);
        String code = awaitingApproval(owner, serviceOffering("Troca de óleo", "120.00"), filter, 2);

        customer(owner.document()).when().post(PUBLIC + code + "/approve")
                .then().statusCode(200)
                .body("status", equalTo("IN_PROGRESS"))
                .body("estimate.decision", equalTo("APPROVED"));

        assertThat(stockOf(filter)).isEqualTo(3);
        admin().when().get("/api/admin/parts/" + filter + "/movements")
                .then().statusCode(200)
                .body("items[0].type", equalTo("CONSUMPTION"))
                .body("items[0].quantity", equalTo(-2))
                .body("items[0].workOrderId", equalTo(code));
    }

    @Test
    void stockReducedBeforeApprovalRejectsAtomically() {
        CustomerWithVehicle owner = customerWithVehicle();
        String filter = partWithStock("Filtro de óleo", "35.50", 5);
        String spark = partWithStock("Vela", "20.00", 4);
        String code = openInDiagnosis(owner);
        admin().body(Map.of("serviceOfferingId", serviceOffering("Revisão", "200.00")))
                .when().post(ADMIN + code + "/services").then().statusCode(200);
        admin().body(Map.of("partId", filter, "quantity", 2)).when().post(ADMIN + code + "/parts").then().statusCode(200);
        admin().body(Map.of("partId", spark, "quantity", 4)).when().post(ADMIN + code + "/parts").then().statusCode(200);
        admin().when().post(ADMIN + code + "/complete-diagnosis").then().statusCode(200);

        admin().body(Map.of("countedQuantity", 3)).when().post("/api/admin/parts/" + spark + "/stock-adjustments")
                .then().statusCode(200);

        customer(owner.document()).when().post(PUBLIC + code + "/approve")
                .then().statusCode(422).body("detail", containsString("Estoque insuficiente de Vela"));

        customer(owner.document()).when().get(PUBLIC + code).then().body("status", equalTo("AWAITING_APPROVAL"));
        assertThat(stockOf(filter)).isEqualTo(5);
        assertThat(stockOf(spark)).isEqualTo(3);
        admin().when().get("/api/admin/parts/" + filter + "/movements")
                .then().body("items.type", not(hasItem("CONSUMPTION")));
    }

    @Test
    void deletedPartBeforeApprovalIsRejected() {
        CustomerWithVehicle owner = customerWithVehicle();
        String filter = partWithStock("Filtro de óleo", "35.50", 5);
        String code = awaitingApproval(owner, serviceOffering("Troca de óleo", "120.00"), filter, 1);
        admin().when().delete("/api/admin/parts/" + filter).then().statusCode(204);

        customer(owner.document()).when().post(PUBLIC + code + "/approve").then().statusCode(422);
        customer(owner.document()).when().get(PUBLIC + code).then().body("status", equalTo("AWAITING_APPROVAL"));
    }

    @Test
    void rejectionCompletesWithoutConsumingStockAndCanBeDelivered() {
        CustomerWithVehicle owner = customerWithVehicle();
        String filter = partWithStock("Filtro de óleo", "35.50", 5);
        String code = awaitingApproval(owner, serviceOffering("Troca de óleo", "120.00"), filter, 2);

        customer(owner.document()).when().post(PUBLIC + code + "/reject")
                .then().statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("estimate.decision", equalTo("REJECTED"));

        assertThat(stockOf(filter)).isEqualTo(5);
        admin().when().post(ADMIN + code + "/deliver").then().statusCode(200).body("status", equalTo("DELIVERED"));
    }

    @Test
    void approvalOutsideAwaitingApprovalIsRejected() {
        CustomerWithVehicle owner = customerWithVehicle();
        String code = awaitingApproval(owner, serviceOffering("Troca de óleo", "120.00"), null, 0);
        customer(owner.document()).when().post(PUBLIC + code + "/approve").then().statusCode(200);

        customer(owner.document()).when().post(PUBLIC + code + "/approve").then().statusCode(422);
        customer(owner.document()).when().post(PUBLIC + code + "/reject").then().statusCode(422);
    }

    @Test
    void fullCycleUntilDelivery() {
        CustomerWithVehicle owner = customerWithVehicle();
        String code = awaitingApproval(owner, serviceOffering("Alinhamento", "80.00"),
                partWithStock("Pastilha", "90.00", 2), 2);
        customer(owner.document()).when().post(PUBLIC + code + "/approve").then().statusCode(200);

        admin().when().post(ADMIN + code + "/finish").then().statusCode(200).body("status", equalTo("COMPLETED"));
        admin().when().post(ADMIN + code + "/deliver")
                .then().statusCode(200)
                .body("status", equalTo("DELIVERED"))
                .body("statusHistory.to", equalTo(List.of("RECEIVED", "IN_DIAGNOSIS", "AWAITING_APPROVAL",
                        "IN_PROGRESS", "COMPLETED", "DELIVERED")));
    }

    @Test
    void wrongDocumentOrUnknownCodeGiveSame404() {
        CustomerWithVehicle owner = customerWithVehicle();
        CustomerWithVehicle other = customerWithVehicle();
        String code = awaitingApproval(owner, serviceOffering("Troca de óleo", "120.00"), null, 0);

        String wrongDocument = customer(other.document()).when().get(PUBLIC + code)
                .then().statusCode(404).extract().path("detail");
        String unknownCode = customer(owner.document()).when().get(PUBLIC + "00000000-0000-0000-0000-000000000000")
                .then().statusCode(404).extract().path("detail");

        assertThat(wrongDocument).isEqualTo(unknownCode);
        customer(other.document()).when().post(PUBLIC + code + "/approve").then().statusCode(404);
    }

    @Test
    void missingDocumentHeaderReturns400() {
        String code = awaitingApproval(customerWithVehicle(), serviceOffering("Troca de óleo", "120.00"), null, 0);

        given().when().get(PUBLIC + code).then().statusCode(400);
        given().when().post(PUBLIC + code + "/approve").then().statusCode(400);
    }

    private static String openInDiagnosis(CustomerWithVehicle owner) {
        String code = admin().body(Map.of("customerId", owner.customerId(), "vehicleId", owner.vehicleId()))
                .when().post("/api/admin/work-orders").then().statusCode(201).extract().path("code");
        admin().when().post(ADMIN + code + "/start-diagnosis").then().statusCode(200);
        return code;
    }

    private static RequestSpecification customer(String document) {
        return given().header("X-Customer-Document", document);
    }

    private static String formatCpf(String cpf) {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
