package br.com.fiap.autorepairshop.customer.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import br.com.fiap.autorepairshop.support.TestDocuments;
import br.com.fiap.autorepairshop.support.TestTokens;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class CustomerResourceTest {

    private static final String PATH = "/api/admin/customers";

    @Test
    void createsCustomerWithCpf() {
        String cpf = TestDocuments.cpf();

        String location = admin()
                .body(customer(formatCpf(cpf)))
                .when().post(PATH)
                .then()
                .statusCode(201)
                .header("Location", containsString(PATH + "/"))
                .body("document", equalTo(cpf))
                .body("documentType", equalTo("CPF"))
                .extract().header("Location");

        admin().when().get(location).then().statusCode(200).body("document", equalTo(cpf));
    }

    @Test
    void createsCustomerWithNumericAndAlphanumericCnpj() {
        admin().body(customer(TestDocuments.cnpj())).when().post(PATH)
                .then().statusCode(201).body("documentType", equalTo("CNPJ"));

        String alphanumeric = TestDocuments.alphanumericCnpj();
        admin().body(customer(alphanumeric.toLowerCase())).when().post(PATH)
                .then().statusCode(201)
                .body("documentType", equalTo("CNPJ"))
                .body("document", equalTo(alphanumeric));
    }

    @Test
    void rejectsInvalidDocument() {
        admin().body(customer("529.982.247-24")).when().post(PATH)
                .then().statusCode(400)
                .body("errors.field", hasItem("document"));
    }

    @Test
    void rejectsMissingOrInvalidEmail() {
        Map<String, Object> body = customer(TestDocuments.cpf());
        body.put("email", "sem-arroba");
        admin().body(body).when().post(PATH).then().statusCode(400).body("errors.field", hasItem("email"));

        body.remove("email");
        admin().body(body).when().post(PATH).then().statusCode(400).body("errors.field", hasItem("email"));
    }

    @Test
    void rejectsDuplicateDocumentButAcceptsAfterDeletion() {
        String cpf = TestDocuments.cpf();
        String id = admin().body(customer(cpf)).when().post(PATH).then().statusCode(201).extract().path("id");

        admin().body(customer(formatCpf(cpf))).when().post(PATH)
                .then().statusCode(409).body("title", equalTo("Conflito com o estado atual"));

        admin().when().delete(PATH + "/" + id).then().statusCode(204);
        admin().body(customer(cpf)).when().post(PATH).then().statusCode(201);
    }

    @Test
    void findsByDocumentWithOrWithoutFormatting() {
        String cpf = TestDocuments.cpf();
        String id = admin().body(customer(cpf)).when().post(PATH).then().statusCode(201).extract().path("id");

        admin().queryParam("document", formatCpf(cpf)).when().get(PATH)
                .then().statusCode(200)
                .body("items", hasSize(1))
                .body("items[0].id", equalTo(id))
                .body("totalItems", equalTo(1));
    }

    @Test
    void unknownOrDeletedCustomerReturns404() {
        admin().when().get(PATH + "/00000000-0000-0000-0000-000000000000").then().statusCode(404);

        String id = admin().body(customer(TestDocuments.cpf())).when().post(PATH).then().extract().path("id");
        admin().when().delete(PATH + "/" + id).then().statusCode(204);
        admin().when().get(PATH + "/" + id).then().statusCode(404);
        admin().when().get(PATH + "?size=100").then().body("items.id", not(hasItem(id)));
    }

    @Test
    void listsWithPaginationAndRejectsOversizedPage() {
        admin().body(customer(TestDocuments.cpf())).when().post(PATH).then().statusCode(201);
        admin().body(customer(TestDocuments.cpf())).when().post(PATH).then().statusCode(201);

        admin().queryParam("page", 0).queryParam("size", 1).when().get(PATH)
                .then().statusCode(200)
                .body("items", hasSize(1))
                .body("page", equalTo(0))
                .body("size", equalTo(1));

        admin().queryParam("size", 101).when().get(PATH).then().statusCode(400);
    }

    @Test
    void updatesDataKeepingDocument() {
        String cpf = TestDocuments.cpf();
        String id = admin().body(customer(cpf)).when().post(PATH).then().extract().path("id");

        admin().body(Map.of("name", "Maria Souza", "email", "souza@email.com", "phone", "11988887777"))
                .when().put(PATH + "/" + id)
                .then().statusCode(200)
                .body("name", equalTo("Maria Souza"))
                .body("email", equalTo("souza@email.com"))
                .body("document", equalTo(cpf));
    }

    @Test
    void requiresAdminToken() {
        given().contentType(ContentType.JSON).body(customer(TestDocuments.cpf()))
                .when().post(PATH).then().statusCode(401);
    }

    @Test
    void phoneIsOptional() {
        Map<String, Object> body = customer(TestDocuments.cpf());
        body.remove("phone");
        admin().body(body).when().post(PATH).then().statusCode(201).body("phone", nullValue());
    }

    private static RequestSpecification admin() {
        return given().auth().oauth2(TestTokens.admin()).contentType(ContentType.JSON);
    }

    private static Map<String, Object> customer(String document) {
        Map<String, Object> body = new HashMap<>();
        body.put("document", document);
        body.put("name", "Maria Silva");
        body.put("email", "maria.silva@email.com");
        body.put("phone", "11999990000");
        return body;
    }

    private static String formatCpf(String cpf) {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
