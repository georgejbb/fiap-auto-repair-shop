package br.com.fiap.autorepairshop.customer.infrastructure;

import br.com.fiap.autorepairshop.customer.domain.LicensePlate;
import br.com.fiap.autorepairshop.customer.domain.Vehicle;
import br.com.fiap.autorepairshop.customer.domain.VehicleRepository;
import br.com.fiap.autorepairshop.shared.domain.Page;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
class PanacheVehicleRepository implements VehicleRepository, PanacheRepositoryBase<VehicleEntity, UUID> {

    private static final Sort ORDER = Sort.by("licensePlate").and("id");

    @Override
    public Optional<Vehicle> findActiveById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional().map(PanacheVehicleRepository::toDomain);
    }

    @Override
    public Optional<Vehicle> findActiveByLicensePlate(String licensePlate) {
        return find("licensePlate = ?1 and deletedAt is null", licensePlate).firstResultOptional()
                .map(PanacheVehicleRepository::toDomain);
    }

    @Override
    public Page<Vehicle> listActive(int page, int size) {
        return page(find("deletedAt is null", ORDER), page, size);
    }

    @Override
    public Page<Vehicle> listActiveByOwner(UUID ownerId, int page, int size) {
        return page(find("ownerId = ?1 and deletedAt is null", ORDER, ownerId), page, size);
    }

    @Override
    public void add(Vehicle vehicle) {
        VehicleEntity entity = new VehicleEntity();
        entity.id = vehicle.id();
        copy(vehicle, entity);
        persistAndFlush(entity);
    }

    @Override
    public void update(Vehicle vehicle) {
        copy(vehicle, findById(vehicle.id()));
        flush();
    }

    private static Page<Vehicle> page(PanacheQuery<VehicleEntity> query, int page, int size) {
        return new Page<>(query.page(page, size).list().stream().map(PanacheVehicleRepository::toDomain).toList(),
                query.count());
    }

    private static void copy(Vehicle vehicle, VehicleEntity entity) {
        entity.licensePlate = vehicle.licensePlate().value();
        entity.brand = vehicle.brand();
        entity.model = vehicle.model();
        entity.year = vehicle.year();
        entity.ownerId = vehicle.ownerId();
        entity.createdAt = vehicle.createdAt();
        entity.updatedAt = vehicle.updatedAt();
        entity.deletedAt = vehicle.deletedAt();
    }

    private static Vehicle toDomain(VehicleEntity entity) {
        return new Vehicle(entity.id, new LicensePlate(entity.licensePlate), entity.brand, entity.model, entity.year,
                entity.ownerId, entity.createdAt, entity.updatedAt, entity.deletedAt);
    }
}
