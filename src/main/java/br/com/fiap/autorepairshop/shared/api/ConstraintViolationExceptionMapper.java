package br.com.fiap.autorepairshop.shared.api;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Comparator;
import java.util.List;

/**
 * Converte falhas de Bean Validation em resposta 400 com uma entrada por campo violado.
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<Problem.FieldError> errors = exception.getConstraintViolations().stream()
                .map(violation -> new Problem.FieldError(fieldName(violation), violation.getMessage()))
                .sorted(Comparator.comparing(Problem.FieldError::field))
                .toList();
        return Problem.validation("Um ou mais campos da requisição são inválidos.", RequestPaths.of(uriInfo), errors)
                .toResponse();
    }

    /**
     * O caminho da violação inclui o método e o parâmetro (por exemplo {@code create.request.name});
     * para o cliente da API interessa apenas o último nó, que é o campo do corpo ou o parâmetro.
     */
    static String fieldName(ConstraintViolation<?> violation) {
        String field = null;
        for (Path.Node node : violation.getPropertyPath()) {
            field = node.getName();
        }
        return field;
    }
}
