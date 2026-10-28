package br.com.fiap.autorepairshop.catalog.api;

import br.com.fiap.autorepairshop.catalog.application.PartService;
import br.com.fiap.autorepairshop.catalog.domain.Part;
import br.com.fiap.autorepairshop.catalog.domain.PartCategory;
import br.com.fiap.autorepairshop.catalog.domain.StockMovement;
import br.com.fiap.autorepairshop.catalog.domain.StockMovementType;
import br.com.fiap.autorepairshop.shared.api.PageResponse;
import br.com.fiap.autorepairshop.shared.domain.Page;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/admin/parts")
@RolesAllowed("ADMIN")
@Tag(name = "Peças e insumos", description = "Cadastro de peças e insumos e controle de estoque")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PartResource {

    private final PartService parts;

    public PartResource(PartService parts) {
        this.parts = parts;
    }

    @Schema(description = "Dados cadastrais da peça ou insumo; o estoque muda apenas por entrada ou ajuste")
    public record PartRequest(
            @Schema(examples = "Óleo 5W30") @NotBlank @Size(max = 120) String name,
            @Schema(description = "PART para peça, SUPPLY para insumo") @NotNull PartCategory category,
            @Schema(description = "Preço unitário em reais, com até duas casas decimais", examples = "45.90")
            @NotNull BigDecimal unitPrice) {
    }

    @Schema(description = "Entrada de estoque")
    public record StockEntryRequest(@Schema(examples = "10") @NotNull Integer quantity) {
    }

    @Schema(description = "Ajuste de estoque por contagem física")
    public record StockAdjustmentRequest(
            @Schema(description = "Quantidade contada, que passa a ser o estoque", examples = "7")
            @NotNull Integer countedQuantity) {
    }

    @Schema(description = "Peça ou insumo")
    public record PartResponse(UUID id, String name, PartCategory category, BigDecimal unitPrice, int stockQuantity,
            Instant createdAt, Instant updatedAt) {

        static PartResponse from(Part part) {
            return new PartResponse(part.id(), part.name(), part.category(), part.unitPrice().amount(),
                    part.stockQuantity(), part.createdAt(), part.updatedAt());
        }
    }

    @Schema(description = "Movimentação de estoque")
    public record StockMovementResponse(UUID id, StockMovementType type,
            @Schema(description = "Variação do estoque; negativa quando o saldo diminuiu") int quantity,
            Instant occurredAt) {

        static StockMovementResponse from(StockMovement movement) {
            return new StockMovementResponse(movement.id(), movement.type(), movement.quantity(),
                    movement.occurredAt());
        }
    }

    @POST
    @Operation(summary = "Cadastra uma peça ou insumo com estoque zero")
    @APIResponse(responseCode = "201", description = "Item cadastrado")
    @APIResponse(responseCode = "400", description = "Dados inválidos")
    public Response create(@Valid @NotNull PartRequest request, @Context UriInfo uriInfo) {
        Part part = parts.create(request.name(), request.category(), request.unitPrice());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(part.id().toString()).build())
                .entity(PartResponse.from(part))
                .build();
    }

    @GET
    @Operation(summary = "Lista peças e insumos ativos")
    public PageResponse<PartResponse> list(
            @Parameter(description = "Filtra por categoria") @QueryParam("category") PartCategory category,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        Page<Part> result = parts.list(category, page, size);
        return new PageResponse<>(result.map(PartResponse::from).items(), page, size, result.total());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consulta uma peça ou insumo, com o estoque atual")
    @APIResponse(responseCode = "404", description = "Item inexistente ou excluído")
    public PartResponse get(@PathParam("id") UUID id) {
        return PartResponse.from(parts.get(id));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Altera nome, categoria e preço unitário", description = "Não altera o estoque.")
    public PartResponse update(@PathParam("id") UUID id, @Valid @NotNull PartRequest request) {
        return PartResponse.from(parts.update(id, request.name(), request.category(), request.unitPrice()));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Exclui logicamente uma peça ou insumo")
    @APIResponse(responseCode = "204", description = "Item excluído")
    public void delete(@PathParam("id") UUID id) {
        parts.delete(id);
    }

    @POST
    @Path("/{id}/stock-entries")
    @Operation(summary = "Registra entrada de estoque")
    @APIResponse(responseCode = "409", description = "O item foi alterado por outra operação simultânea")
    public PartResponse registerInbound(@PathParam("id") UUID id, @Valid @NotNull StockEntryRequest request) {
        return PartResponse.from(parts.registerInbound(id, request.quantity()));
    }

    @POST
    @Path("/{id}/stock-adjustments")
    @Operation(summary = "Ajusta o estoque para a quantidade contada")
    @APIResponse(responseCode = "409", description = "O item foi alterado por outra operação simultânea")
    public PartResponse adjustStock(@PathParam("id") UUID id, @Valid @NotNull StockAdjustmentRequest request) {
        return PartResponse.from(parts.adjustStock(id, request.countedQuantity()));
    }

    @GET
    @Path("/{id}/movements")
    @Operation(summary = "Lista as movimentações de estoque, da mais recente para a mais antiga")
    public PageResponse<StockMovementResponse> movements(@PathParam("id") UUID id,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        Page<StockMovement> result = parts.movements(id, page, size);
        return new PageResponse<>(result.map(StockMovementResponse::from).items(), page, size, result.total());
    }
}
