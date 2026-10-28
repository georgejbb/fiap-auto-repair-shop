package br.com.fiap.autorepairshop.shared.api;

import jakarta.ws.rs.core.UriInfo;

final class RequestPaths {

    private RequestPaths() {
    }

    /** Caminho da requisição para o campo {@code instance}; nulo quando o contexto HTTP não está disponível. */
    static String of(UriInfo uriInfo) {
        return uriInfo == null ? null : "/" + uriInfo.getPath().replaceFirst("^/", "");
    }
}
