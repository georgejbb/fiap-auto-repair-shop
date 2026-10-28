package br.com.fiap.autorepairshop.shared.api;

import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.ForbiddenException;
import io.quarkus.security.UnauthorizedException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/**
 * Respostas Problem Details para falhas de autenticação e autorização das rotas administrativas.
 */
public class SecurityExceptionMappers {

    @ServerExceptionMapper
    public Response unauthorized(UnauthorizedException exception, UriInfo uriInfo) {
        return unauthenticated(uriInfo);
    }

    @ServerExceptionMapper
    public Response authenticationFailed(AuthenticationFailedException exception, UriInfo uriInfo) {
        return unauthenticated(uriInfo);
    }

    @ServerExceptionMapper
    public Response forbidden(ForbiddenException exception, UriInfo uriInfo) {
        return Problem.of(403, "O token informado não tem permissão para este recurso.", RequestPaths.of(uriInfo))
                .toResponse();
    }

    private static Response unauthenticated(UriInfo uriInfo) {
        Response response = Problem.of(401, "Informe um token de acesso válido no cabeçalho Authorization.",
                RequestPaths.of(uriInfo)).toResponse();
        return Response.fromResponse(response).header(HttpHeaders.WWW_AUTHENTICATE, "Bearer").build();
    }
}
