package br.com.fiap.autorepairshop.workorder.domain;

import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository {

    Optional<WorkOrder> findByCode(UUID code);

    void add(WorkOrder workOrder);

    /** Grava a OS; falha se outra transação alterou a mesma OS depois da leitura. */
    void update(WorkOrder workOrder);
}
