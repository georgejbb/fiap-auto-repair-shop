package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.shared.domain.Page;

/**
 * Consultas de leitura sobre as ordens de serviço. Não carregam o agregado e podem combinar dados de outros
 * contextos numa única consulta; gravações continuam passando pelo agregado e pelos casos de uso.
 */
public interface WorkOrderQueries {

    Page<WorkOrderSummary> list(WorkOrderFilter filter, int page, int size);

    ExecutionTimeReport executionTime(ExecutionPeriod period);
}
