package br.com.fiap.autorepairshop.identity.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api/admin/test-support/ping")
public class AdminProbeResource {

    @GET
    public String ping() {
        return "pong";
    }
}
