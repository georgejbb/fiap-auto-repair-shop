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
import br.com.fiap.autorepairshop.shared.domain.DuplicateResourceException;
import br.com.fiap.autorepairshop.shared.domain.InvalidValueException;
import br.com.fiap.autorepairshop.shared.domain.Page;
import br.com.fiap.autorepairshop.shared.domain.ResourceNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CustomerServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    private final CustomerRepository repository = mock(CustomerRepository.class);
    private final CustomerService service = new CustomerService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void registersCustomerWithNormalizedDocument() {
        when(repository.findActiveByDocument("52998224725")).thenReturn(Optional.empty());

        Customer customer = service.register("529.982.247-25", "Maria", "maria@email.com", null);

        assertThat(customer.document().number()).isEqualTo("52998224725");
        assertThat(customer.createdAt()).isEqualTo(NOW);
        verify(repository).add(customer);
    }

    @Test
    void rejectsDuplicateDocument() {
        when(repository.findActiveByDocument("52998224725")).thenReturn(Optional.of(existing()));

        assertThatThrownBy(() -> service.register("52998224725", "Outra", "outra@email.com", null))
                .isInstanceOf(DuplicateResourceException.class);
        verify(repository, never()).add(any());
    }

    @Test
    void rejectsInvalidDocumentBeforeQueryingRepository() {
        assertThatThrownBy(() -> service.register("123", "Maria", "maria@email.com", null))
                .isInstanceOf(InvalidValueException.class);
        verify(repository, never()).findActiveByDocument(any());
    }

    @Test
    void getUnknownCustomerFails() {
        UUID id = UUID.randomUUID();
        when(repository.findActiveById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cliente não encontrado.");
    }

    @Test
    void findByDocumentNormalizesInput() {
        Customer customer = existing();
        when(repository.findActiveByDocument("52998224725")).thenReturn(Optional.of(customer));

        Page<Customer> page = service.findByDocument("529.982.247-25");

        assertThat(page.items()).containsExactly(customer);
        assertThat(page.total()).isEqualTo(1);
    }

    @Test
    void listDelegatesToRepository() {
        Page<Customer> page = new Page<>(List.of(existing()), 1);
        when(repository.listActive(0, 20)).thenReturn(page);

        assertThat(service.list(0, 20)).isSameAs(page);
    }

    @Test
    void updateChangesDataAndPersists() {
        Customer customer = existing();
        when(repository.findActiveById(customer.id())).thenReturn(Optional.of(customer));

        service.update(customer.id(), "Maria Souza", "souza@email.com", "11988887777");

        assertThat(customer.name()).isEqualTo("Maria Souza");
        assertThat(customer.updatedAt()).isEqualTo(NOW);
        verify(repository).update(customer);
    }

    @Test
    void deleteMarksCustomerInactive() {
        Customer customer = existing();
        when(repository.findActiveById(customer.id())).thenReturn(Optional.of(customer));

        service.delete(customer.id());

        assertThat(customer.isActive()).isFalse();
        verify(repository).update(customer);
    }

    private static Customer existing() {
        return Customer.register(Document.of("52998224725"), "Maria", "maria@email.com", null, Instant.EPOCH);
    }
}
