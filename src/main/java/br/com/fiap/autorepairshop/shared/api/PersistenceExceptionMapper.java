package br.com.fiap.autorepairshop.shared.api;

import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.PersistenceException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.hibernate.exception.ConstraintViolationException;

/**
 * Converte em 409 os conflitos detectados pelo banco: escrita concorrente no mesmo registro e violação de
 * restrição única que escapou da verificação prévia por corrida. Demais falhas de persistência viram 500.
 */
@Provider
public class PersistenceExceptionMapper implements ExceptionMapper<PersistenceException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(PersistenceException exception) {
        String instance = RequestPaths.of(uriInfo);
        if (exception instanceof OptimisticLockException) {
            return Problem.of(409, "O registro foi alterado por outra operação. Consulte-o novamente e repita a operação.",
                    instance).toResponse();
        }
        if (hasConstraintViolation(exception)) {
            return Problem.of(409, "A operação viola uma restrição de unicidade ou integridade dos dados.", instance)
                    .toResponse();
        }
        return UnexpectedExceptionMapper.internalError(exception, instance);
    }

    private static boolean hasConstraintViolation(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException) {
                return true;
            }
        }
        return false;
    }
}
