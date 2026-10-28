package br.com.fiap.autorepairshop.shared.api;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.fiap.autorepairshop.shared.domain.BusinessRuleException;
import br.com.fiap.autorepairshop.shared.domain.DomainException;
import br.com.fiap.autorepairshop.shared.domain.DuplicateResourceException;
import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;

class DomainAndPersistenceMappersTest {

    private final DomainExceptionMapper domainMapper = new DomainExceptionMapper();
    private final PersistenceExceptionMapper persistenceMapper = new PersistenceExceptionMapper();
    private final SecurityExceptionMappers securityMappers = new SecurityExceptionMappers();

    @Test
    void invalidValueBecomes400WithField() {
        Response response = domainMapper.toResponse(new InvalidValueException("document", "CPF ou CNPJ inválido"));

        Problem problem = problem(response, 400);
        assertThat(problem.errors()).containsExactly(new Problem.FieldError("document", "CPF ou CNPJ inválido"));
    }

    @Test
    void notFoundBecomes404() {
        assertThat(problem(domainMapper.toResponse(new ResourceNotFoundException("Cliente não encontrado.")), 404)
                .detail()).isEqualTo("Cliente não encontrado.");
    }

    @Test
    void duplicateBecomes409() {
        assertThat(problem(domainMapper.toResponse(new DuplicateResourceException("Documento já cadastrado.")), 409)
                .title()).isEqualTo("Conflito com o estado atual");
    }

    @Test
    void businessRuleBecomes422() {
        assertThat(problem(domainMapper.toResponse(new BusinessRuleException("Proprietário inativo.")), 422)
                .title()).isEqualTo("Regra de negócio violada");
    }

    @Test
    void unknownDomainExceptionBecomes500() {
        DomainException unknown = new DomainException("regra sem mapeamento") {
        };
        assertThat(problem(domainMapper.toResponse(unknown), 500).detail()).doesNotContain("regra sem mapeamento");
    }

    @Test
    void optimisticLockBecomes409() {
        problem(persistenceMapper.toResponse(new OptimisticLockException()), 409);
    }

    @Test
    void nestedConstraintViolationBecomes409() {
        ConstraintViolationException violation = new ConstraintViolationException(
                "duplicate key", new SQLException("duplicate key"), "ux_customer_document_active");
        problem(persistenceMapper.toResponse(new PersistenceException("flush", violation)), 409);
    }

    @Test
    void otherPersistenceFailureBecomes500() {
        problem(persistenceMapper.toResponse(new PersistenceException("connection lost")), 500);
    }

    @Test
    void unauthorizedAndAuthenticationFailedBecome401WithChallenge() {
        Response unauthorized = securityMappers.unauthorized(new UnauthorizedException(), null);
        Response failed = securityMappers.authenticationFailed(new AuthenticationFailedException(), null);

        assertThat(problem(unauthorized, 401).title()).isEqualTo("Não autenticado");
        assertThat(unauthorized.getHeaderString(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(problem(failed, 401).status()).isEqualTo(401);
        assertThat(failed.getHeaderString(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
    }

    @Test
    void forbiddenBecomes403() {
        assertThat(problem(securityMappers.forbidden(new ForbiddenException(), null), 403).title())
                .isEqualTo("Acesso negado");
    }

    private static Problem problem(Response response, int expectedStatus) {
        assertThat(response.getStatus()).isEqualTo(expectedStatus);
        assertThat(response.getMediaType()).hasToString(Problem.MEDIA_TYPE);
        return (Problem) response.getEntity();
    }
}
