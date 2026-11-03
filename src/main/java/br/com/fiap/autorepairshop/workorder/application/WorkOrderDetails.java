package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.workorder.domain.WorkOrder;
import java.util.UUID;

public record WorkOrderDetails(WorkOrder workOrder, CustomerSummary customer, VehicleSummary vehicle) {

    public record CustomerSummary(UUID id, String name, String document, String email) {
    }

    public record VehicleSummary(UUID id, String licensePlate, String brand, String model) {
    }
}
