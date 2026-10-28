package br.com.fiap.autorepairshop.customer.domain;

import br.com.fiap.autorepairshop.shared.domain.Page;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência de veículos. Consultas retornam apenas veículos ativos.
 */
public interface VehicleRepository {

    Optional<Vehicle> findActiveById(UUID id);

    /** Busca também veículos excluídos, para exibir referências históricas, como as das ordens de serviço. */
    Optional<Vehicle> findIncludingDeleted(UUID id);

    Optional<Vehicle> findActiveByLicensePlate(String licensePlate);

    Page<Vehicle> listActive(int page, int size);

    Page<Vehicle> listActiveByOwner(UUID ownerId, int page, int size);

    void add(Vehicle vehicle);

    void update(Vehicle vehicle);
}
