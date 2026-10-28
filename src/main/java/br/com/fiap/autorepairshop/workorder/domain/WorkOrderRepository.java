package br.com.fiap.autorepairshop.workorder.domain;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistência das ordens de serviço, com itens, orçamento e histórico.
 */
public interface WorkOrderRepository {

    /** Busca pelo código da OS, que é o identificador informado ao cliente. */
    Optional<WorkOrder> findByCode(UUID code);

    void add(WorkOrder workOrder);

    /** Grava a OS; falha se outra transação alterou a mesma OS depois da leitura. */
    void update(WorkOrder workOrder);
}
