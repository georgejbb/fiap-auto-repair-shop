package br.com.fiap.autorepairshop.customer.application;

import br.com.fiap.autorepairshop.customer.domain.CustomerRepository;
import br.com.fiap.autorepairshop.customer.domain.LicensePlate;
import br.com.fiap.autorepairshop.customer.domain.Vehicle;
import br.com.fiap.autorepairshop.customer.domain.VehicleRepository;
import br.com.fiap.autorepairshop.shared.domain.BusinessRuleException;
import br.com.fiap.autorepairshop.shared.domain.DuplicateResourceException;
import br.com.fiap.autorepairshop.shared.domain.Page;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Casos de uso do cadastro de veículos.
 */
@ApplicationScoped
public class VehicleService {

    private final VehicleRepository vehicles;
    private final CustomerRepository customers;
    private final Clock clock;

    public VehicleService(VehicleRepository vehicles, CustomerRepository customers, Clock clock) {
        this.vehicles = vehicles;
        this.customers = customers;
        this.clock = clock;
    }

    @Transactional
    public Vehicle register(String licensePlate, String brand, String model, int year, UUID ownerId) {
        LicensePlate plate = LicensePlate.of(licensePlate);
        requireActiveOwner(ownerId);
        if (vehicles.findActiveByLicensePlate(plate.value()).isPresent()) {
            throw new DuplicateResourceException("Já existe um veículo ativo com esta placa.");
        }
        Vehicle vehicle = Vehicle.register(plate, brand, model, year, ownerId, now());
        vehicles.add(vehicle);
        return vehicle;
    }

    public Vehicle get(UUID id) {
        return vehicles.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado."));
    }

    /** Consulta o veículo mesmo que excluído, para referências históricas. */
    public Vehicle getIncludingDeleted(UUID id) {
        return vehicles.findIncludingDeleted(id).orElseThrow(() -> new ResourceNotFoundException("Veículo não encontrado."));
    }

    /** Busca pela placa informada com ou sem hífen; devolve uma página com zero ou um veículo. */
    public Page<Vehicle> findByLicensePlate(String licensePlate) {
        List<Vehicle> found = vehicles.findActiveByLicensePlate(LicensePlate.normalize(licensePlate)).stream().toList();
        return new Page<>(found, found.size());
    }

    public Page<Vehicle> listByOwner(UUID ownerId, int page, int size) {
        return vehicles.listActiveByOwner(ownerId, page, size);
    }

    public Page<Vehicle> list(int page, int size) {
        return vehicles.listActive(page, size);
    }

    @Transactional
    public Vehicle update(UUID id, String brand, String model, int year, UUID ownerId) {
        Vehicle vehicle = get(id);
        if (!vehicle.ownerId().equals(ownerId)) {
            requireActiveOwner(ownerId);
        }
        vehicle.update(brand, model, year, ownerId, now());
        vehicles.update(vehicle);
        return vehicle;
    }

    @Transactional
    public void delete(UUID id) {
        Vehicle vehicle = get(id);
        vehicle.delete(now());
        vehicles.update(vehicle);
    }

    private void requireActiveOwner(UUID ownerId) {
        if (customers.findActiveById(ownerId).isEmpty()) {
            throw new BusinessRuleException("O proprietário informado não é um cliente ativo.");
        }
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
