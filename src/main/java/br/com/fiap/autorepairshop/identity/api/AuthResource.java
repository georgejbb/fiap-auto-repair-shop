package br.com.fiap.autorepairshop.identity.api;

import br.com.fiap.autorepairshop.identity.application.AccessToken;
import br.com.fiap.autorepairshop.identity.application.AuthenticationService;
import br.com.fiap.autorepairshop.shared.api.Problem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/auth")
@Tag(name = "Autenticação", description = "Emissão do token de acesso às APIs administrativas")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthenticationService authentication;

    public AuthResource(AuthenticationService authentication) {
        this.authentication = authentication;
    }

    @Schema(description = "Credenciais do usuário administrativo")
    public record LoginRequest(
            @Schema(examples = "admin") @NotBlank String username,
            @Schema(examples = "admin123") @NotBlank String password) {
    }

    @Schema(description = "Token de acesso")
    public record TokenResponse(
            @Schema(description = "JWT a enviar no cabeçalho Authorization") String accessToken,
            @Schema(examples = "Bearer") String tokenType,
            @Schema(description = "Validade do token em segundos", examples = "3600") long expiresIn) {
    }

    @POST
    @Path("/login")
    @Operation(summary = "Autentica um usuário administrativo",
            description = "Devolve um JWT válido por 1 hora, a ser enviado como 'Authorization: Bearer <token>'.")
    @APIResponse(responseCode = "200", description = "Autenticado")
    @APIResponse(responseCode = "401", description = "Login ou senha inválidos",
            content = @Content(mediaType = Problem.MEDIA_TYPE, schema = @Schema(implementation = Problem.class)))
    public TokenResponse login(@Valid @NotNull LoginRequest request) {
        AccessToken token = authentication.login(request.username(), request.password());
        return new TokenResponse(token.value(), "Bearer", token.expiresIn());
    }
}
