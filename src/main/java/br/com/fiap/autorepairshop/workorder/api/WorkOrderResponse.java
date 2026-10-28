package br.com.fiap.autorepairshop.workorder.api;

import br.com.fiap.autorepairshop.workorder.application.WorkOrderDetails;
import br.com.fiap.autorepairshop.workorder.domain.Estimate;
import br.com.fiap.autorepairshop.workorder.domain.EstimateDecision;
import br.com.fiap.autorepairshop.workorder.domain.PartLine;
import br.com.fiap.autorepairshop.workorder.domain.ServiceLine;
import br.com.fiap.autorepairshop.workorder.domain.StatusChange;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Ordem de serviço detalhada, como vista pela equipe da oficina.
 */
@Schema(description = "Ordem de serviço detalhada")
public record WorkOrderResponse(
        @Schema(description = "Código da OS, informado ao cliente") UUID code,
        WorkOrderStatus status,
        CustomerData customer,
        VehicleData vehicle,
        List<ServiceItem> services,
        List<PartItem> parts,
        @Schema(description = "Orçamento; ausente até a conclusão do diagnóstico") EstimateData estimate,
        List<StatusChangeData> statusHistory,
        Instant openedAt) {

    @Schema(description = "Cliente da OS")
    public record CustomerData(UUID id, String name, String document) {
    }

    @Schema(description = "Veículo da OS")
    public record VehicleData(UUID id, String licensePlate, String brand, String model) {
    }

    @Schema(description = "Serviço incluído, com o preço da inclusão")
    public record ServiceItem(UUID lineId, UUID serviceOfferingId, String name, BigDecimal price) {

        static ServiceItem from(ServiceLine line) {
            return new ServiceItem(line.id(), line.serviceOfferingId(), line.name(), line.price().amount());
        }
    }

    @Schema(description = "Peça ou insumo incluído, com o preço unitário da inclusão")
    public record PartItem(UUID lineId, UUID partId, String name, BigDecimal unitPrice, int quantity,
            BigDecimal total) {

        static PartItem from(PartLine line) {
            return new PartItem(line.id(), line.partId(), line.name(), line.unitPrice().amount(), line.quantity(),
                    line.total().amount());
        }
    }

    @Schema(description = "Orçamento e decisão do cliente")
    public record EstimateData(BigDecimal total, Instant generatedAt,
            @Schema(description = "Ausente enquanto aguarda a decisão") EstimateDecision decision,
            Instant decidedAt) {

        static EstimateData from(Estimate estimate) {
            return estimate == null ? null
                    : new EstimateData(estimate.total().amount(), estimate.generatedAt(), estimate.decision(),
                            estimate.decidedAt());
        }
    }

    @Schema(description = "Mudança de status")
    public record StatusChangeData(
            @Schema(description = "Ausente na abertura") WorkOrderStatus from, WorkOrderStatus to, Instant changedAt) {

        static StatusChangeData from(StatusChange change) {
            return new StatusChangeData(change.from(), change.to(), change.changedAt());
        }
    }

    static WorkOrderResponse from(WorkOrderDetails details) {
        WorkOrder workOrder = details.workOrder();
        return new WorkOrderResponse(workOrder.id(), workOrder.status(),
                new CustomerData(details.customer().id(), details.customer().name(), details.customer().document()),
                new VehicleData(details.vehicle().id(), details.vehicle().licensePlate(), details.vehicle().brand(),
                        details.vehicle().model()),
                workOrder.serviceLines().stream().map(ServiceItem::from).toList(),
                workOrder.partLines().stream().map(PartItem::from).toList(),
                EstimateData.from(workOrder.estimate()),
                workOrder.statusHistory().stream().map(StatusChangeData::from).toList(),
                workOrder.openedAt());
    }
}
