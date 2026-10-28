package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.shared.domain.Page;

/**
 * Consultas de leitura sobre as ordens de serviço. Não carregam o agregado e podem combinar dados de outros
 * contextos numa única consulta; gravações continuam passando pelo agregado e pelos casos de uso.
 */
public interface WorkOrderQueries {

    /** Lista as OS por prioridade de atendimento: status e, dentro do status, a mais antiga primeiro. */
    Page<WorkOrderSummary> list(WorkOrderFilter filter, int page, int size);

    /** Tempo médio de execução, geral e por serviço, das OS concluídas no período. */
    ExecutionTimeReport executionTime(ExecutionPeriod period);
}
