package br.com.fiap.autorepairshop.shared.api;

import jakarta.ws.rs.core.UriInfo;

final class RequestPaths {

    private RequestPaths() {
    }

    static String of(UriInfo uriInfo) {
        return uriInfo == null ? null : "/" + uriInfo.getPath().replaceFirst("^/", "");
    }
}
