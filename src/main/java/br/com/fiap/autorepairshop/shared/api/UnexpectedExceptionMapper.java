package br.com.fiap.autorepairshop.shared.api;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.UUID;
import org.jboss.logging.Logger;

/**
 * Última barreira para exceções não tratadas: registra a falha completa no log e devolve ao cliente
 * apenas um identificador para correlação, sem detalhes internos.
 */
@Provider
public class UnexpectedExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(UnexpectedExceptionMapper.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        String errorId = UUID.randomUUID().toString();
        LOG.errorf(exception, "Erro não tratado [errorId=%s]", errorId);
        String detail = "Ocorreu um erro inesperado. Informe o identificador " + errorId + " ao suporte.";
        return Problem.of(500, detail, RequestPaths.of(uriInfo)).toResponse();
    }
}
