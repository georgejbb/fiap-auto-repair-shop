package br.com.fiap.autorepairshop.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ExceptionMappersTest {

    @Test
    void webApplicationExceptionKeepsStatusAndUsesProblemMediaType() {
        Response response = new WebApplicationExceptionMapper().toResponse(new NotAllowedException("GET"));

        assertThat(response.getStatus()).isEqualTo(405);
        assertThat(response.getMediaType()).hasToString(Problem.MEDIA_TYPE);
        Problem problem = (Problem) response.getEntity();
        assertThat(problem.title()).isEqualTo("Método não permitido");
        assertThat(problem.instance()).isNull();
        assertThat(problem.errors()).isNull();
    }

    @Test
    void notFoundHasPortugueseDetail() {
        Problem problem = (Problem) new WebApplicationExceptionMapper().toResponse(new NotFoundException()).getEntity();

        assertThat(problem.status()).isEqualTo(404);
        assertThat(problem.detail()).isEqualTo("O recurso solicitado não existe.");
    }

    @Test
    void constraintViolationsBecomeSortedFieldErrors() {
        ConstraintViolation<?> quantity = violation("create", "request", "quantity", "deve ser maior que 0");
        ConstraintViolation<?> name = violation("create", "request", "name", "não deve estar em branco");

        Response response = new ConstraintViolationExceptionMapper()
                .toResponse(new ConstraintViolationException(Set.of(quantity, name)));

        assertThat(response.getStatus()).isEqualTo(400);
        Problem problem = (Problem) response.getEntity();
        assertThat(problem.errors()).containsExactly(
                new Problem.FieldError("name", "não deve estar em branco"),
                new Problem.FieldError("quantity", "deve ser maior que 0"));
    }

    @Test
    void unexpectedExceptionHidesMessageAndClass() {
        Response response = new UnexpectedExceptionMapper()
                .toResponse(new IllegalStateException("senha=123 em SELECT"));

        assertThat(response.getStatus()).isEqualTo(500);
        Problem problem = (Problem) response.getEntity();
        assertThat(problem.title()).isEqualTo("Erro interno");
        assertThat(problem.detail()).doesNotContain("senha", "SELECT", "IllegalStateException");
    }

    @Test
    void unknownStatusGetsGenericTitle() {
        assertThat(Problem.titleFor(418)).isEqualTo("Erro HTTP 418");
    }

    private static ConstraintViolation<?> violation(String method, String parameter, String field, String message) {
        List<Path.Node> nodes = List.of(node(method), node(parameter), node(field));
        Path path = nodes::iterator;
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn(message);
        return violation;
    }

    private static Path.Node node(String name) {
        Path.Node node = mock(Path.Node.class);
        when(node.getName()).thenReturn(name);
        return node;
    }
}
