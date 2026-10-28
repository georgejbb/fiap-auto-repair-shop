package br.com.fiap.autorepairshop.catalog.domain;

/**
 * Origem de uma movimentação de estoque.
 */
public enum StockMovementType {
    /** Entrada por compra ou reposição. */
    INBOUND,
    /** Correção do saldo após contagem física. */
    ADJUSTMENT,
    /** Baixa pela aprovação do orçamento de uma ordem de serviço. */
    CONSUMPTION
}
