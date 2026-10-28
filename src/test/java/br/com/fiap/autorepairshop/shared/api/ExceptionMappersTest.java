package br.com.fiap.autorepairshop.shared.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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

    @ParameterizedTest
    @CsvSource({
            "400, Requisição inválida",
            "404, Recurso não encontrado",
            "405, Método não permitido",
            "415, Tipo de conteúdo não suportado",
            "500, Erro interno",
            "418, Erro HTTP 418"
    })
    void titleMatchesStatus(int status, String title) {
        assertThat(Problem.titleFor(status)).isEqualTo(title);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "400 | O corpo ou os parâmetros da requisição não puderam ser interpretados.",
            "404 | O recurso solicitado não existe.",
            "405 | O método HTTP não é suportado por este recurso.",
            "415 | O tipo de conteúdo enviado não é suportado por este recurso.",
            "409 | A requisição não pôde ser atendida."
    })
    void webApplicationDetailMatchesStatus(int status, String detail) {
        Problem problem = (Problem) new WebApplicationExceptionMapper()
                .toResponse(new WebApplicationException(status))
                .getEntity();

        assertThat(problem.status()).isEqualTo(status);
        assertThat(problem.detail()).isEqualTo(detail);
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
