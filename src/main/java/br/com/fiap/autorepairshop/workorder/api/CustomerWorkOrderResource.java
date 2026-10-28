package br.com.fiap.autorepairshop.workorder.api;

import br.com.fiap.autorepairshop.workorder.application.CustomerDecisionService;
import br.com.fiap.autorepairshop.workorder.application.WorkOrderDetails;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/**
 * Acompanhamento e decisão do orçamento pelo cliente, sem conta no sistema.
 *
 * <p>O documento vai em cabeçalho, e não na URL, para não ficar registrado em logs de acesso nem no
 * histórico do navegador.
 */
@Path("/api/public/work-orders/{code}")
@Tag(name = "Acompanhamento pelo cliente",
        description = "Consulta e decisão do orçamento com o código da OS e o CPF ou CNPJ do cliente")
@Produces(MediaType.APPLICATION_JSON)
public class CustomerWorkOrderResource {

    static final String DOCUMENT_HEADER = "X-Customer-Document";

    private final CustomerDecisionService decisions;

    public CustomerWorkOrderResource(CustomerDecisionService decisions) {
        this.decisions = decisions;
    }

    @Schema(description = "Ordem de serviço como vista pelo cliente")
    public record CustomerWorkOrderResponse(UUID code, WorkOrderStatus status, String licensePlate,
            List<Item> items, WorkOrderResponse.EstimateData estimate,
            List<WorkOrderResponse.StatusChangeData> statusHistory) {

        @Schema(description = "Item da OS")
        public record Item(String description, int quantity, BigDecimal total) {
        }

        static CustomerWorkOrderResponse from(WorkOrderDetails details) {
            WorkOrder workOrder = details.workOrder();
            List<Item> items = new ArrayList<>();
            workOrder.serviceLines().forEach(line -> items.add(new Item(line.name(), 1, line.price().amount())));
            workOrder.partLines().forEach(line -> items.add(
                    new Item(line.name(), line.quantity(), line.total().amount())));
            return new CustomerWorkOrderResponse(workOrder.id(), workOrder.status(), details.vehicle().licensePlate(),
                    items, WorkOrderResponse.EstimateData.from(workOrder.estimate()),
                    workOrder.statusHistory().stream().map(WorkOrderResponse.StatusChangeData::from).toList());
        }
    }

    @GET
    @Operation(summary = "Consulta o andamento da ordem de serviço")
    @APIResponse(responseCode = "200", description = "Status, histórico, itens e orçamento")
    @APIResponse(responseCode = "404", description = "Código inexistente ou documento que não é o do cliente da OS")
    public CustomerWorkOrderResponse consult(@PathParam("code") UUID code,
            @Parameter(description = "CPF ou CNPJ do cliente, com ou sem pontuação")
            @HeaderParam(DOCUMENT_HEADER) @NotBlank String document) {
        return CustomerWorkOrderResponse.from(decisions.consult(code, document));
    }

    @POST
    @Path("/approve")
    @Operation(summary = "Aprova o orçamento",
            description = "A OS passa a IN_PROGRESS e o estoque das peças e insumos é baixado numa única transação.")
    @APIResponse(responseCode = "404", description = "Código inexistente ou documento que não é o do cliente da OS")
    @APIResponse(responseCode = "409", description = "Estoque alterado por outra operação simultânea")
    @APIResponse(responseCode = "422", description = "OS fora de AWAITING_APPROVAL ou estoque insuficiente")
    public CustomerWorkOrderResponse approve(@PathParam("code") UUID code,
            @HeaderParam(DOCUMENT_HEADER) @NotBlank String document) {
        return CustomerWorkOrderResponse.from(decisions.approve(code, document));
    }

    @POST
    @Path("/reject")
    @Operation(summary = "Rejeita o orçamento", description = "A OS passa a COMPLETED sem execução e sem baixa de estoque.")
    @APIResponse(responseCode = "404", description = "Código inexistente ou documento que não é o do cliente da OS")
    @APIResponse(responseCode = "422", description = "OS fora de AWAITING_APPROVAL")
    public CustomerWorkOrderResponse reject(@PathParam("code") UUID code,
            @HeaderParam(DOCUMENT_HEADER) @NotBlank String document) {
        return CustomerWorkOrderResponse.from(decisions.reject(code, document));
    }
}
