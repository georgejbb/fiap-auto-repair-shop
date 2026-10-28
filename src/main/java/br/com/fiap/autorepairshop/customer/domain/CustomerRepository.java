package br.com.fiap.autorepairshop.customer.domain;

import br.com.fiap.autorepairshop.shared.domain.Page;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistência de clientes. Consultas retornam apenas clientes ativos.
 */
public interface CustomerRepository {

    Optional<Customer> findActiveById(UUID id);

    /** Busca também clientes excluídos, para exibir referências históricas, como as das ordens de serviço. */
    Optional<Customer> findIncludingDeleted(UUID id);

    Optional<Customer> findActiveByDocument(String documentNumber);

    Page<Customer> listActive(int page, int size);

    void add(Customer customer);

    void update(Customer customer);
}
