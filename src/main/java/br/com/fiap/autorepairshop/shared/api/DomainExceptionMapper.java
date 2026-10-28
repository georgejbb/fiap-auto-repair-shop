package br.com.fiap.autorepairshop.shared.api;

import br.com.fiap.autorepairshop.shared.domain.BusinessRuleException;
import br.com.fiap.autorepairshop.shared.domain.DomainException;
import br.com.fiap.autorepairshop.shared.domain.DuplicateResourceException;
import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;

/**
 * Converte as exceções de domínio no status HTTP correspondente.
 */
@Provider
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(DomainException exception) {
        String instance = RequestPaths.of(uriInfo);
        return switch (exception) {
            case InvalidValueException invalid -> Problem.validation(
                    "Um ou mais campos da requisição são inválidos.", instance,
                    List.of(new Problem.FieldError(invalid.field(), invalid.getMessage()))).toResponse();
            case ResourceNotFoundException notFound -> Problem.of(404, notFound.getMessage(), instance).toResponse();
            case DuplicateResourceException duplicate -> Problem.of(409, duplicate.getMessage(), instance).toResponse();
            case BusinessRuleException rule -> Problem.of(422, rule.getMessage(), instance).toResponse();
            default -> UnexpectedExceptionMapper.internalError(exception, instance);
        };
    }
}
