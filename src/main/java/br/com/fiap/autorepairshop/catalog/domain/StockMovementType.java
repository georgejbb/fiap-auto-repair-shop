package br.com.fiap.autorepairshop.catalog.domain;

/**
 * Origem de uma movimentação de estoque.
 */
public enum StockMovementType {
    /** Entrada por compra ou reposição. */
    INBOUND,
    /** Correção do saldo após contagem física. */
    ADJUSTMENT
}
