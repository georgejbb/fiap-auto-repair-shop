package br.com.fiap.autorepairshop.workorder.domain;

/**
 * Etapas da ordem de serviço, na ordem em que normalmente acontecem.
 */
public enum WorkOrderStatus {
    /** OS aberta com cliente e veículo identificados. */
    RECEIVED,
    /** Mecânico avaliando o veículo e definindo os itens. */
    IN_DIAGNOSIS,
    /** Orçamento gerado e enviado; aguarda a decisão do cliente. */
    AWAITING_APPROVAL,
    /** Orçamento aprovado e estoque baixado; serviço em andamento. */
    IN_PROGRESS,
    /** Serviço terminado, ou orçamento rejeitado sem execução. */
    COMPLETED,
    /** Veículo devolvido ao cliente. Status final. */
    DELIVERED
}
