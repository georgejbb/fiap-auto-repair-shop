package br.com.fiap.autorepairshop.customer.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;

import br.com.fiap.autorepairshop.support.TestDocuments;
import br.com.fiap.autorepairshop.support.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.time.Year;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class VehicleResourceTest {

    private static final String PATH = "/api/admin/vehicles";

    @Test
    void createsVehicleWithOldPlateNormalized() {
        String owner = newCustomer();
        String plate = TestDocuments.oldPlate();
        String formatted = plate.substring(0, 3).toLowerCase() + "-" + plate.substring(3);

        admin().body(vehicle(formatted, owner, 2022)).when().post(PATH)
                .then().statusCode(201)
                .header("Location", containsString(PATH + "/"))
                .body("licensePlate", equalTo(plate))
                .body("ownerId", equalTo(owner));
    }

    @Test
    void createsVehicleWithMercosulPlate() {
        admin().body(vehicle(TestDocuments.mercosulPlate(), newCustomer(), 2024)).when().post(PATH)
                .then().statusCode(201);
    }

    @Test
    void rejectsInvalidPlate() {
        admin().body(vehicle("AB-12345", newCustomer(), 2022)).when().post(PATH)
                .then().statusCode(400).body("errors.field", hasItem("licensePlate"));
    }

    @Test
    void rejectsYearTwoYearsAhead() {
        int year = Year.now().getValue() + 2;
        admin().body(vehicle(TestDocuments.mercosulPlate(), newCustomer(), year)).when().post(PATH)
                .then().statusCode(400).body("errors.field", hasItem("year"));
    }

    @Test
    void rejectsUnknownOwner() {
        admin().body(vehicle(TestDocuments.mercosulPlate(), "00000000-0000-0000-0000-000000000000", 2022))
                .when().post(PATH)
                .then().statusCode(422).body("title", equalTo("Regra de negócio violada"));
    }

    @Test
    void rejectsDuplicatePlateInAnyFormatting() {
        String owner = newCustomer();
        String plate = TestDocuments.oldPlate();
        admin().body(vehicle(plate, owner, 2022)).when().post(PATH).then().statusCode(201);

        String formatted = plate.substring(0, 3).toLowerCase() + "-" + plate.substring(3);
        admin().body(vehicle(formatted, owner, 2022)).when().post(PATH).then().statusCode(409);
    }

    @Test
    void searchesByPlateAndByOwner() {
        String owner = newCustomer();
        String plate = TestDocuments.mercosulPlate();
        String id = admin().body(vehicle(plate, owner, 2022)).when().post(PATH).then().extract().path("id");
        admin().body(vehicle(TestDocuments.mercosulPlate(), owner, 2023)).when().post(PATH).then().statusCode(201);
        admin().body(vehicle(TestDocuments.mercosulPlate(), newCustomer(), 2023)).when().post(PATH)
                .then().statusCode(201);

        admin().queryParam("licensePlate", plate).when().get(PATH)
                .then().statusCode(200).body("items", hasSize(1)).body("items[0].id", equalTo(id));
        admin().queryParam("ownerId", owner).when().get(PATH)
                .then().statusCode(200).body("items", hasSize(2)).body("totalItems", equalTo(2));
        admin().when().get(PATH).then().statusCode(200).body("items.size()", greaterThan(0));
    }

    @Test
    void updatesOwnerKeepingPlate() {
        String plate = TestDocuments.mercosulPlate();
        String id = admin().body(vehicle(plate, newCustomer(), 2022)).when().post(PATH).then().extract().path("id");
        String newOwner = newCustomer();

        admin().body(Map.of("brand", "Fiat", "model", "Cronos", "year", 2023, "ownerId", newOwner))
                .when().put(PATH + "/" + id)
                .then().statusCode(200)
                .body("ownerId", equalTo(newOwner))
                .body("model", equalTo("Cronos"))
                .body("licensePlate", equalTo(plate));
    }

    @Test
    void deletedVehicleIsNoLongerFound() {
        String owner = newCustomer();
        String id = admin().body(vehicle(TestDocuments.mercosulPlate(), owner, 2022)).when().post(PATH)
                .then().extract().path("id");

        admin().when().delete(PATH + "/" + id).then().statusCode(204);
        admin().when().get(PATH + "/" + id).then().statusCode(404);
        admin().queryParam("ownerId", owner).when().get(PATH).then().body("items.id", not(hasItem(id)));
    }

    private static String newCustomer() {
        return admin()
                .body(Map.of("document", TestDocuments.cpf(), "name", "Cliente", "email", "cliente@email.com"))
                .when().post("/api/admin/customers")
                .then().statusCode(201)
                .extract().path("id");
    }

    private static RequestSpecification admin() {
        return given().auth().oauth2(TestTokens.admin()).contentType(ContentType.JSON);
    }

    private static Map<String, Object> vehicle(String plate, String ownerId, int year) {
        return Map.of("licensePlate", plate, "brand", "Fiat", "model", "Argo", "year", year, "ownerId", ownerId);
    }
}
