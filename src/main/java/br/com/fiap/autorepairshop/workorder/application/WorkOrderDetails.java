package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import java.util.UUID;

/**
 * Ordem de serviço com os dados de cliente e veículo obtidos do contexto Cadastro.
 *
 * @param workOrder ordem de serviço
 * @param customer  resumo do cliente, mesmo que excluído depois da abertura
 * @param vehicle   resumo do veículo, mesmo que excluído depois da abertura
 */
public record WorkOrderDetails(WorkOrder workOrder, CustomerSummary customer, VehicleSummary vehicle) {

    /** Dados do cliente usados pela OS. */
    public record CustomerSummary(UUID id, String name, String document, String email) {
    }

    /** Dados do veículo usados pela OS. */
    public record VehicleSummary(UUID id, String licensePlate, String brand, String model) {
    }
}
