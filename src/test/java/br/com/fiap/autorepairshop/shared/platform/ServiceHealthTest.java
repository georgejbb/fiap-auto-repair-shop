package br.com.fiap.autorepairshop.shared.platform;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class ServiceHealthTest {

    @Test
    void livenessIsUp() {
        given()
                .when().get("/q/health/live")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"));
    }

    @Test
    void readinessIncludesDatabaseCheck() {
        given()
                .when().get("/q/health/ready")
                .then()
                .statusCode(200)
                .body("status", equalTo("UP"))
                .body("checks.name", hasItem("Database connections health check"))
                .body("checks.find { it.name == 'Database connections health check' }.status", equalTo("UP"));
    }
}
