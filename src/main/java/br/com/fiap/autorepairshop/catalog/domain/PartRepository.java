package br.com.fiap.autorepairshop.catalog.domain;

import br.com.fiap.autorepairshop.shared.domain.Page;
import java.util.Optional;
import java.util.UUID;

public interface PartRepository {

    Optional<Part> findActiveById(UUID id);

    Page<Part> listActive(PartCategory category, int page, int size);

    void add(Part part);

    /** Grava o item; falha se outro processo alterou o item depois da leitura. */
    void update(Part part);

    void addMovement(StockMovement movement);

    /** Movimentações do item, da mais recente para a mais antiga. */
    Page<StockMovement> listMovements(UUID partId, int page, int size);
}
