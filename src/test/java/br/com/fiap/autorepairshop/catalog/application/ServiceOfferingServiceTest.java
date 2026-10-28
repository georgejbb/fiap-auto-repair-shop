package br.com.fiap.autorepairshop.catalog.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.fiap.autorepairshop.catalog.domain.ServiceOffering;
import br.com.fiap.autorepairshop.catalog.domain.ServiceOfferingRepository;
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

class ServiceOfferingServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    private final ServiceOfferingRepository repository = mock(ServiceOfferingRepository.class);
    private final ServiceOfferingService service =
            new ServiceOfferingService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void createsOfferingWithTwoDecimalPrice() {
        ServiceOffering offering = service.create(" Troca de óleo ", " ", new BigDecimal("120"));

        assertThat(offering.name()).isEqualTo("Troca de óleo");
        assertThat(offering.description()).isNull();
        assertThat(offering.price().amount()).isEqualTo(new BigDecimal("120.00"));
        assertThat(offering.createdAt()).isEqualTo(NOW);
        verify(repository).add(offering);
    }

    @Test
    void rejectsInvalidPriceOrName() {
        assertThatThrownBy(() -> service.create("Alinhamento", null, new BigDecimal("-1")))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("price"));
        assertThatThrownBy(() -> service.create(" ", null, BigDecimal.TEN))
                .isInstanceOfSatisfying(InvalidValueException.class, e -> assertThat(e.field()).isEqualTo("name"));
        verify(repository, never()).add(any());
    }

    @Test
    void getUnknownOfferingFails() {
        UUID id = UUID.randomUUID();
        when(repository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Serviço não encontrado.");
    }

    @Test
    void listDelegatesToRepository() {
        Page<ServiceOffering> page = new Page<>(List.of(existing()), 1);
        when(repository.listActive(0, 20)).thenReturn(page);

        assertThat(service.list(0, 20)).isSameAs(page);
    }

    @Test
    void updateChangesPrice() {
        ServiceOffering offering = existing();
        when(repository.findActiveById(offering.id())).thenReturn(Optional.of(offering));

        service.update(offering.id(), "Troca de óleo", "Com filtro", new BigDecimal("150.50"));

        assertThat(offering.price().amount()).isEqualTo(new BigDecimal("150.50"));
        assertThat(offering.description()).isEqualTo("Com filtro");
        assertThat(offering.updatedAt()).isEqualTo(NOW);
        verify(repository).update(offering);
    }

    @Test
    void deleteMarksOfferingInactive() {
        ServiceOffering offering = existing();
        when(repository.findActiveById(offering.id())).thenReturn(Optional.of(offering));

        service.delete(offering.id());

        assertThat(offering.isActive()).isFalse();
        assertThat(offering.deletedAt()).isEqualTo(NOW);
        verify(repository).update(offering);
    }

    @Test
    void updateRejectsBlankName() {
        ServiceOffering offering = existing();
        when(repository.findActiveById(offering.id())).thenReturn(Optional.of(offering));

        assertThatThrownBy(() -> service.update(offering.id(), "", null, BigDecimal.ONE))
                .isInstanceOf(InvalidValueException.class);
    }

    private static ServiceOffering existing() {
        return ServiceOffering.create("Troca de óleo", null, Money.of("price", new BigDecimal("120.00")),
                Instant.EPOCH);
    }
}
