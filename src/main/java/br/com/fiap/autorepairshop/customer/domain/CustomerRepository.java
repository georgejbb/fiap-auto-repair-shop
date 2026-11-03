package br.com.fiap.autorepairshop.customer.domain;

import br.com.fiap.autorepairshop.shared.domain.Page;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

    Optional<Customer> findActiveById(UUID id);

    Optional<Customer> findIncludingDeleted(UUID id);

    Optional<Customer> findActiveByDocument(String documentNumber);

    Page<Customer> listActive(int page, int size);

    void add(Customer customer);

    void update(Customer customer);
}
