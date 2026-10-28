package br.com.fiap.autorepairshop.customer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.autorepairshop.customer.domain.Customer;
import br.com.fiap.autorepairshop.customer.domain.CustomerRepository;
import br.com.fiap.autorepairshop.customer.domain.Document;
import br.com.fiap.autorepairshop.customer.domain.LicensePlate;
import br.com.fiap.autorepairshop.customer.domain.Vehicle;
import br.com.fiap.autorepairshop.customer.domain.VehicleRepository;
import br.com.fiap.autorepairshop.shared.domain.BusinessRuleException;
import br.com.fiap.autorepairshop.shared.domain.DuplicateResourceException;
import br.com.fiap.autorepairshop.shared.domain.Page;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VehicleServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    private final VehicleRepository vehicles = mock(VehicleRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final VehicleService service = new VehicleService(vehicles, customers, Clock.fixed(NOW, ZoneOffset.UTC));

    private final Customer owner =
            Customer.register(Document.of("52998224725"), "Maria", "maria@email.com", null, Instant.EPOCH);

    @Test
    void registersVehicleForActiveOwner() {
        when(customers.findActiveById(owner.id())).thenReturn(Optional.of(owner));
        when(vehicles.findActiveByLicensePlate("ABC1234")).thenReturn(Optional.empty());

        Vehicle vehicle = service.register("abc-1234", "Fiat", "Argo", 2022, owner.id());

        assertThat(vehicle.licensePlate().value()).isEqualTo("ABC1234");
        assertThat(vehicle.createdAt()).isEqualTo(NOW);
        verify(vehicles).add(vehicle);
    }

    @Test
    void rejectsInactiveOwner() {
        UUID unknown = UUID.randomUUID();
        when(customers.findActiveById(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.register("ABC1234", "Fiat", "Argo", 2022, unknown))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("O proprietário informado não é um cliente ativo.");
        verify(vehicles, never()).add(any());
    }

    @Test
    void rejectsDuplicatePlate() {
        when(customers.findActiveById(owner.id())).thenReturn(Optional.of(owner));
        when(vehicles.findActiveByLicensePlate("ABC1234")).thenReturn(Optional.of(vehicle(owner.id())));

        assertThatThrownBy(() -> service.register("ABC-1234", "Fiat", "Argo", 2022, owner.id()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void getUnknownVehicleFails() {
        UUID id = UUID.randomUUID();
        when(vehicles.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getIncludingDeletedFindsDeletedVehicle() {
        Vehicle vehicle = vehicle(owner.id());
        vehicle.delete(NOW);
        when(vehicles.findIncludingDeleted(vehicle.id())).thenReturn(Optional.of(vehicle));
        UUID unknown = UUID.randomUUID();
        when(vehicles.findIncludingDeleted(unknown)).thenReturn(Optional.empty());

        assertThat(service.getIncludingDeleted(vehicle.id())).isSameAs(vehicle);
        assertThatThrownBy(() -> service.getIncludingDeleted(unknown)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findByLicensePlateNormalizesInput() {
        Vehicle vehicle = vehicle(owner.id());
        when(vehicles.findActiveByLicensePlate("ABC1234")).thenReturn(Optional.of(vehicle));

        assertThat(service.findByLicensePlate("abc-1234").items()).containsExactly(vehicle);
    }

    @Test
    void listingsDelegateToRepository() {
        Page<Vehicle> page = new Page<>(List.of(vehicle(owner.id())), 1);
        when(vehicles.listActive(0, 20)).thenReturn(page);
        when(vehicles.listActiveByOwner(owner.id(), 0, 20)).thenReturn(page);

        assertThat(service.list(0, 20)).isSameAs(page);
        assertThat(service.listByOwner(owner.id(), 0, 20)).isSameAs(page);
    }

    @Test
    void updateWithSameOwnerDoesNotRecheckOwner() {
        Vehicle vehicle = vehicle(owner.id());
        when(vehicles.findActiveById(vehicle.id())).thenReturn(Optional.of(vehicle));

        service.update(vehicle.id(), "Fiat", "Cronos", 2023, owner.id());

        assertThat(vehicle.model()).isEqualTo("Cronos");
        verify(customers, never()).findActiveById(any());
        verify(vehicles).update(vehicle);
    }

    @Test
    void updateToInactiveOwnerFails() {
        Vehicle vehicle = vehicle(owner.id());
        UUID other = UUID.randomUUID();
        when(vehicles.findActiveById(vehicle.id())).thenReturn(Optional.of(vehicle));
        when(customers.findActiveById(other)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(vehicle.id(), "Fiat", "Argo", 2022, other))
                .isInstanceOf(BusinessRuleException.class);
        verify(vehicles, never()).update(any());
    }

    @Test
    void deleteMarksVehicleInactive() {
        Vehicle vehicle = vehicle(owner.id());
        when(vehicles.findActiveById(vehicle.id())).thenReturn(Optional.of(vehicle));

        service.delete(vehicle.id());

        assertThat(vehicle.isActive()).isFalse();
        verify(vehicles).update(vehicle);
    }

    private static Vehicle vehicle(UUID ownerId) {
        return Vehicle.register(LicensePlate.of("ABC1234"), "Fiat", "Argo", 2022, ownerId, NOW);
    }
}
