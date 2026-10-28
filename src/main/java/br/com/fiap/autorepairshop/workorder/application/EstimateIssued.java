package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.shared.domain.Money;
import java.util.List;
import java.util.UUID;

/**
 * Evento de aplicação disparado quando o orçamento é gerado, com os dados necessários para avisar o cliente.
 *
 * @param workOrderCode código da OS, que o cliente usa para consultar e decidir
 * @param customerName  nome do cliente
 * @param customerEmail e-mail do cliente
 * @param licensePlate  placa do veículo
 * @param items         itens do orçamento
 * @param total         total do orçamento
 */
public record EstimateIssued(UUID workOrderCode, String customerName, String customerEmail, String licensePlate,
        List<Item> items, Money total) {

    /** Item do orçamento: descrição, quantidade e valor total da linha. */
    public record Item(String description, int quantity, Money amount) {
    }
}
