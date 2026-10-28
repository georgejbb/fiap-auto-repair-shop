package br.com.fiap.autorepairshop.workorder.api;

import br.com.fiap.autorepairshop.workorder.application.WorkOrderDetails;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Operações da equipe da oficina sobre a ordem de serviço. Cada transição de status é uma rota própria,
 * porque cada uma tem efeitos diferentes (gerar orçamento, enviar e-mail, encerrar a OS).
 */
@Path("/api/admin/work-orders")
@RolesAllowed("ADMIN")
@Tag(name = "Ordens de serviço", description = "Abertura, diagnóstico, orçamento, execução e entrega")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class WorkOrderResource {

    private final WorkOrderService workOrders;

    public WorkOrderResource(WorkOrderService workOrders) {
        this.workOrders = workOrders;
    }

    @Schema(description = "Cliente e veículo da nova OS")
    public record OpenWorkOrderRequest(@NotNull UUID customerId, @NotNull UUID vehicleId) {
    }

    @Schema(description = "Serviço do catálogo a incluir")
    public record AddServiceRequest(@NotNull UUID serviceOfferingId) {
    }

    @Schema(description = "Peça ou insumo a incluir")
    public record AddPartRequest(@NotNull UUID partId, @Schema(examples = "2") @NotNull Integer quantity) {
    }

    @POST
    @Operation(summary = "Abre uma ordem de serviço", description = "A OS começa no status RECEIVED.")
    @APIResponse(responseCode = "201", description = "OS aberta")
    @APIResponse(responseCode = "422", description = "Cliente ou veículo inexistente, ou veículo de outro cliente")
    public Response open(@Valid @NotNull OpenWorkOrderRequest request, @Context UriInfo uriInfo) {
        WorkOrderDetails details = workOrders.open(request.customerId(), request.vehicleId());
        return Response.created(uriInfo.getAbsolutePathBuilder().path(details.workOrder().id().toString()).build())
                .entity(WorkOrderResponse.from(details))
                .build();
    }

    @GET
    @Path("/{code}")
    @Operation(summary = "Detalha uma ordem de serviço", description = "Itens, orçamento e histórico de status.")
    @APIResponse(responseCode = "404", description = "OS inexistente")
    public WorkOrderResponse get(@PathParam("code") UUID code) {
        return WorkOrderResponse.from(workOrders.get(code));
    }

    @POST
    @Path("/{code}/start-diagnosis")
    @Operation(summary = "Inicia o diagnóstico", description = "RECEIVED para IN_DIAGNOSIS.")
    @APIResponse(responseCode = "422", description = "Transição não permitida no status atual")
    public WorkOrderResponse startDiagnosis(@PathParam("code") UUID code) {
        return WorkOrderResponse.from(workOrders.startDiagnosis(code));
    }

    @POST
    @Path("/{code}/services")
    @Operation(summary = "Inclui um serviço do catálogo", description = "Somente em IN_DIAGNOSIS.")
    @APIResponse(responseCode = "422", description = "Fora do diagnóstico, serviço repetido ou inexistente")
    public WorkOrderResponse addService(@PathParam("code") UUID code, @Valid @NotNull AddServiceRequest request) {
        return WorkOrderResponse.from(workOrders.addService(code, request.serviceOfferingId()));
    }

    @DELETE
    @Path("/{code}/services/{lineId}")
    @Operation(summary = "Remove um serviço da OS", description = "Somente em IN_DIAGNOSIS.")
    public WorkOrderResponse removeService(@PathParam("code") UUID code, @PathParam("lineId") UUID lineId) {
        return WorkOrderResponse.from(workOrders.removeService(code, lineId));
    }

    @POST
    @Path("/{code}/parts")
    @Operation(summary = "Inclui uma peça ou insumo",
            description = "Somente em IN_DIAGNOSIS. Soma à linha existente do mesmo item; o total não pode superar o estoque.")
    @APIResponse(responseCode = "422", description = "Fora do diagnóstico, estoque insuficiente ou item inexistente")
    public WorkOrderResponse addPart(@PathParam("code") UUID code, @Valid @NotNull AddPartRequest request) {
        return WorkOrderResponse.from(workOrders.addPart(code, request.partId(), request.quantity()));
    }

    @DELETE
    @Path("/{code}/parts/{lineId}")
    @Operation(summary = "Remove uma peça ou insumo da OS", description = "Somente em IN_DIAGNOSIS.")
    public WorkOrderResponse removePart(@PathParam("code") UUID code, @PathParam("lineId") UUID lineId) {
        return WorkOrderResponse.from(workOrders.removePart(code, lineId));
    }

    @POST
    @Path("/{code}/complete-diagnosis")
    @Operation(summary = "Conclui o diagnóstico e gera o orçamento",
            description = "IN_DIAGNOSIS para AWAITING_APPROVAL. O orçamento é enviado por e-mail ao cliente.")
    @APIResponse(responseCode = "422", description = "OS sem itens ou fora do diagnóstico")
    public WorkOrderResponse completeDiagnosis(@PathParam("code") UUID code) {
        return WorkOrderResponse.from(workOrders.completeDiagnosis(code));
    }

    @POST
    @Path("/{code}/finish")
    @Operation(summary = "Finaliza a execução", description = "IN_PROGRESS para COMPLETED.")
    @APIResponse(responseCode = "422", description = "Transição não permitida no status atual")
    public WorkOrderResponse finish(@PathParam("code") UUID code) {
        return WorkOrderResponse.from(workOrders.finish(code));
    }

    @POST
    @Path("/{code}/deliver")
    @Operation(summary = "Registra a entrega do veículo", description = "COMPLETED para DELIVERED.")
    @APIResponse(responseCode = "422", description = "Transição não permitida no status atual")
    public WorkOrderResponse deliver(@PathParam("code") UUID code) {
        return WorkOrderResponse.from(workOrders.deliver(code));
    }
}
