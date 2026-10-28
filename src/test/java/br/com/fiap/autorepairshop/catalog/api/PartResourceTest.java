package br.com.fiap.autorepairshop.catalog.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;

import br.com.fiap.autorepairshop.support.TestTokens;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

@QuarkusTest
class PartResourceTest {

    private static final String PATH = "/api/admin/parts";

    @Inject
    EntityManager entityManager;

    @Test
    void createsSupplyWithZeroStock() {
        admin().body("{\"name\": \"Óleo 5W30\", \"category\": \"SUPPLY\", \"unitPrice\": 45.90}")
                .when().post(PATH)
                .then().statusCode(201)
                .header("Location", containsString(PATH + "/"))
                .body("stockQuantity", equalTo(0))
                .body("category", equalTo("SUPPLY"))
                .body(containsString("\"unitPrice\":45.90"));
    }

    @Test
    void rejectsInvalidCategory() {
        admin().body("{\"name\": \"Pneu\", \"category\": \"TIRE\", \"unitPrice\": 300}").when().post(PATH)
                .then().statusCode(400);
    }

    @Test
    void stockEntryAddsToBalance() {
        String id = newPart("PART");
        entry(id, 5);

        admin().body(Map.of("quantity", 10)).when().post(PATH + "/" + id + "/stock-entries")
                .then().statusCode(200).body("stockQuantity", equalTo(15));
    }

    @Test
    void stockEntryRequiresPositiveQuantity() {
        String id = newPart("PART");

        admin().body(Map.of("quantity", 0)).when().post(PATH + "/" + id + "/stock-entries")
                .then().statusCode(400).body("errors.field", hasItem("quantity"));
        admin().body(Map.of("quantity", -2)).when().post(PATH + "/" + id + "/stock-entries")
                .then().statusCode(400).body("errors.field", hasItem("quantity"));
    }

    @Test
    void adjustmentSetsCountedQuantityAndMovementsAreListedNewestFirst() {
        String id = newPart("SUPPLY");
        entry(id, 10);

        admin().body(Map.of("countedQuantity", 7)).when().post(PATH + "/" + id + "/stock-adjustments")
                .then().statusCode(200).body("stockQuantity", equalTo(7));

        admin().when().get(PATH + "/" + id + "/movements")
                .then().statusCode(200)
                .body("items", hasSize(2))
                .body("items[0].type", equalTo("ADJUSTMENT"))
                .body("items[0].quantity", equalTo(-3))
                .body("items[1].type", equalTo("INBOUND"))
                .body("items[1].quantity", equalTo(10));
    }

    @Test
    void adjustmentRejectsNegativeCount() {
        String id = newPart("SUPPLY");

        admin().body(Map.of("countedQuantity", -1)).when().post(PATH + "/" + id + "/stock-adjustments")
                .then().statusCode(400);
    }

    @Test
    void updateIgnoresStockSentInBody() {
        String id = newPart("PART");
        entry(id, 10);

        admin().body("{\"name\": \"Filtro de ar\", \"category\": \"PART\", \"unitPrice\": 50, \"stockQuantity\": 99}")
                .when().put(PATH + "/" + id)
                .then().statusCode(200)
                .body("name", equalTo("Filtro de ar"))
                .body("stockQuantity", equalTo(10));
    }

    @Test
    void filtersByCategory() {
        newPart("PART");
        newPart("SUPPLY");

        admin().queryParam("category", "PART").queryParam("size", 100).when().get(PATH)
                .then().statusCode(200).body("items.category", everyItem(equalTo("PART")));
    }

    @Test
    void deletedPartIsNoLongerFound() {
        String id = newPart("PART");

        admin().when().delete(PATH + "/" + id).then().statusCode(204);
        admin().when().get(PATH + "/" + id).then().statusCode(404);
        admin().when().get(PATH + "/" + id + "/movements").then().statusCode(404);
    }

    @Test
    @TestTransaction
    void databaseRejectsNegativeStock() {
        UUID id = UUID.fromString(newPart("PART"));

        assertThatThrownBy(() -> entityManager
                .createNativeQuery("UPDATE part SET stock_quantity = -1 WHERE id = :id")
                .setParameter("id", id)
                .executeUpdate())
                .isInstanceOf(PersistenceException.class);
    }

    private static String newPart(String category) {
        return admin().body(Map.of("name", "Item " + category, "category", category, "unitPrice", 10))
                .when().post(PATH)
                .then().statusCode(201)
                .extract().path("id");
    }

    private static void entry(String id, int quantity) {
        admin().body(Map.of("quantity", quantity)).when().post(PATH + "/" + id + "/stock-entries")
                .then().statusCode(200);
    }

    private static RequestSpecification admin() {
        return given().auth().oauth2(TestTokens.admin()).contentType(ContentType.JSON);
    }
}
