package br.com.fiap.autorepairshop.customer.infrastructure;

import br.com.fiap.autorepairshop.customer.domain.Customer;
import br.com.fiap.autorepairshop.customer.domain.CustomerRepository;
import br.com.fiap.autorepairshop.customer.domain.Document;
import br.com.fiap.autorepairshop.shared.domain.Page;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
class PanacheCustomerRepository implements CustomerRepository, PanacheRepositoryBase<CustomerEntity, UUID> {

    @Override
    public Optional<Customer> findActiveById(UUID id) {
        return find("id = ?1 and deletedAt is null", id).firstResultOptional().map(PanacheCustomerRepository::toDomain);
    }

    @Override
    public Optional<Customer> findActiveByDocument(String documentNumber) {
        return find("document = ?1 and deletedAt is null", documentNumber).firstResultOptional()
                .map(PanacheCustomerRepository::toDomain);
    }

    @Override
    public Page<Customer> listActive(int page, int size) {
        PanacheQuery<CustomerEntity> query = find("deletedAt is null", Sort.by("name").and("id"));
        return new Page<>(query.page(page, size).list().stream().map(PanacheCustomerRepository::toDomain).toList(),
                query.count());
    }

    @Override
    public void add(Customer customer) {
        CustomerEntity entity = new CustomerEntity();
        entity.id = customer.id();
        copy(customer, entity);
        persistAndFlush(entity);
    }

    @Override
    public void update(Customer customer) {
        CustomerEntity entity = findById(customer.id());
        copy(customer, entity);
        flush();
    }

    private static void copy(Customer customer, CustomerEntity entity) {
        entity.documentType = customer.document().type();
        entity.document = customer.document().number();
        entity.name = customer.name();
        entity.email = customer.email();
        entity.phone = customer.phone();
        entity.createdAt = customer.createdAt();
        entity.updatedAt = customer.updatedAt();
        entity.deletedAt = customer.deletedAt();
    }

    private static Customer toDomain(CustomerEntity entity) {
        return new Customer(entity.id, new Document(entity.documentType, entity.document), entity.name, entity.email,
                entity.phone, entity.createdAt, entity.updatedAt, entity.deletedAt);
    }
}
