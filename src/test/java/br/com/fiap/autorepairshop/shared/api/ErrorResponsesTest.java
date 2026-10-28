package br.com.fiap.autorepairshop.shared.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ErrorResponsesTest {

    @Test
    void unknownRouteReturnsProblemDetails404() {
        given()
                .when().get("/rota-inexistente")
                .then()
                .statusCode(404)
                .contentType(startsWith(Problem.MEDIA_TYPE))
                .body("status", equalTo(404))
                .body("title", equalTo("Recurso não encontrado"))
                .body("type", equalTo("about:blank"))
                .body("instance", equalTo("/rota-inexistente"));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"name\": \"\", \"quantity\": 0}")
                .when().post("/test-support/errors/validation")
                .then()
                .statusCode(400)
                .contentType(startsWith(Problem.MEDIA_TYPE))
                .body("status", equalTo(400))
                .body("errors", hasSize(2))
                .body("errors[0].field", equalTo("name"))
                .body("errors[1].field", equalTo("quantity"));
    }

    @Test
    void malformedJsonReturnsProblemDetails400() {
        given()
                .contentType(ContentType.JSON)
                .body("{nao e json")
                .when().post("/test-support/errors/validation")
                .then()
                .statusCode(400)
                .contentType(startsWith(Problem.MEDIA_TYPE))
                .body("status", equalTo(400));
    }

    @Test
    void unexpectedFailureReturns500WithoutInternalDetails() {
        given()
                .when().get("/test-support/errors/unexpected")
                .then()
                .statusCode(500)
                .contentType(startsWith(Problem.MEDIA_TYPE))
                .body("status", equalTo(500))
                .body("detail", containsString("identificador"))
                .body(not(containsString("IllegalStateException")))
                .body(not(containsString("SELECT")))
                .body(not(containsString("at br.com")));
    }
}
