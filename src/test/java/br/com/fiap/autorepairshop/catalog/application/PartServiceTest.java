package br.com.fiap.autorepairshop.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.autorepairshop.catalog.domain.Part;
import br.com.fiap.autorepairshop.catalog.domain.PartCategory;
import br.com.fiap.autorepairshop.catalog.domain.PartRepository;
import br.com.fiap.autorepairshop.catalog.domain.StockMovement;
import br.com.fiap.autorepairshop.catalog.domain.StockMovementType;
import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import br.com.fiap.autorepairshop.shared.domain.Money;
import br.com.fiap.autorepairshop.shared.domain.Page;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PartServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    private final PartRepository repository = mock(PartRepository.class);
    private final PartService service = new PartService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsPartWithZeroStock() {
        Part part = service.create("Filtro de óleo", PartCategory.PART, new BigDecimal("35.00"));

        assertThat(part.stockQuantity()).isZero();
        assertThat(part.createdAt()).isEqualTo(NOW);
        verify(repository).add(part);
    }

    @Test
    void rejectsInvalidUnitPrice() {
        assertThatThrownBy(() -> service.create("Filtro", PartCategory.PART, new BigDecimal("1.234")))
                .isInstanceOfSatisfying(InvalidValueException.class,
                        e -> assertThat(e.field()).isEqualTo("unitPrice"));
        verify(repository, never()).add(any());
    }

    @Test
    void inboundUpdatesPartAndRecordsMovement() {
        Part part = existing();
        when(repository.findActiveById(part.id())).thenReturn(Optional.of(part));

        service.registerInbound(part.id(), 10);

        assertThat(part.stockQuantity()).isEqualTo(10);
        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(repository).update(part);
        verify(repository).addMovement(movement.capture());
        assertThat(movement.getValue().type()).isEqualTo(StockMovementType.INBOUND);
        assertThat(movement.getValue().occurredAt()).isEqualTo(NOW);
    }

    @Test
    void invalidInboundDoesNotPersist() {
        Part part = existing();
        when(repository.findActiveById(part.id())).thenReturn(Optional.of(part));

        assertThatThrownBy(() -> service.registerInbound(part.id(), 0)).isInstanceOf(InvalidValueException.class);
        verify(repository, never()).update(any());
        verify(repository, never()).addMovement(any());
    }

    @Test
    void adjustmentRecordsDelta() {
        Part part = existing();
        part.registerInbound(10, NOW);
        when(repository.findActiveById(part.id())).thenReturn(Optional.of(part));

        service.adjustStock(part.id(), 7);

        ArgumentCaptor<StockMovement> movement = ArgumentCaptor.forClass(StockMovement.class);
        verify(repository).addMovement(movement.capture());
        assertThat(movement.getValue().quantity()).isEqualTo(-3);
        assertThat(part.stockQuantity()).isEqualTo(7);
    }

    @Test
    void getUnknownPartFails() {
        UUID id = UUID.randomUUID();
        when(repository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Peça ou insumo não encontrado.");
        assertThatThrownBy(() -> service.movements(id, 0, 20)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listAndMovementsDelegateToRepository() {
        Part part = existing();
        Page<Part> parts = new Page<>(List.of(part), 1);
        Page<StockMovement> movements = new Page<>(List.of(), 0);
        when(repository.listActive(PartCategory.PART, 0, 20)).thenReturn(parts);
        when(repository.findActiveById(part.id())).thenReturn(Optional.of(part));
        when(repository.listMovements(part.id(), 0, 20)).thenReturn(movements);

        assertThat(service.list(PartCategory.PART, 0, 20)).isSameAs(parts);
        assertThat(service.movements(part.id(), 0, 20)).isSameAs(movements);
    }

    @Test
    void updateKeepsStock() {
        Part part = existing();
        part.registerInbound(4, NOW);
        when(repository.findActiveById(part.id())).thenReturn(Optional.of(part));

        service.update(part.id(), "Filtro de ar", PartCategory.PART, new BigDecimal("50.00"));

        assertThat(part.stockQuantity()).isEqualTo(4);
        assertThat(part.name()).isEqualTo("Filtro de ar");
        verify(repository).update(part);
    }

    @Test
    void deleteMarksPartInactive() {
        Part part = existing();
        when(repository.findActiveById(part.id())).thenReturn(Optional.of(part));

        service.delete(part.id());

        assertThat(part.isActive()).isFalse();
        verify(repository).update(part);
    }

    private static Part existing() {
        return Part.create("Filtro de óleo", PartCategory.PART, Money.of("unitPrice", new BigDecimal("35.00")),
                Instant.EPOCH);
    }
}
