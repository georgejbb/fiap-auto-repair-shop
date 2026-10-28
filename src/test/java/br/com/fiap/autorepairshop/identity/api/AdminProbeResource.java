package br.com.fiap.autorepairshop.identity.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

/**
 * Rota administrativa disponível apenas nos testes, para verificar a política de acesso.
 */
@Path("/api/admin/test-support/ping")
public class AdminProbeResource {

    @GET
    public String ping() {
        return "pong";
    }
}
