package br.com.fiap.autorepairshop.catalog.application;

import br.com.fiap.autorepairshop.catalog.domain.Part;
import br.com.fiap.autorepairshop.catalog.domain.PartCategory;
import br.com.fiap.autorepairshop.catalog.domain.PartRepository;
import br.com.fiap.autorepairshop.catalog.domain.StockMovement;
import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.shared.domain.Page;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * Casos de uso de peças, insumos e estoque.
 */
@ApplicationScoped
public class PartService {

    private final PartRepository parts;
    private final Clock clock;

    public PartService(PartRepository parts, Clock clock) {
        this.parts = parts;
        this.clock = clock;
    }

    @Transactional
    public Part create(String name, PartCategory category, BigDecimal unitPrice) {
        Part part = Part.create(name, category, Money.of("unitPrice", unitPrice), now());
        parts.add(part);
        return part;
    }

    public Part get(UUID id) {
        return parts.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Peça ou insumo não encontrado."));
    }

    public Page<Part> list(PartCategory category, int page, int size) {
        return parts.listActive(category, page, size);
    }

    @Transactional
    public Part update(UUID id, String name, PartCategory category, BigDecimal unitPrice) {
        Part part = get(id);
        part.update(name, category, Money.of("unitPrice", unitPrice), now());
        parts.update(part);
        return part;
    }

    @Transactional
    public void delete(UUID id) {
        Part part = get(id);
        part.delete(now());
        parts.update(part);
    }

    /** Soma a quantidade ao estoque e registra a movimentação na mesma transação. */
    @Transactional
    public Part registerInbound(UUID id, int quantity) {
        Part part = get(id);
        StockMovement movement = part.registerInbound(quantity, now());
        parts.update(part);
        parts.addMovement(movement);
        return part;
    }

    /** Fixa o estoque na quantidade contada e registra a diferença na mesma transação. */
    @Transactional
    public Part adjustStock(UUID id, int countedQuantity) {
        Part part = get(id);
        StockMovement movement = part.adjustTo(countedQuantity, now());
        parts.update(part);
        parts.addMovement(movement);
        return part;
    }

    public Page<StockMovement> movements(UUID id, int page, int size) {
        get(id);
        return parts.listMovements(id, page, size);
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
