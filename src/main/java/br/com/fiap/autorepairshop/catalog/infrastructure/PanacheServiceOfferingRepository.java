package br.com.fiap.autorepairshop.catalog.infrastructure;

import br.com.fiap.autorepairshop.catalog.domain.ServiceOffering;
import br.com.fiap.autorepairshop.catalog.domain.ServiceOfferingRepository;
import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.shared.domain.Page;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
class PanacheServiceOfferingRepository
        implements ServiceOfferingRepository, PanacheRepositoryBase<ServiceOfferingEntity, UUID> {

    @Override
    public Optional<ServiceOffering> findActiveById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional()
                .map(PanacheServiceOfferingRepository::toDomain);
    }

    @Override
    public Page<ServiceOffering> listActive(int page, int size) {
        PanacheQuery<ServiceOfferingEntity> query = find("deletedAt is null", Sort.by("name").and("id"));
        return new Page<>(query.page(page, size).list().stream()
                .map(PanacheServiceOfferingRepository::toDomain).toList(), query.count());
    }

    @Override
    public void add(ServiceOffering offering) {
        ServiceOfferingEntity entity = new ServiceOfferingEntity();
        entity.id = offering.id();
        copy(offering, entity);
        persistAndFlush(entity);
    }

    @Override
    public void update(ServiceOffering offering) {
        copy(offering, findById(offering.id()));
        flush();
    }

    private static void copy(ServiceOffering offering, ServiceOfferingEntity entity) {
        entity.name = offering.name();
        entity.description = offering.description();
        entity.price = offering.price().amount();
        entity.createdAt = offering.createdAt();
        entity.updatedAt = offering.updatedAt();
        entity.deletedAt = offering.deletedAt();
    }

    private static ServiceOffering toDomain(ServiceOfferingEntity entity) {
        return new ServiceOffering(entity.id, entity.name, entity.description, new Money(entity.price),
                entity.createdAt, entity.updatedAt, entity.deletedAt);
    }
}
