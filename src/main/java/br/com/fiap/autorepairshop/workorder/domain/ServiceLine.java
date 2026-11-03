package br.com.fiap.autorepairshop.workorder.domain;

import br.com.fiap.autorepairshop.shared.domain.Money;
import java.util.Objects;
import java.util.UUID;

/**
 * Serviço incluído na OS, com nome e preço copiados do catálogo no momento da inclusão.
 */
public record ServiceLine(UUID id, UUID serviceOfferingId, String name, Money price) {

    public ServiceLine {
        Objects.requireNonNull(id);
        Objects.requireNonNull(serviceOfferingId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(price);
    }
}
