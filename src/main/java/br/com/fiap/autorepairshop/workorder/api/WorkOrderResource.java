package br.com.fiap.autorepairshop.workorder.api;

import br.com.fiap.autorepairshop.shared.api.PageResponse;
import br.com.fiap.autorepairshop.shared.domain.Page;
import br.com.fiap.autorepairshop.workorder.application.ExecutionPeriod;
import br.com.fiap.autorepairshop.workorder.application.ExecutionTimeReport;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderDetails;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderFilter;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderQueries;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderService;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderSummary;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
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
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
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
    private final WorkOrderQueries queries;

    public WorkOrderResource(WorkOrderService workOrders, WorkOrderQueries queries) {
        this.workOrders = workOrders;
        this.queries = queries;
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

    @Schema(description = "Ordem de serviço na listagem")
    public record WorkOrderSummaryResponse(UUID code, WorkOrderStatus status, String customerName,
            String licensePlate,
            @Schema(description = "Ausente antes da conclusão do diagnóstico") BigDecimal estimateTotal,
            Instant openedAt) {

        static WorkOrderSummaryResponse from(WorkOrderSummary summary) {
            return new WorkOrderSummaryResponse(summary.code(), summary.status(), summary.customerName(),
                    summary.licensePlate(),
                    summary.estimateTotal() == null ? null : summary.estimateTotal().amount(), summary.openedAt());
        }
    }

    @GET
    @Operation(summary = "Lista ordens de serviço por prioridade de atendimento",
            description = "Ordem: IN_PROGRESS, AWAITING_APPROVAL, IN_DIAGNOSIS, RECEIVED, COMPLETED, DELIVERED; "
                    + "dentro do mesmo status, a aberta há mais tempo primeiro.")
    @APIResponse(responseCode = "400", description = "Status inválido ou paginação fora dos limites")
    public PageResponse<WorkOrderSummaryResponse> list(
            @Parameter(description = "Status a incluir; repita o parâmetro para combinar vários")
            @QueryParam("status") List<String> statuses,
            @Parameter(description = "Id do cliente") @QueryParam("customerId") UUID customerId,
            @QueryParam("page") @DefaultValue("0") @Min(0) int page,
            @QueryParam("size") @DefaultValue("20") @Min(1) @Max(100) int size) {
        Page<WorkOrderSummary> result = queries.list(WorkOrderFilter.of(statuses, customerId), page, size);
        return new PageResponse<>(result.map(WorkOrderSummaryResponse::from).items(), page, size, result.total());
    }

    @GET
    @Path("/metrics/execution-time")
    @Operation(summary = "Tempo médio de execução das ordens de serviço",
            description = "Considera as OS que passaram de IN_PROGRESS para COMPLETED, opcionalmente concluídas "
                    + "no período informado. OS rejeitadas e ainda em execução ficam fora.")
    @APIResponse(responseCode = "400", description = "Data fora do formato ISO-8601 ou período invertido")
    public ExecutionTimeReport executionTime(
            @Parameter(description = "Início do período de conclusão, ISO-8601", example = "2026-10-01T00:00:00Z")
            @QueryParam("from") String from,
            @Parameter(description = "Fim do período de conclusão, ISO-8601", example = "2026-10-31T23:59:59Z")
            @QueryParam("to") String to) {
        return queries.executionTime(ExecutionPeriod.of(from, to));
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
