package br.com.fiap.autorepairshop.shared.api;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Converte erros HTTP lançados pelo framework (rota inexistente, método não permitido, corpo ilegível)
 * em Problem Details, preservando o status original.
 */
@Provider
public class WebApplicationExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(WebApplicationException exception) {
        int status = exception.getResponse().getStatus();
        return Problem.of(status, detailFor(status), RequestPaths.of(uriInfo)).toResponse();
    }

    private static String detailFor(int status) {
        return switch (status) {
            case 400 -> "O corpo ou os parâmetros da requisição não puderam ser interpretados.";
            case 404 -> "O recurso solicitado não existe.";
            case 405 -> "O método HTTP não é suportado por este recurso.";
            case 415 -> "O tipo de conteúdo enviado não é suportado por este recurso.";
            default -> "A requisição não pôde ser atendida.";
        };
    }
}
