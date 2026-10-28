package br.com.fiap.autorepairshop.catalog.api;

import br.com.fiap.autorepairshop.catalog.application.ServiceOfferingService;
import br.com.fiap.autorepairshop.catalog.domain.ServiceOffering;
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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/admin/service-offerings")
@RolesAllowed("ADMIN")
@Tag(name = "Serviços", description = "Catálogo de serviços de mão de obra")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ServiceOfferingResource {

    private final ServiceOfferingService offerings;

    public ServiceOfferingResource(ServiceOfferingService offerings) {
        this.offerings = offerings;
    }

    @Schema(description = "Dados do serviço")
    public record ServiceOfferingRequest(
            @Schema(examples = "Troca de óleo") @NotBlank @Size(max = 120) String name,
            @Schema(examples = "Troca do óleo do motor e do filtro") @Size(max = 500) String description,
            @Schema(description = "Preço em reais, com até duas casas decimais", examples = "120.00")
            @NotNull BigDecimal price) {
    }

    @Schema(description = "Serviço do catálogo")
    public record ServiceOfferingResponse(UUID id, String name, String description, BigDecimal price,
            Instant createdAt, Instant updatedAt) {

        static ServiceOfferingResponse from(ServiceOffering offering) {
            return new ServiceOfferingResponse(offering.id(), offering.name(), offering.description(),
                    offering.price().amount(), offering.createdAt(), offering.updatedAt());
        }
    }

    @POST
    @Operation(summary = "Cadastra um serviço")
    @APIResponse(responseCode = "201", description = "Serviço cadastrado")
    @APIResponse(responseCode = "400", description = "Dados inválidos, inclusive preço negativo ou com mais de duas casas")
    public Response create(@Valid @NotNull ServiceOfferingRequest request, @Context UriInfo uriInfo) {
        ServiceOffering offering = offerings.create(request.name(), request.description(), request.price());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(offering.id().toString()).build())
                .entity(ServiceOfferingResponse.from(offering))
                .build();
    }

    @GET
    @Operation(summary = "Lista serviços ativos")
    public PageResponse<ServiceOfferingResponse> list(
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        Page<ServiceOffering> result = offerings.list(page, size);
        return new PageResponse<>(result.map(ServiceOfferingResponse::from).items(), page, size, result.total());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consulta um serviço")
    @APIResponse(responseCode = "404", description = "Serviço inexistente ou excluído")
    public ServiceOfferingResponse get(@PathParam("id") UUID id) {
        return ServiceOfferingResponse.from(offerings.get(id));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Altera nome, descrição e preço de um serviço")
    public ServiceOfferingResponse update(@PathParam("id") UUID id, @Valid @NotNull ServiceOfferingRequest request) {
        return ServiceOfferingResponse.from(
                offerings.update(id, request.name(), request.description(), request.price()));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Exclui logicamente um serviço")
    @APIResponse(responseCode = "204", description = "Serviço excluído")
    public void delete(@PathParam("id") UUID id) {
        offerings.delete(id);
    }
}
