package br.com.fiap.autorepairshop.workorder.domain;

import br.com.fiap.autorepairshop.shared.domain.Money;
import java.util.Objects;
import java.util.UUID;

/**
 * Peça ou insumo incluído na OS, com nome e preço unitário copiados do catálogo no momento da inclusão.
 */
public record PartLine(UUID id, UUID partId, String name, Money unitPrice, int quantity) {

    public PartLine {
        Objects.requireNonNull(id);
        Objects.requireNonNull(partId);
        Objects.requireNonNull(name);
        Objects.requireNonNull(unitPrice);
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser positiva: " + quantity);
        }
    }

    public Money total() {
        return unitPrice.times(quantity);
    }

    PartLine withQuantity(int newQuantity) {
        return new PartLine(id, partId, name, unitPrice, newQuantity);
    }
}
