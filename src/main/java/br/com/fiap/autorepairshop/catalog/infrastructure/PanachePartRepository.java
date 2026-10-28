package br.com.fiap.autorepairshop.catalog.infrastructure;

import br.com.fiap.autorepairshop.catalog.domain.Part;
import br.com.fiap.autorepairshop.catalog.domain.PartCategory;
import br.com.fiap.autorepairshop.catalog.domain.PartRepository;
import br.com.fiap.autorepairshop.catalog.domain.StockMovement;
import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.shared.domain.Page;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
class PanachePartRepository implements PartRepository, PanacheRepositoryBase<PartEntity, UUID> {

    private static final Sort ORDER = Sort.by("name").and("id");

    private final StockMovements movements;

    PanachePartRepository(StockMovements movements) {
        this.movements = movements;
    }

    @Override
    public Optional<Part> findActiveById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional().map(PanachePartRepository::toDomain);
    }

    @Override
    public Page<Part> listActive(PartCategory category, int page, int size) {
        PanacheQuery<PartEntity> query = category == null
                ? find("deletedAt is null", ORDER)
                : find("category = ?1 and deletedAt is null", ORDER, category);
        return new Page<>(query.page(page, size).list().stream().map(PanachePartRepository::toDomain).toList(),
                query.count());
    }

    @Override
    public void add(Part part) {
        PartEntity entity = new PartEntity();
        entity.id = part.id();
        copy(part, entity);
        persistAndFlush(entity);
    }

    /**
     * A entidade já está no contexto de persistência desde a leitura feita na mesma transação; a coluna
     * {@code version} faz o flush falhar se outra transação gravou o item depois dessa leitura.
     */
    @Override
    public void update(Part part) {
        copy(part, findById(part.id()));
        flush();
    }

    @Override
    public void addMovement(StockMovement movement) {
        StockMovementEntity entity = new StockMovementEntity();
        entity.id = movement.id();
        entity.partId = movement.partId();
        entity.type = movement.type();
        entity.quantity = movement.quantity();
        entity.occurredAt = movement.occurredAt();
        movements.persistAndFlush(entity);
    }

    @Override
    public Page<StockMovement> listMovements(UUID partId, int page, int size) {
        PanacheQuery<StockMovementEntity> query = movements.find("partId",
                Sort.descending("occurredAt").and("id", Sort.Direction.Descending), partId);
        return new Page<>(query.page(page, size).list().stream().map(PanachePartRepository::toDomain).toList(),
                query.count());
    }

    private static void copy(Part part, PartEntity entity) {
        entity.name = part.name();
        entity.category = part.category();
        entity.unitPrice = part.unitPrice().amount();
        entity.stockQuantity = part.stockQuantity();
        entity.createdAt = part.createdAt();
        entity.updatedAt = part.updatedAt();
        entity.deletedAt = part.deletedAt();
    }

    private static Part toDomain(PartEntity entity) {
        return new Part(entity.id, entity.name, entity.category, new Money(entity.unitPrice), entity.stockQuantity,
                entity.version, entity.createdAt, entity.updatedAt, entity.deletedAt);
    }

    private static StockMovement toDomain(StockMovementEntity entity) {
        return new StockMovement(entity.id, entity.partId, entity.type, entity.quantity, entity.occurredAt);
    }

    @ApplicationScoped
    static class StockMovements implements PanacheRepositoryBase<StockMovementEntity, UUID> {
    }
}
