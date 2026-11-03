package br.com.fiap.autorepairshop.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "Problem", description = "Resposta de erro no formato Problem Details (RFC 9457)")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Problem(String type, String title, int status, String detail, String instance, List<FieldError> errors) {

    public static final String MEDIA_TYPE = "application/problem+json";

    private static final String DEFAULT_TYPE = "about:blank";

    @Schema(name = "FieldError", description = "Violação de validação de um campo")
    public record FieldError(String field, String message) {
    }

    public static Problem of(int status, String detail, String instance) {
        return new Problem(DEFAULT_TYPE, titleFor(status), status, detail, instance, null);
    }

    public static Problem validation(String detail, String instance, List<FieldError> errors) {
        return new Problem(DEFAULT_TYPE, titleFor(400), 400, detail, instance, List.copyOf(errors));
    }

    public Response toResponse() {
        return Response.status(status).type(MEDIA_TYPE).entity(this).build();
    }

    static String titleFor(int status) {
        return switch (status) {
            case 400 -> "Requisição inválida";
            case 401 -> "Não autenticado";
            case 403 -> "Acesso negado";
            case 404 -> "Recurso não encontrado";
            case 405 -> "Método não permitido";
            case 409 -> "Conflito com o estado atual";
            case 415 -> "Tipo de conteúdo não suportado";
            case 422 -> "Regra de negócio violada";
            case 500 -> "Erro interno";
            default -> "Erro HTTP " + status;
        };
    }
}
