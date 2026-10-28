package br.com.fiap.autorepairshop.shared.platform;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ApiDocumentationTest {

    @Test
    void openApiSpecificationIsPublished() {
        given()
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body(containsString("openapi: 3"));
    }

    @Test
    void openApiMetadataIsInPortuguese() {
        given()
                .queryParam("format", "json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .contentType(startsWith("application/json"))
                .body("info.title", equalTo("API da Oficina Mecânica"))
                .body("info.description", startsWith("API do sistema integrado de atendimento"));
    }

    @Test
    void declaresBearerJwtSecurityScheme() {
        given()
                .queryParam("format", "json")
                .when().get("/q/openapi")
                .then()
                .statusCode(200)
                .body("components.securitySchemes.bearerAuth.scheme", equalTo("bearer"))
                .body("components.securitySchemes.bearerAuth.bearerFormat", equalTo("JWT"));
    }

    @Test
    void swaggerUiIsServed() {
        given()
                .when().get("/q/swagger-ui/")
                .then()
                .statusCode(200)
                .body(containsString("swagger-ui"));
    }
}
