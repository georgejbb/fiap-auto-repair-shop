package br.com.fiap.autorepairshop.shared.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

/**
 * Recurso disponível apenas nos testes para provocar erros de validação e falhas inesperadas.
 */
@Path("/test-support/errors")
public class ErrorProbeResource {

    public record ProbeRequest(@NotBlank String name, @Positive int quantity) {
    }

    @POST
    @Path("/validation")
    @Consumes(MediaType.APPLICATION_JSON)
    public String validate(@Valid ProbeRequest request) {
        return "ok";
    }

    @GET
    @Path("/unexpected")
    public String fail() {
        throw new IllegalStateException("falha interna com detalhe sensível: SELECT * FROM secret");
    }
}
