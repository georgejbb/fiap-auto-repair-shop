package br.com.fiap.autorepairshop.identity.api;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import br.com.fiap.autorepairshop.shared.api.Problem;
import br.com.fiap.autorepairshop.support.TestTokens;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthenticationTest {

    private static final String ADMIN_ROUTE = "/api/admin/test-support/ping";

    @Inject
    EntityManager entityManager;

    @Test
    void loginWithValidCredentialsReturnsBearerTokenAccepted() {
        String token = login("admin", "admin123");

        String payload = payload(token);
        assertThat(payload)
                .contains("\"iss\":\"fiap-auto-repair-shop\"")
                .contains("\"upn\":\"admin\"")
                .contains("\"groups\":[\"ADMIN\"]");
        given().auth().oauth2(token).when().get(ADMIN_ROUTE).then().statusCode(200);
    }

    @Test
    void tokenExpiresOneHourAfterIssue() throws Exception {
        JsonNode claims = new ObjectMapper().readTree(payload(login("admin", "admin123")));

        assertThat(claims.get("exp").asLong() - claims.get("iat").asLong()).isEqualTo(3600);
    }

    @Test
    void wrongPasswordAndUnknownUserReturnSame401() {
        String wrongPassword = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "admin", "password", "errada"))
                .when().post("/api/auth/login")
                .then().statusCode(401).contentType(startsWith(Problem.MEDIA_TYPE))
                .extract().path("detail");
        String unknownUser = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "ninguem", "password", "errada"))
                .when().post("/api/auth/login")
                .then().statusCode(401)
                .extract().path("detail");

        assertThat(wrongPassword).isEqualTo(unknownUser);
    }

    @Test
    void blankCredentialsReturn400() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "", "password", ""))
                .when().post("/api/auth/login")
                .then().statusCode(400);
    }

    @Test
    void adminRouteWithoutTokenReturns401ProblemWithChallenge() {
        given()
                .when().get(ADMIN_ROUTE)
                .then()
                .statusCode(401)
                .contentType(startsWith(Problem.MEDIA_TYPE))
                .header("WWW-Authenticate", "Bearer")
                .body("title", equalTo("Não autenticado"));
    }

    @Test
    void tokenWithoutAdminRoleReturns403Problem() {
        given().auth().oauth2(TestTokens.withoutAdminRole())
                .when().get(ADMIN_ROUTE)
                .then()
                .statusCode(403)
                .contentType(startsWith(Problem.MEDIA_TYPE))
                .body("title", equalTo("Acesso negado"));
    }

    @Test
    void expiredTokenReturns401() {
        given().auth().oauth2(TestTokens.expired()).when().get(ADMIN_ROUTE).then().statusCode(401);
    }

    @Test
    void tokenSignedWithUnknownKeyReturns401() {
        given().auth().oauth2(TestTokens.signedWithUnknownKey()).when().get(ADMIN_ROUTE).then().statusCode(401);
    }

    @Test
    void passwordIsStoredOnlyAsBcryptHash() {
        String hash = (String) entityManager
                .createNativeQuery("SELECT password_hash FROM admin_user WHERE username = 'admin'")
                .getSingleResult();

        assertThat(hash).startsWith("$2").doesNotContain("admin123");
    }

    private static String login(String username, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .when().post("/api/auth/login")
                .then()
                .statusCode(200)
                .body("tokenType", equalTo("Bearer"))
                .body("expiresIn", greaterThan(0))
                .body("accessToken", notNullValue())
                .extract().path("accessToken");
    }

    private static String payload(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
    }
}
