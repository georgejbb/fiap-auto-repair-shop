package br.com.fiap.autorepairshop.customer.api;

import br.com.fiap.autorepairshop.customer.application.VehicleService;
import br.com.fiap.autorepairshop.customer.domain.Vehicle;
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
import java.time.Instant;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/admin/vehicles")
@RolesAllowed("ADMIN")
@Tag(name = "Veículos", description = "Cadastro de veículos vinculados a clientes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class VehicleResource {

    private final VehicleService vehicles;

    public VehicleResource(VehicleService vehicles) {
        this.vehicles = vehicles;
    }

    @Schema(description = "Dados para cadastro de veículo")
    public record CreateVehicleRequest(
            @Schema(description = "Placa no formato AAA9999 ou AAA9A99, com ou sem hífen", examples = "BRA2E19")
            @NotBlank String licensePlate,
            @Schema(examples = "Fiat") @NotBlank @Size(max = 60) String brand,
            @Schema(examples = "Argo") @NotBlank @Size(max = 60) String model,
            @Schema(examples = "2022") @NotNull Integer year,
            @Schema(description = "Id do cliente proprietário") @NotNull UUID ownerId) {
    }

    @Schema(description = "Dados alteráveis do veículo; a placa não pode ser alterada")
    public record UpdateVehicleRequest(
            @NotBlank @Size(max = 60) String brand,
            @NotBlank @Size(max = 60) String model,
            @NotNull Integer year,
            @NotNull UUID ownerId) {
    }

    @Schema(description = "Veículo")
    public record VehicleResponse(UUID id, String licensePlate, String brand, String model, int year, UUID ownerId,
            Instant createdAt, Instant updatedAt) {

        static VehicleResponse from(Vehicle vehicle) {
            return new VehicleResponse(vehicle.id(), vehicle.licensePlate().value(), vehicle.brand(), vehicle.model(),
                    vehicle.year(), vehicle.ownerId(), vehicle.createdAt(), vehicle.updatedAt());
        }
    }

    @POST
    @Operation(summary = "Cadastra um veículo")
    @APIResponse(responseCode = "201", description = "Veículo cadastrado")
    @APIResponse(responseCode = "400", description = "Dados inválidos, inclusive placa ou ano")
    @APIResponse(responseCode = "409", description = "Já existe veículo ativo com a placa")
    @APIResponse(responseCode = "422", description = "Proprietário inexistente ou excluído")
    public Response create(@Valid @NotNull CreateVehicleRequest request, @Context UriInfo uriInfo) {
        Vehicle vehicle = vehicles.register(request.licensePlate(), request.brand(), request.model(), request.year(),
                request.ownerId());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(vehicle.id().toString()).build())
                .entity(VehicleResponse.from(vehicle))
                .build();
    }

    @GET
    @Operation(summary = "Lista veículos ativos",
            description = "Com licensePlate, devolve somente o veículo da placa; com ownerId, os veículos do cliente.")
    public PageResponse<VehicleResponse> list(
            @Parameter(description = "Placa, com ou sem hífen") @QueryParam("licensePlate") String licensePlate,
            @Parameter(description = "Id do cliente proprietário") @QueryParam("ownerId") UUID ownerId,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        Page<Vehicle> result;
        if (licensePlate != null && !licensePlate.isBlank()) {
            result = vehicles.findByLicensePlate(licensePlate);
        } else if (ownerId != null) {
            result = vehicles.listByOwner(ownerId, page, size);
        } else {
            result = vehicles.list(page, size);
        }
        return new PageResponse<>(result.map(VehicleResponse::from).items(), page, size, result.total());
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Consulta um veículo")
    @APIResponse(responseCode = "404", description = "Veículo inexistente ou excluído")
    public VehicleResponse get(@PathParam("id") UUID id) {
        return VehicleResponse.from(vehicles.get(id));
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "Altera marca, modelo, ano e proprietário de um veículo")
    public VehicleResponse update(@PathParam("id") UUID id, @Valid @NotNull UpdateVehicleRequest request) {
        return VehicleResponse.from(vehicles.update(id, request.brand(), request.model(), request.year(),
                request.ownerId()));
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Exclui logicamente um veículo")
    @APIResponse(responseCode = "204", description = "Veículo excluído")
    public void delete(@PathParam("id") UUID id) {
        vehicles.delete(id);
    }
}
