package br.com.fiap.autorepairshop.support;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.Map;

/**
 * Cria pela API os cadastros de que as ordens de serviço dependem.
 */
public final class ApiFixtures {

    private ApiFixtures() {
    }

    /** Cliente e veículo dele, com o documento usado na consulta pública. */
    public record CustomerWithVehicle(String customerId, String document, String vehicleId, String licensePlate) {
    }

    public static RequestSpecification admin() {
        return given().auth().oauth2(TestTokens.admin()).contentType(ContentType.JSON);
    }

    public static CustomerWithVehicle customerWithVehicle() {
        String document = TestDocuments.cpf();
        String customerId = admin()
                .body(Map.of("document", document, "name", "Maria Silva", "email", "maria." + document + "@email.com"))
                .when().post("/api/admin/customers")
                .then().statusCode(201).extract().path("id");
        String plate = TestDocuments.mercosulPlate();
        String vehicleId = admin()
                .body(Map.of("licensePlate", plate, "brand", "Fiat", "model", "Argo", "year", 2022,
                        "ownerId", customerId))
                .when().post("/api/admin/vehicles")
                .then().statusCode(201).extract().path("id");
        return new CustomerWithVehicle(customerId, document, vehicleId, plate);
    }

    public static String serviceOffering(String name, String price) {
        return admin().body("{\"name\": \"" + name + "\", \"price\": " + price + "}")
                .when().post("/api/admin/service-offerings")
                .then().statusCode(201).extract().path("id");
    }

    public static String partWithStock(String name, String unitPrice, int stock) {
        String id = admin().body("{\"name\": \"" + name + "\", \"category\": \"PART\", \"unitPrice\": " + unitPrice + "}")
                .when().post("/api/admin/parts")
                .then().statusCode(201).extract().path("id");
        if (stock > 0) {
            admin().body(Map.of("quantity", stock)).when().post("/api/admin/parts/" + id + "/stock-entries")
                    .then().statusCode(200);
        }
        return id;
    }

    public static int stockOf(String partId) {
        return admin().when().get("/api/admin/parts/" + partId).then().statusCode(200).extract().path("stockQuantity");
    }

    public static String openWorkOrder(CustomerWithVehicle owner) {
        return admin().body(Map.of("customerId", owner.customerId(), "vehicleId", owner.vehicleId()))
                .when().post("/api/admin/work-orders")
                .then().statusCode(201).extract().path("code");
    }

    /** Abre a OS, inicia o diagnóstico, inclui os itens e conclui o diagnóstico. */
    public static String awaitingApproval(CustomerWithVehicle owner, String serviceId, String partId, int quantity) {
        String code = openWorkOrder(owner);
        String base = "/api/admin/work-orders/" + code;
        admin().when().post(base + "/start-diagnosis").then().statusCode(200);
        admin().body(Map.of("serviceOfferingId", serviceId)).when().post(base + "/services").then().statusCode(200);
        if (partId != null) {
            admin().body(Map.of("partId", partId, "quantity", quantity)).when().post(base + "/parts")
                    .then().statusCode(200);
        }
        admin().when().post(base + "/complete-diagnosis").then().statusCode(200);
        return code;
    }
}
