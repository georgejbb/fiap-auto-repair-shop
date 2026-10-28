package br.com.fiap.autorepairshop.catalog.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import br.com.fiap.autorepairshop.support.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ServiceOfferingResourceTest {

    private static final String PATH = "/api/admin/service-offerings";

    @Test
    void createsOfferingWithPriceInTwoDecimals() {
        // O corpo é enviado como texto para que o preço chegue exatamente como 120.00, sem conversão do cliente.
        admin().body("{\"name\": \"Troca de óleo\", \"description\": \"Óleo e filtro\", \"price\": 120}")
                .when().post(PATH)
                .then().statusCode(201)
                .header("Location", containsString(PATH + "/"))
                .body(containsString("\"price\":120.00"));
    }

    @Test
    void rejectsNegativePrice() {
        admin().body(Map.of("name", "Alinhamento", "price", -1)).when().post(PATH)
                .then().statusCode(400).body("errors.field", hasItem("price"));
    }

    @Test
    void rejectsPriceWithThreeDecimalPlaces() {
        admin().body("{\"name\": \"Alinhamento\", \"price\": 80.555}").when().post(PATH)
                .then().statusCode(400).body("errors.field", hasItem("price"));
    }

    @Test
    void updatesPriceAndDeletesLogically() {
        String id = admin().body(Map.of("name", "Balanceamento", "price", 60)).when().post(PATH)
                .then().statusCode(201).extract().path("id");

        admin().body("{\"name\": \"Balanceamento\", \"price\": 75.90}").when().put(PATH + "/" + id)
                .then().statusCode(200).body(containsString("\"price\":75.90"));

        admin().when().get(PATH + "?size=100").then().statusCode(200).body("items.id", hasItem(id));
        admin().when().delete(PATH + "/" + id).then().statusCode(204);
        admin().when().get(PATH + "/" + id).then().statusCode(404).body("detail", equalTo("Serviço não encontrado."));
        admin().when().get(PATH + "?size=100").then().body("items.id", not(hasItem(id)));
    }

    @Test
    void requiresAdminToken() {
        given().when().get(PATH).then().statusCode(401);
    }

    private static RequestSpecification admin() {
        return given().auth().oauth2(TestTokens.admin()).contentType(ContentType.JSON);
    }
}
