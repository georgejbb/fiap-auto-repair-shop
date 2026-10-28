package br.com.fiap.autorepairshop.workorder.application;

import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import br.com.fiap.autorepairshop.workorder.domain.WorkOrderStatus;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Filtros da listagem de ordens de serviço.
 *
 * @param statuses   status aceitos; vazio para todos
 * @param customerId cliente das OS; nulo para todos
 */
public record WorkOrderFilter(List<WorkOrderStatus> statuses, UUID customerId) {

    public WorkOrderFilter {
        statuses = List.copyOf(statuses);
    }

    /**
     * Converte os status recebidos como texto. Um valor que não é status da OS é erro de entrada (400),
     * e não recurso inexistente, que seria a resposta padrão do JAX-RS para parâmetro inválido.
     */
    public static WorkOrderFilter of(List<String> rawStatuses, UUID customerId) {
        List<WorkOrderStatus> statuses = rawStatuses == null ? List.of() : rawStatuses.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(WorkOrderFilter::parse)
                .toList();
        return new WorkOrderFilter(statuses, customerId);
    }

    private static WorkOrderStatus parse(String value) {
        try {
            return WorkOrderStatus.valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new InvalidValueException("status", "valor inválido: " + value);
        }
    }
}
