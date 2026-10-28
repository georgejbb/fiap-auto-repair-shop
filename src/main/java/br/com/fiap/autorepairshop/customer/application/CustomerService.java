package br.com.fiap.autorepairshop.customer.application;

import br.com.fiap.autorepairshop.customer.domain.Customer;
import br.com.fiap.autorepairshop.customer.domain.CustomerRepository;
import br.com.fiap.autorepairshop.customer.domain.Document;
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
 * Casos de uso do cadastro de clientes.
 */
@ApplicationScoped
public class CustomerService {

    private final CustomerRepository customers;
    private final Clock clock;

    public CustomerService(CustomerRepository customers, Clock clock) {
        this.customers = customers;
        this.clock = clock;
    }

    @Transactional
    public Customer register(String document, String name, String email, String phone) {
        Document validDocument = Document.of(document);
        if (customers.findActiveByDocument(validDocument.number()).isPresent()) {
            throw new DuplicateResourceException("Já existe um cliente ativo com este documento.");
        }
        Customer customer = Customer.register(validDocument, name, email, phone, now());
        customers.add(customer);
        return customer;
    }

    public Customer get(UUID id) {
        return customers.findActiveById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));
    }

    /** Consulta o cliente mesmo que excluído, para referências históricas. */
    public Customer getIncludingDeleted(UUID id) {
        return customers.findIncludingDeleted(id).orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado."));
    }

    /** Busca pelo documento informado com ou sem pontuação; devolve uma página com zero ou um cliente. */
    public Page<Customer> findByDocument(String document) {
        List<Customer> found = customers.findActiveByDocument(Document.normalize(document)).stream().toList();
        return new Page<>(found, found.size());
    }

    public Page<Customer> list(int page, int size) {
        return customers.listActive(page, size);
    }

    @Transactional
    public Customer update(UUID id, String name, String email, String phone) {
        Customer customer = get(id);
        customer.update(name, email, phone, now());
        customers.update(customer);
        return customer;
    }

    @Transactional
    public void delete(UUID id) {
        Customer customer = get(id);
        customer.delete(now());
        customers.update(customer);
    }

    private Instant now() {
        return Instant.now(clock);
    }
}
